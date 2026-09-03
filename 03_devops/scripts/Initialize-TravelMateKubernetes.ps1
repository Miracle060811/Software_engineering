[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$DeepSeekApiKey = "",
    [ValidateSet("base", "local", "server")]
    [string]$Environment = "local",
    [switch]$InfrastructureOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Kubectl {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & kubectl @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl $($Arguments -join ' ')"
    }
}

function New-RandomBytes {
    param([int]$Length)

    $bytes = [byte[]]::new($Length)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return $bytes
}

function New-HexSecret {
    param([int]$Length = 32)
    return ([BitConverter]::ToString((New-RandomBytes -Length $Length))).Replace("-", "").ToLowerInvariant()
}

function ConvertTo-SecretData {
    param([string]$Value)
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$kubernetesDirectory = Join-Path $repositoryRoot "deploy\k8s"
$deploymentDirectory = if ($Environment -eq "base") {
    $kubernetesDirectory
}
else {
    Join-Path $repositoryRoot "deploy\k8s-overlays\$Environment"
}
$namespaceManifest = Join-Path $kubernetesDirectory "namespace.yaml"
$initSql = Join-Path $repositoryRoot "docs\sql\init.sql"

foreach ($path in @($namespaceManifest, $initSql, (Join-Path $deploymentDirectory "kustomization.yaml"))) {
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Required file is missing: $path"
    }
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is not available"
}

$contexts = @(& kubectl config get-contexts -o name 2>$null)
if ($LASTEXITCODE -ne 0 -or $contexts -notcontains $KubeContext) {
    throw "Kubernetes context '$KubeContext' is not available"
}
Invoke-Kubectl config use-context $KubeContext
Invoke-Kubectl get nodes --request-timeout=15s
& (Join-Path $PSScriptRoot "Ensure-KindProxy.ps1")
if ($LASTEXITCODE -ne 0) {
    throw "Unable to prepare Kind node networking"
}
Invoke-Kubectl apply -f $namespaceManifest

$existingSecret = & kubectl get secret travelmate-secrets -n $Namespace -o name --ignore-not-found
if ($LASTEXITCODE -ne 0) {
    throw "Unable to inspect travelmate-secrets"
}

if (-not $existingSecret) {
    $mysqlRootPassword = New-HexSecret
    $mysqlPassword = New-HexSecret
    $jwtSecret = [Convert]::ToBase64String((New-RandomBytes -Length 64))
    $adminRegisterSecret = New-HexSecret

    $secret = [ordered]@{
        apiVersion = "v1"
        kind = "Secret"
        metadata = [ordered]@{
            name = "travelmate-secrets"
            namespace = $Namespace
            labels = [ordered]@{
                "app.kubernetes.io/name" = "travelmate-secrets"
                "app.kubernetes.io/part-of" = "travelmate"
            }
        }
        type = "Opaque"
        data = [ordered]@{
            "mysql-root-password" = ConvertTo-SecretData $mysqlRootPassword
            "mysql-password" = ConvertTo-SecretData $mysqlPassword
            "jwt-secret" = ConvertTo-SecretData $jwtSecret
            "admin-register-secret" = ConvertTo-SecretData $adminRegisterSecret
            "deepseek-api-key" = ConvertTo-SecretData $DeepSeekApiKey
            "s3-access-key" = ConvertTo-SecretData ("travelmate" + (New-HexSecret -Length 6))
            "s3-secret-key" = ConvertTo-SecretData (New-HexSecret -Length 32)
        }
    }

    $secretJson = $secret | ConvertTo-Json -Depth 8 -Compress
    $secretJson | & kubectl apply -f -
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to create travelmate-secrets"
    }
    Write-Output "Created travelmate-secrets without printing secret values."
}
else {
    Write-Output "Reusing existing travelmate-secrets; values were not rotated."
}

$secretState = & kubectl get secret travelmate-secrets -n $Namespace -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) {
    throw "Unable to read travelmate-secrets"
}
$missingStorageSecrets = [ordered]@{}
if (-not $secretState.data.PSObject.Properties['s3-access-key']) {
    $missingStorageSecrets['s3-access-key'] = "travelmate" + (New-HexSecret -Length 6)
}
if (-not $secretState.data.PSObject.Properties['s3-secret-key']) {
    $missingStorageSecrets['s3-secret-key'] = New-HexSecret -Length 32
}
if ($missingStorageSecrets.Count -gt 0) {
    $storagePatch = @{ stringData = $missingStorageSecrets } | ConvertTo-Json -Depth 4 -Compress
    $storagePatchFile = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-storage-" + [guid]::NewGuid() + ".json")
    try {
        [IO.File]::WriteAllText($storagePatchFile, $storagePatch, [Text.UTF8Encoding]::new($false))
        & kubectl patch secret travelmate-secrets -n $Namespace --type=merge --patch-file=$storagePatchFile
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to add object-storage credentials to travelmate-secrets"
        }
    }
    finally {
        Remove-Item -LiteralPath $storagePatchFile -Force -ErrorAction SilentlyContinue
    }
    Write-Output "Added missing object-storage credentials without printing secret values."
}

$initConfigMap = & kubectl create configmap travelmate-mysql-init `
    -n $Namespace `
    "--from-file=init.sql=$initSql" `
    --dry-run=client `
    -o json
if ($LASTEXITCODE -ne 0) {
    throw "Unable to generate the MySQL initialization ConfigMap"
}
$initConfigMap | & kubectl apply -f -
if ($LASTEXITCODE -ne 0) {
    throw "Unable to apply the MySQL initialization ConfigMap"
}

Invoke-Kubectl apply -k $deploymentDirectory
Invoke-Kubectl rollout status statefulset/travelmate-mysql -n $Namespace --timeout=300s
Invoke-Kubectl rollout status deployment/travelmate-redis -n $Namespace --timeout=180s
if ($Environment -eq "local") {
    Invoke-Kubectl rollout status statefulset/travelmate-minio -n $Namespace --timeout=300s
}

if (-not $InfrastructureOnly) {
    Invoke-Kubectl rollout status deployment/travelmate-backend -n $Namespace --timeout=360s
    Invoke-Kubectl rollout status deployment/travelmate-frontend -n $Namespace --timeout=240s
}

Write-Output "TravelMate Kubernetes '$Environment' resources are initialized in namespace '$Namespace'."
