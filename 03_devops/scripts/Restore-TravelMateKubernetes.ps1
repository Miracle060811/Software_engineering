[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$BackupDirectory,
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$ObjectStorageEndpoint = "",
    [switch]$ConfirmDataOverwrite
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Find-MinIoClient {
    $command = Get-Command mc -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $wingetPackages = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
    if (Test-Path -LiteralPath $wingetPackages -PathType Container) {
        $candidate = Get-ChildItem -LiteralPath $wingetPackages -Recurse -Filter "mc.exe" -File `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match 'MinIO\.Client' } |
            Select-Object -First 1
        if ($candidate) { return $candidate.FullName }
    }
    throw "MinIO Client 'mc' is required to restore the S3 object backup. Install it with: winget install --id MinIO.Client --exact"
}

function Invoke-Kubectl {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    & kubectl --context $KubeContext @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl --context $KubeContext $($Arguments -join ' ')"
    }
}

function Get-KubectlText {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $lines = @(& kubectl --context $KubeContext @Arguments)
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl --context $KubeContext $($Arguments -join ' ')"
    }
    return ($lines -join [Environment]::NewLine)
}

if (-not $ConfirmDataOverwrite) {
    throw "Restore drops and recreates the TravelMate database. Re-run with -ConfirmDataOverwrite after verifying the target cluster and backup."
}

$BackupDirectory = (Resolve-Path -LiteralPath $BackupDirectory).Path
$localDump = Join-Path $BackupDirectory "mysql\travelmate.sql"
$uploadsDirectory = Join-Path $BackupDirectory "uploads"
$objectsDirectory = Join-Path $BackupDirectory "objects"
$metadataFile = Join-Path $BackupDirectory "metadata.json"
$checksumsFile = Join-Path $BackupDirectory "checksums.sha256"

foreach ($path in @($localDump, $checksumsFile)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required backup content is missing: $path"
    }
}

$metadata = if (Test-Path -LiteralPath $metadataFile) { Get-Content -LiteralPath $metadataFile -Raw | ConvertFrom-Json } else { $null }
$storageType = if ($metadata -and $metadata.storageType) { [string]$metadata.storageType } else { "local" }
if ($storageType -eq "s3" -and -not (Test-Path -LiteralPath $objectsDirectory -PathType Container)) {
    throw "This backup does not contain object storage data and cannot perform a complete restore."
}
if ($storageType -ne "s3" -and -not (Test-Path -LiteralPath $uploadsDirectory -PathType Container)) {
    throw "Legacy uploads backup is missing: $uploadsDirectory"
}

$checksumFailures = [Collections.Generic.List[string]]::new()
foreach ($line in Get-Content -LiteralPath $checksumsFile) {
    if ($line -notmatch '^([0-9a-f]{64})  (.+)$') {
        throw "Invalid checksum entry: $line"
    }
    $expectedHash = $Matches[1]
    $relativePath = $Matches[2].Replace("/", [IO.Path]::DirectorySeparatorChar)
    $targetPath = Join-Path $BackupDirectory $relativePath
    if (-not (Test-Path -LiteralPath $targetPath -PathType Leaf)) {
        $checksumFailures.Add("missing: $relativePath")
        continue
    }
    $actualHash = (Get-FileHash -LiteralPath $targetPath -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actualHash -ne $expectedHash) {
        $checksumFailures.Add("hash mismatch: $relativePath")
    }
}
if ($checksumFailures.Count -gt 0) {
    throw "Backup integrity check failed: $($checksumFailures -join '; ')"
}

$mysqlPod = (Get-KubectlText get pods -n $Namespace `
    -l app.kubernetes.io/name=travelmate-mysql `
    --output="jsonpath={.items[0].metadata.name}").Trim()
$backendPod = (Get-KubectlText get pods -n $Namespace `
    -l app.kubernetes.io/name=travelmate-backend `
    --output="jsonpath={.items[0].metadata.name}").Trim()

if (-not $mysqlPod -or -not $backendPod) {
    throw "TravelMate Pods are not ready. Run Configure-TravelMateGhcrCredential.ps1 and Initialize-TravelMateKubernetes.ps1 first."
}

$remoteDump = "/tmp/travelmate-restore.sql"
try {
    Push-Location $BackupDirectory
    try {
        Invoke-Kubectl cp "mysql/travelmate.sql" "$Namespace/${mysqlPod}:$remoteDump" -c mysql
    }
    finally {
        Pop-Location
    }
    Invoke-Kubectl exec -n $Namespace $mysqlPod -c mysql "--" sh -ec `
        'mysql --user=root --password="$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 < "$1"' `
        "--" $remoteDump
}
finally {
    & kubectl --context $KubeContext exec -n $Namespace $mysqlPod -c mysql -- rm -f $remoteDump 2>$null
}

if ($storageType -eq "s3") {
    $mcPath = Find-MinIoClient
    $bucket = (Get-KubectlText get configmap travelmate-config -n $Namespace `
        --output="jsonpath={.data.S3_BUCKET}").Trim()
    if (-not $ObjectStorageEndpoint) {
        $ObjectStorageEndpoint = (Get-KubectlText get configmap travelmate-config -n $Namespace `
            --output="jsonpath={.data.S3_ENDPOINT}").Trim()
        if ($ObjectStorageEndpoint -eq "http://travelmate-minio:9000") {
            $ObjectStorageEndpoint = "http://127.0.0.1:30900"
        }
    }
    if (-not $bucket -or -not $ObjectStorageEndpoint) {
        throw "S3 bucket or host-reachable endpoint is unavailable"
    }

    $accessKeyBase64 = (Get-KubectlText get secret travelmate-secrets -n $Namespace `
        --output="jsonpath={.data.s3-access-key}").Trim()
    $secretKeyBase64 = (Get-KubectlText get secret travelmate-secrets -n $Namespace `
        --output="jsonpath={.data.s3-secret-key}").Trim()
    $accessKey = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($accessKeyBase64))
    $secretKey = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($secretKeyBase64))
    $endpoint = [Uri]$ObjectStorageEndpoint
    $endpointAuthority = $endpoint.GetLeftPart([UriPartial]::Authority)
    $encodedAccessKey = [Uri]::EscapeDataString($accessKey)
    $encodedSecretKey = [Uri]::EscapeDataString($secretKey)
    $env:MC_HOST_travelmate_restore = $endpointAuthority.Replace("://", "://$encodedAccessKey`:$encodedSecretKey@")
    $mcConfigDirectory = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-mc-restore-" + [guid]::NewGuid())
    try {
        & $mcPath --config-dir $mcConfigDirectory mirror --overwrite --remove $objectsDirectory "travelmate_restore/$bucket"
        if ($LASTEXITCODE -ne 0) {
            throw "mc mirror failed while restoring bucket '$bucket'"
        }
    }
    finally {
        Remove-Item Env:MC_HOST_travelmate_restore -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $mcConfigDirectory -Recurse -Force -ErrorAction SilentlyContinue
        $accessKey = $null
        $secretKey = $null
    }
}
else {
    Push-Location $BackupDirectory
    try {
        Invoke-Kubectl cp "uploads/." "$Namespace/${backendPod}:/app/uploads" -c backend
    }
    finally {
        Pop-Location
    }
}
Invoke-Kubectl rollout restart deployment/travelmate-backend -n $Namespace
Invoke-Kubectl rollout status deployment/travelmate-backend -n $Namespace --timeout=360s

Write-Output "Restore completed from: $BackupDirectory"
Write-Output "MySQL and $storageType file storage were restored; backend rollout is ready."
