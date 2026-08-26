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

    $dockerLoginOutput = $plainToken | & docker login ghcr.io --username $GitHubUsername --password-stdin 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Docker login to GHCR failed"
    }

    foreach ($image in @(
        "ghcr.io/miracle060811/travelmate-backend:deploy",
        "ghcr.io/miracle060811/travelmate-frontend:deploy"
    )) {
        & docker pull $image | Out-Null
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
