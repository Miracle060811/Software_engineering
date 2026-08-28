[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$GitHubUsername = "Sylphira-ovo",
    [Security.SecureString]$Token,
    [switch]$TokenFromStandardInput
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function ConvertTo-Base64 {
    param([string]$Value)
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

function Invoke-Kubectl {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $output = & kubectl @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Test-GhcrPullAccess {
    param(
        [string]$Repository,
        [string]$Username,
        [string]$PlainToken
    )

    $basicAuthorization = [Convert]::ToBase64String(
        [Text.Encoding]::ASCII.GetBytes("${Username}:$PlainToken")
    )
    try {
        $response = Invoke-WebRequest `
            -UseBasicParsing `
            -Uri "https://ghcr.io/token?service=ghcr.io&scope=repository:${Repository}:pull" `
            -Headers @{ Authorization = "Basic $basicAuthorization" }
        $registryToken = ($response.Content | ConvertFrom-Json).token
        if ($response.StatusCode -ne 200 -or [string]::IsNullOrWhiteSpace($registryToken)) {
            throw "GHCR did not issue a pull token for '$Repository'"
        }
    }
    finally {
        $basicAuthorization = $null
        $registryToken = $null
        $response = $null
    }
}

function Save-DockerCredential {
    param(
        [string]$Registry,
        [string]$Username,
        [string]$PlainToken
    )

    $dockerConfigPath = Join-Path $env:USERPROFILE ".docker\config.json"
    if (-not (Test-Path -LiteralPath $dockerConfigPath -PathType Leaf)) {
        throw "Docker configuration is missing: $dockerConfigPath"
    }

    $dockerConfigObject = Get-Content -LiteralPath $dockerConfigPath -Raw | ConvertFrom-Json
    $credentialStore = [string]$dockerConfigObject.credsStore
    if ([string]::IsNullOrWhiteSpace($credentialStore)) {
        throw "Docker is not configured with a credential store"
    }

    $credentialHelperName = "docker-credential-$credentialStore"
    $credentialHelper = Get-Command $credentialHelperName -ErrorAction SilentlyContinue
    if ($null -eq $credentialHelper) {
        throw "Docker credential helper is unavailable: $credentialHelperName"
    }

    $credentialPayload = [ordered]@{
        ServerURL = $Registry
        Username = $Username
        Secret = $PlainToken
    } | ConvertTo-Json -Compress

    # Windows PowerShell 5.1 prefixes native-pipeline input with a UTF-8 BOM,
    # which Docker Desktop's credential helper rejects. Pass the ASCII-only
    # JSON through a short-lived child-process environment variable instead.
    $credentialEnvironmentName = "TRAVELMATE_DOCKER_CREDENTIAL"
    $processStartInfo = [Diagnostics.ProcessStartInfo]::new()
    $processStartInfo.FileName = $env:ComSpec
    $processStartInfo.Arguments = "/d /s /c `"echo %${credentialEnvironmentName}%|$credentialHelperName store`""
    $processStartInfo.UseShellExecute = $false
    $processStartInfo.CreateNoWindow = $true
    $processStartInfo.RedirectStandardOutput = $true
    $processStartInfo.RedirectStandardError = $true
    $processStartInfo.EnvironmentVariables[$credentialEnvironmentName] = $credentialPayload

    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $processStartInfo
    try {
        if (-not $process.Start()) {
            throw "Unable to start Docker credential helper: $credentialHelperName"
        }
        $storeOutput = $process.StandardOutput.ReadToEnd()
        $storeError = $process.StandardError.ReadToEnd()
        $process.WaitForExit()
        $storeExitCode = $process.ExitCode
    }
    finally {
        $credentialPayload = $null
        $processStartInfo.EnvironmentVariables.Remove($credentialEnvironmentName)
        $process.Dispose()
    }
    if ($storeExitCode -ne 0) {
        $storeMessage = @($storeOutput, $storeError) -join [Environment]::NewLine
        throw "Unable to save the GHCR credential in Docker Desktop: $storeMessage"
    }
}

foreach ($command in @("docker", "kubectl")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "$command is not available"
    }
}

& docker info --format "{{.ServerVersion}}" 2>$null | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Engine is not available"
}

$contexts = @((Invoke-Kubectl -Arguments @("config", "get-contexts", "-o", "name")))
if ($contexts -notcontains $KubeContext) {
    throw "Kubernetes context '$KubeContext' is not available"
}
Invoke-Kubectl -Arguments @("config", "use-context", $KubeContext) | Out-Null

$namespaceManifest = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path "deploy\k8s\namespace.yaml"
if (-not (Test-Path -LiteralPath $namespaceManifest -PathType Leaf)) {
    throw "Namespace manifest is missing: $namespaceManifest"
}
Invoke-Kubectl -Arguments @("apply", "-f", $namespaceManifest) | Out-Null

$tokenPointer = [IntPtr]::Zero
$plainToken = $null
try {
    if ($null -eq $Token) {
        if ($TokenFromStandardInput) {
            $plainToken = [Console]::In.ReadLine()
        }
        else {
            $Token = Read-Host "Paste the GitHub token with read:packages scope" -AsSecureString
        }
    }

    if ($null -eq $plainToken) {
        $tokenPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Token)
        $plainToken = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($tokenPointer)
    }
    if ([string]::IsNullOrWhiteSpace($plainToken)) {
        throw "The GitHub token is empty"
    }

    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = "Continue"
        $dockerLoginOutput = $plainToken | & docker login ghcr.io --username $GitHubUsername --password-stdin 2>&1
        $dockerLoginExitCode = $LASTEXITCODE
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($dockerLoginExitCode -ne 0) {
        # Some Docker Desktop/GHCR combinations reject Docker's unscoped /v2/
        # login probe even though repository-scoped pull authorization succeeds.
        foreach ($repository in @(
            "miracle060811/travelmate-backend",
            "miracle060811/travelmate-frontend"
        )) {
            Test-GhcrPullAccess -Repository $repository -Username $GitHubUsername -PlainToken $plainToken
        }
        Save-DockerCredential -Registry "ghcr.io" -Username $GitHubUsername -PlainToken $plainToken
        $dockerLoginOutput = @(
            "Docker's unscoped GHCR login probe was rejected; repository-scoped pull access was verified.",
            "The credential was saved through Docker Desktop's configured credential helper."
        )
    }

    foreach ($image in @(
        "ghcr.io/miracle060811/travelmate-backend:deploy",
        "ghcr.io/miracle060811/travelmate-frontend:deploy"
    )) {
        Write-Output "Pulling $image ..."
        & docker pull $image
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to pull private image: $image"
        }
    }

    $auth = ConvertTo-Base64 "${GitHubUsername}:$plainToken"
    $dockerConfig = [ordered]@{
        auths = [ordered]@{
            "ghcr.io" = [ordered]@{
                username = $GitHubUsername
                password = $plainToken
                auth = $auth
            }
        }
    } | ConvertTo-Json -Depth 6 -Compress

    $secret = [ordered]@{
        apiVersion = "v1"
        kind = "Secret"
        metadata = [ordered]@{
            name = "travelmate-ghcr"
            namespace = $Namespace
            labels = [ordered]@{
                "app.kubernetes.io/name" = "travelmate-ghcr"
                "app.kubernetes.io/part-of" = "travelmate"
            }
        }
        type = "kubernetes.io/dockerconfigjson"
        data = [ordered]@{
            ".dockerconfigjson" = ConvertTo-Base64 $dockerConfig
        }
    }

    $secretJson = $secret | ConvertTo-Json -Depth 8 -Compress
    $applyOutput = $secretJson | & kubectl apply -f - 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to configure the Kubernetes GHCR pull secret"
    }

    Write-Output ($dockerLoginOutput -join [Environment]::NewLine)
    Write-Output ($applyOutput -join [Environment]::NewLine)
    Write-Output "Private GHCR access is configured for Docker and namespace '$Namespace'."
}
finally {
    if ($tokenPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($tokenPointer)
    }
    $plainToken = $null
    $dockerConfig = $null
    $secretJson = $null
    $Token = $null
}
