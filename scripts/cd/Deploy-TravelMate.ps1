[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$BackendRepository = "ghcr.io/miracle060811/travelmate-backend",
    [string]$FrontendRepository = "ghcr.io/miracle060811/travelmate-frontend",
    [string]$Channel = "deploy",
    [int]$DockerStartupTimeoutSeconds = 240,
    [int]$RolloutTimeoutSeconds = 360
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$stateDirectory = Join-Path $env:USERPROFILE "TravelMateCD"
$logPath = Join-Path $stateDirectory "deploy.log"
[IO.Directory]::CreateDirectory($stateDirectory) | Out-Null

function Write-DeployLog {
    param([string]$Message)
    $line = "{0:yyyy-MM-dd HH:mm:ss} {1}" -f (Get-Date), $Message
    Add-Content -LiteralPath $logPath -Value $line -Encoding utf8
    Write-Output $line
}

function Invoke-Checked {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    $output = & $Command @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "$Command failed: $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Wait-DockerDesktop {
    & docker info --format "{{.ServerVersion}}" 2>$null | Out-Null
    if ($LASTEXITCODE -eq 0) {
        return
    }

    $dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path -LiteralPath $dockerDesktop -PathType Leaf)) {
        throw "Docker Desktop is not installed at the expected path"
    }

    Write-DeployLog "Docker Engine is unavailable; starting Docker Desktop."
    Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
    $deadline = (Get-Date).AddSeconds($DockerStartupTimeoutSeconds)
    do {
        Start-Sleep -Seconds 5
        & docker info --format "{{.ServerVersion}}" 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-DeployLog "Docker Engine is ready."
            return
        }
    } while ((Get-Date) -lt $deadline)

    throw "Docker Engine did not become ready within $DockerStartupTimeoutSeconds seconds"
}

function Get-LocalImageIdentity {
    param(
        [string]$Image,
        [string]$Repository
    )

    Invoke-Checked -Command docker -Arguments @("pull", $Image) | Out-Null
    $inspect = (Invoke-Checked -Command docker -Arguments @("image", "inspect", $Image)) -join [Environment]::NewLine
    $imageInfo = @($inspect | ConvertFrom-Json)[0]
    $revision = [string]$imageInfo.Config.Labels."org.opencontainers.image.revision"
    $digest = @($imageInfo.RepoDigests | Where-Object { $_ -like "$Repository@sha256:*" }) | Select-Object -First 1
    if (-not $digest) {
        throw "No repository digest was found for $Image"
    }
    return [pscustomobject]@{
        Revision = $revision
        Digest = [string]$digest
    }
}

function Get-DeploymentImage {
    param([string]$Deployment, [string]$Container)
    $raw = (Invoke-Checked -Command kubectl -Arguments @("get", "deployment", $Deployment, "-n", $Namespace, "-o", "json")) -join [Environment]::NewLine
    $deploymentObject = $raw | ConvertFrom-Json
    return [string](@($deploymentObject.spec.template.spec.containers | Where-Object name -eq $Container)[0].image)
}

function Wait-Rollout {
    param([string]$Deployment)
    Invoke-Checked -Command kubectl -Arguments @("rollout", "status", "deployment/$Deployment", "-n", $Namespace, "--timeout=${RolloutTimeoutSeconds}s") | Out-Null
}

$mutex = [Threading.Mutex]::new($false, "Local\TravelMateCD")
$lockAcquired = $false
try {
    $lockAcquired = $mutex.WaitOne(0)
    if (-not $lockAcquired) {
        Write-DeployLog "Another deployment check is already running; skipping this cycle."
        return
    }

    Wait-DockerDesktop
    $contexts = @((Invoke-Checked -Command kubectl -Arguments @("config", "get-contexts", "-o", "name")))
    if ($contexts -notcontains $KubeContext) {
        throw "Kubernetes context '$KubeContext' is not available"
    }
    Invoke-Checked -Command kubectl -Arguments @("config", "use-context", $KubeContext) | Out-Null
    Invoke-Checked -Command kubectl -Arguments @("get", "nodes", "--request-timeout=15s") | Out-Null
    & (Join-Path $PSScriptRoot "Ensure-KindProxy.ps1") | ForEach-Object { Write-DeployLog $_ }
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to prepare Kind node networking"
    }

    $backendTag = "${BackendRepository}:${Channel}"
    $frontendTag = "${FrontendRepository}:${Channel}"
    $backend = Get-LocalImageIdentity -Image $backendTag -Repository $BackendRepository
    $frontend = Get-LocalImageIdentity -Image $frontendTag -Repository $FrontendRepository

    if ($backend.Revision -notmatch '^[0-9a-f]{40}$') {
        throw "Backend image has an invalid commit revision label"
    }
    if ($backend.Revision -ne $frontend.Revision) {
        throw "Frontend and backend deployment channels point to different commits"
    }

    $currentCommit = (& kubectl get deployment travelmate-backend -n $Namespace `
        -o "jsonpath={.metadata.annotations.travelmate\.io/commit}" 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "TravelMate Deployments are not initialized"
    }
    if ($currentCommit -eq $backend.Revision) {
        Write-DeployLog "Commit $currentCommit is already deployed."
        return
    }

    $previousBackend = Get-DeploymentImage -Deployment travelmate-backend -Container backend
    $previousFrontend = Get-DeploymentImage -Deployment travelmate-frontend -Container frontend
    Write-DeployLog "Deploying commit $($backend.Revision); backend=$($backend.Digest), frontend=$($frontend.Digest)."

    try {
        Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-backend", "backend=$($backend.Digest)", "-n", $Namespace) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-frontend", "frontend=$($frontend.Digest)", "-n", $Namespace) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @(
            "annotate", "deployment/travelmate-backend",
            "travelmate.io/commit=$($backend.Revision)",
            "travelmate.io/image-digest=$($backend.Digest.Split('@')[1])",
            "--overwrite", "-n", $Namespace
        ) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @(
            "annotate", "deployment/travelmate-frontend",
            "travelmate.io/commit=$($frontend.Revision)",
            "travelmate.io/image-digest=$($frontend.Digest.Split('@')[1])",
            "--overwrite", "-n", $Namespace
        ) | Out-Null

        Wait-Rollout -Deployment travelmate-backend
        Wait-Rollout -Deployment travelmate-frontend
        $health = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:30080/healthz" -TimeoutSec 15
        if ($health.StatusCode -ne 200) {
            throw "Frontend health endpoint returned HTTP $($health.StatusCode)"
        }
    }
    catch {
        Write-DeployLog "Rollout failed; restoring previous images. Reason: $($_.Exception.Message)"
        Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-backend", "backend=$previousBackend", "-n", $Namespace) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-frontend", "frontend=$previousFrontend", "-n", $Namespace) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @("annotate", "deployment/travelmate-backend", "travelmate.io/commit=$currentCommit", "--overwrite", "-n", $Namespace) | Out-Null
        Invoke-Checked -Command kubectl -Arguments @("annotate", "deployment/travelmate-frontend", "travelmate.io/commit=$currentCommit", "--overwrite", "-n", $Namespace) | Out-Null
        Wait-Rollout -Deployment travelmate-backend
        Wait-Rollout -Deployment travelmate-frontend
        throw
    }

    Write-DeployLog "Commit $($backend.Revision) deployed successfully."
}
catch {
    Write-DeployLog "Deployment check failed: $($_.Exception.Message)"
    exit 1
}
finally {
    if ($lockAcquired) {
        $mutex.ReleaseMutex()
    }
    $mutex.Dispose()
}
