[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$DeepSeekApiKey = "",
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
$namespaceManifest = Join-Path $kubernetesDirectory "namespace.yaml"
$initSql = Join-Path $repositoryRoot "docs\sql\init.sql"

foreach ($path in @($namespaceManifest, $initSql, (Join-Path $kubernetesDirectory "kustomization.yaml"))) {
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

Invoke-Kubectl apply -k $kubernetesDirectory
Invoke-Kubectl rollout status statefulset/travelmate-mysql -n $Namespace --timeout=300s
Invoke-Kubectl rollout status deployment/travelmate-redis -n $Namespace --timeout=180s

if (-not $InfrastructureOnly) {
    Invoke-Kubectl rollout status deployment/travelmate-backend -n $Namespace --timeout=360s
    Invoke-Kubectl rollout status deployment/travelmate-frontend -n $Namespace --timeout=240s
}

Write-Output "TravelMate Kubernetes resources are initialized in namespace '$Namespace'."
