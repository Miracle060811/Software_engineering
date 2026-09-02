[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$BackendRepository = "ghcr.io/miracle060811/travelmate-backend",
    [string]$FrontendRepository = "ghcr.io/miracle060811/travelmate-frontend",
    [string]$Channel = "deploy",
    [int]$DockerStartupTimeoutSeconds = 240,
    [int]$KubernetesStartupTimeoutSeconds = 300,
    [int]$RolloutTimeoutSeconds = 360
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$stateDirectory = Join-Path $env:USERPROFILE "TravelMateCD"
$logPath = Join-Path $stateDirectory "deploy.log"
[IO.Directory]::CreateDirectory($stateDirectory) | Out-Null
$dockerDesktopStartedByScript = $false
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path

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

function Test-NativeCommand {
    param(
        [string]$Command,
        [string[]]$Arguments
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $Command @Arguments 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Repair-StaleDockerRuntimeSockets {
    $dockerProcesses = @(
        Get-Process -Name "Docker Desktop", "com.docker.backend" -ErrorAction SilentlyContinue
    )
    if ($dockerProcesses.Count -gt 0) {
        return
    }

    $runtimeDirectories = @(
        (Join-Path $env:LOCALAPPDATA "Docker\run"),
        (Join-Path $env:LOCALAPPDATA "docker-secrets-engine")
    )
    foreach ($runtimeDirectory in $runtimeDirectories) {
        if (-not (Test-Path -LiteralPath $runtimeDirectory -PathType Container)) {
            continue
        }

        $socketEntries = @(
            Get-ChildItem -LiteralPath $runtimeDirectory -Force -ErrorAction SilentlyContinue |
                Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }
        )
        if ($socketEntries.Count -eq 0) {
            continue
        }

        $suffix = Get-Date -Format "yyyyMMdd-HHmmssfff"
        $backupDirectory = "$runtimeDirectory.stale-manual-$suffix"
        Move-Item -LiteralPath $runtimeDirectory -Destination $backupDirectory
        [IO.Directory]::CreateDirectory($runtimeDirectory) | Out-Null
        Write-DeployLog "Moved stale Docker runtime sockets to '$backupDirectory'."
    }
}

function Wait-DockerDesktop {
    if (Test-NativeCommand -Command docker -Arguments @("info", "--format", "{{.ServerVersion}}")) {
        return
    }

    $dockerDesktop = "C:\Program Files\Docker\Docker\Docker Desktop.exe"
    if (-not (Test-Path -LiteralPath $dockerDesktop -PathType Leaf)) {
        throw "Docker Desktop is not installed at the expected path"
    }

    $dockerProcesses = @(
        Get-Process -Name "Docker Desktop", "com.docker.backend" -ErrorAction SilentlyContinue
    )
    if ($dockerProcesses.Count -eq 0) {
        Repair-StaleDockerRuntimeSockets
        Write-DeployLog "Docker Engine is unavailable; starting Docker Desktop."
        Start-Process -FilePath $dockerDesktop -WindowStyle Hidden
        $script:dockerDesktopStartedByScript = $true
    }
    else {
        Write-DeployLog "Docker Desktop is starting; waiting for the Engine."
    }
    $deadline = (Get-Date).AddSeconds($DockerStartupTimeoutSeconds)
    do {
        Start-Sleep -Seconds 5
        if (Test-NativeCommand -Command docker -Arguments @("info", "--format", "{{.ServerVersion}}")) {
            Write-DeployLog "Docker Engine is ready."
            return
        }
    } while ((Get-Date) -lt $deadline)

    throw "Docker Engine did not become ready within $DockerStartupTimeoutSeconds seconds"
}

function Wait-KindNode {
    $deadline = (Get-Date).AddSeconds($KubernetesStartupTimeoutSeconds)
    do {
        if (Test-NativeCommand -Command docker -Arguments @("inspect", "desktop-control-plane")) {
            return
        }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    throw "Docker Desktop Kubernetes node did not appear within $KubernetesStartupTimeoutSeconds seconds"
}

function Wait-KubernetesApi {
    $deadline = (Get-Date).AddSeconds($KubernetesStartupTimeoutSeconds)
    do {
        if (Test-NativeCommand -Command kubectl -Arguments @("get", "nodes", "--request-timeout=5s")) {
            if ($script:dockerDesktopStartedByScript) {
                Write-DeployLog "Kubernetes API is responding; allowing controllers to reconcile after the cold start."
                Start-Sleep -Seconds 30
            }
            Invoke-Checked -Command kubectl -Arguments @(
                "wait", "--for=condition=Ready", "node", "--all",
                "--timeout=${KubernetesStartupTimeoutSeconds}s"
            ) | Out-Null
            Invoke-Checked -Command kubectl -Arguments @(
                "wait", "--for=condition=Ready", "pod", "-l", "k8s-app=kube-dns",
                "-n", "kube-system", "--timeout=${KubernetesStartupTimeoutSeconds}s"
            ) | Out-Null
            Write-DeployLog "Kubernetes API is ready."
            return
        }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)

    throw "Kubernetes API did not become ready within $KubernetesStartupTimeoutSeconds seconds"
}

function Get-LocalImageIdentity {
    param(
        [string]$Image,
        [string]$Repository
    )

    Write-DeployLog "Pulling deployment image $Image." | Write-Host
    Invoke-Checked -Command docker -Arguments @("pull", $Image) | Out-Null
    Write-DeployLog "Deployment image $Image is available locally." | Write-Host
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

function Get-DeploymentState {
    param([string]$Deployment, [string]$Container)
    $raw = (Invoke-Checked -Command kubectl -Arguments @("get", "deployment", $Deployment, "-n", $Namespace, "-o", "json")) -join [Environment]::NewLine
    $deploymentObject = $raw | ConvertFrom-Json
    $containerObject = @($deploymentObject.spec.template.spec.containers | Where-Object name -eq $Container)[0]
    if ($null -eq $containerObject) {
        throw "Container '$Container' was not found in Deployment '$Deployment'"
    }

    $annotationsProperty = $deploymentObject.metadata.PSObject.Properties["annotations"]
    $annotations = if ($null -eq $annotationsProperty) {
        $null
    }
    else {
        $annotationsProperty.Value
    }
    $commitProperty = if ($null -eq $annotations) {
        $null
    }
    else {
        $annotations.PSObject.Properties["travelmate.io/commit"]
    }
    $digestProperty = if ($null -eq $annotations) {
        $null
    }
    else {
        $annotations.PSObject.Properties["travelmate.io/image-digest"]
    }

    return [pscustomobject]@{
        Deployment = $Deployment
        Container = $Container
        Image = [string]$containerObject.image
        Commit = [pscustomobject]@{
            Exists = ($null -ne $commitProperty)
            Value = if ($null -eq $commitProperty) { $null } else { [string]$commitProperty.Value }
        }
        Digest = [pscustomobject]@{
            Exists = ($null -ne $digestProperty)
            Value = if ($null -eq $digestProperty) { $null } else { [string]$digestProperty.Value }
        }
    }
}

function Restore-DeploymentAnnotation {
    param(
        [string]$Deployment,
        [string]$Name,
        [pscustomobject]$State
    )

    $annotationArgument = if ($State.Exists) {
        "$Name=$($State.Value)"
    }
    else {
        "$Name-"
    }
    Invoke-Checked -Command kubectl -Arguments @(
        "annotate", "deployment/$Deployment", $annotationArgument,
        "--overwrite", "-n", $Namespace
    ) | Out-Null
}

function Invoke-RollbackStep {
    param(
        [string]$Description,
        [scriptblock]$Action,
        [System.Collections.Generic.List[string]]$Failures
    )

    try {
        & $Action
        Write-DeployLog "Rollback step succeeded: $Description."
    }
    catch {
        $failure = "$Description`: $($_.Exception.Message)"
        $Failures.Add($failure)
        Write-DeployLog "Rollback step failed: $failure"
    }
}

function Wait-Rollout {
    param([string]$Deployment)
    Invoke-Checked -Command kubectl -Arguments @("rollout", "status", "deployment/$Deployment", "-n", $Namespace, "--timeout=${RolloutTimeoutSeconds}s") | Out-Null
}

function Wait-TravelMateDependencies {
    Invoke-Checked -Command kubectl -Arguments @(
        "rollout", "status", "statefulset/travelmate-mysql", "-n", $Namespace,
        "--timeout=${RolloutTimeoutSeconds}s"
    ) | Out-Null
    Wait-Rollout -Deployment travelmate-redis
}

function Ensure-DeploymentReady {
    param([string]$Deployment)

    $raw = (Invoke-Checked -Command kubectl -Arguments @(
        "get", "deployment", $Deployment, "-n", $Namespace, "-o", "json"
    )) -join [Environment]::NewLine
    $deploymentObject = $raw | ConvertFrom-Json
    $desiredReplicas = [int]$deploymentObject.spec.replicas
    $availableReplicasProperty = $deploymentObject.status.PSObject.Properties["availableReplicas"]
    $availableReplicas = if ($null -eq $availableReplicasProperty) {
        0
    }
    else {
        [int]$availableReplicasProperty.Value
    }

    if ($availableReplicas -lt $desiredReplicas) {
        Write-DeployLog "Deployment $Deployment is not ready ($availableReplicas/$desiredReplicas); restarting its Pods."
        Invoke-Checked -Command kubectl -Arguments @(
            "rollout", "restart", "deployment/$Deployment", "-n", $Namespace
        ) | Out-Null
    }
    Wait-Rollout -Deployment $Deployment
}

function Assert-FrontendHealth {
    $deadline = (Get-Date).AddSeconds(90)
    do {
        try {
            $health = Invoke-WebRequest -UseBasicParsing -Uri "http://127.0.0.1:30080/healthz" -TimeoutSec 10
            if ($health.StatusCode -eq 200) {
                return
            }
        }
        catch {
            # The Docker Desktop load balancer can lag behind ready Pods briefly.
        }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $deadline)
    throw "Frontend health endpoint did not return HTTP 200 within 90 seconds"
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
    Wait-KindNode
    & (Join-Path $PSScriptRoot "Ensure-KindProxy.ps1") | ForEach-Object { Write-DeployLog $_ }
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to prepare Kind node networking"
    }
    Wait-KubernetesApi

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
        Wait-TravelMateDependencies
        Ensure-DeploymentReady -Deployment travelmate-backend
        Ensure-DeploymentReady -Deployment travelmate-frontend
        Assert-FrontendHealth
        Write-DeployLog "Commit $currentCommit is already deployed and healthy."
        return
    }

    $previousBackend = Get-DeploymentState -Deployment travelmate-backend -Container backend
    $previousFrontend = Get-DeploymentState -Deployment travelmate-frontend -Container frontend
    Write-DeployLog "Deploying commit $($backend.Revision); backend=$($backend.Digest), frontend=$($frontend.Digest)."

    try {
        $rbacManifest = Join-Path $repositoryRoot "deploy\k8s\rbac-secret-admin.yaml"
        Invoke-Checked -Command kubectl -Arguments @("apply", "-f", $rbacManifest) | Out-Null
        Write-DeployLog "Applied secret-admin RBAC."
        Invoke-Checked -Command kubectl -Arguments @(
            "patch", "deployment", "travelmate-backend", "-n", $Namespace,
            "--type", "merge",
            "-p", '{"spec":{"template":{"spec":{"serviceAccountName":"secret-admin"}}}}'
        ) | Out-Null
        Write-DeployLog "Patched backend deployment to use secret-admin service account."

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
        Assert-FrontendHealth
    }
    catch {
        $rolloutFailure = $_.Exception.Message
        Write-DeployLog "Rollout failed; restoring previous images and annotations. Reason: $rolloutFailure"
        $rollbackFailures = [System.Collections.Generic.List[string]]::new()

        Invoke-RollbackStep -Description "restore travelmate-backend image" -Failures $rollbackFailures -Action {
            Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-backend", "backend=$($previousBackend.Image)", "-n", $Namespace) | Out-Null
        }
        Invoke-RollbackStep -Description "restore travelmate-frontend image" -Failures $rollbackFailures -Action {
            Invoke-Checked -Command kubectl -Arguments @("set", "image", "deployment/travelmate-frontend", "frontend=$($previousFrontend.Image)", "-n", $Namespace) | Out-Null
        }
        Invoke-RollbackStep -Description "restore travelmate-backend commit annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment travelmate-backend -Name "travelmate.io/commit" -State $previousBackend.Commit
        }
        Invoke-RollbackStep -Description "restore travelmate-backend digest annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment travelmate-backend -Name "travelmate.io/image-digest" -State $previousBackend.Digest
        }
        Invoke-RollbackStep -Description "restore travelmate-frontend commit annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment travelmate-frontend -Name "travelmate.io/commit" -State $previousFrontend.Commit
        }
        Invoke-RollbackStep -Description "restore travelmate-frontend digest annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment travelmate-frontend -Name "travelmate.io/image-digest" -State $previousFrontend.Digest
        }
        Invoke-RollbackStep -Description "wait for travelmate-backend rollback" -Failures $rollbackFailures -Action {
            Wait-Rollout -Deployment travelmate-backend
        }
        Invoke-RollbackStep -Description "wait for travelmate-frontend rollback" -Failures $rollbackFailures -Action {
            Wait-Rollout -Deployment travelmate-frontend
        }

        if ($rollbackFailures.Count -gt 0) {
            throw "Deployment rollout failed: $rolloutFailure Rollback also encountered: $($rollbackFailures -join ' | ')"
        }
        throw "Deployment rollout failed; previous images and annotations were restored. Original failure: $rolloutFailure"
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
