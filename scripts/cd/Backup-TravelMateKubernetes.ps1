[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$OutputRoot = "",
    [string]$ObjectStorageEndpoint = "",
    [switch]$SkipObjectStorageBackup
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

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

function Write-Utf8File {
    param(
        [string]$Path,
        [string]$Content
    )

    $encoding = [Text.UTF8Encoding]::new($false)
    [IO.File]::WriteAllText($Path, $Content, $encoding)
}

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
    throw "MinIO Client 'mc' is required for an S3 object backup. Install it with: winget install --id MinIO.Client --exact"
}

if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is not available"
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $repositoryRoot "backups\kubernetes"
}
$OutputRoot = [IO.Path]::GetFullPath($OutputRoot)

$contexts = @(Get-KubectlText config get-contexts --output=name) -split "`r?`n"
if ($contexts -notcontains $KubeContext) {
    throw "Kubernetes context '$KubeContext' is not available"
}

Invoke-Kubectl get namespace $Namespace --request-timeout=15s | Out-Null

$mysqlPod = (Get-KubectlText get pods -n $Namespace `
    -l app.kubernetes.io/name=travelmate-mysql `
    --output="jsonpath={.items[0].metadata.name}").Trim()
$backendPod = (Get-KubectlText get pods -n $Namespace `
    -l app.kubernetes.io/name=travelmate-backend `
    --output="jsonpath={.items[0].metadata.name}").Trim()

if (-not $mysqlPod) {
    throw "TravelMate MySQL Pod was not found"
}
if (-not $backendPod) {
    throw "TravelMate backend Pod was not found"
}

$storageType = (Get-KubectlText get configmap travelmate-config -n $Namespace `
    --output="jsonpath={.data.STORAGE_TYPE}").Trim().ToLowerInvariant()
if (-not $storageType) {
    $storageType = "local"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupDirectory = Join-Path $OutputRoot "travelmate-$timestamp"
$mysqlDirectory = Join-Path $backupDirectory "mysql"
$uploadsDirectory = Join-Path $backupDirectory "uploads"
$objectsDirectory = Join-Path $backupDirectory "objects"
$manifestsDirectory = Join-Path $backupDirectory "manifests"

foreach ($directory in @($backupDirectory, $mysqlDirectory, $uploadsDirectory, $manifestsDirectory)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}

$remoteDump = "/tmp/travelmate-$timestamp.sql"
$localDump = Join-Path $mysqlDirectory "travelmate.sql"

try {
    Invoke-Kubectl exec -n $Namespace $mysqlPod -c mysql "--" sh -ec `
        'mysqldump --user=root --password="$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers --events --hex-blob --no-tablespaces --set-gtid-purged=OFF --default-character-set=utf8mb4 --add-drop-database --databases "$MYSQL_DATABASE" --result-file="$1"' `
        "--" $remoteDump
    Push-Location $backupDirectory
    try {
        Invoke-Kubectl cp "$Namespace/${mysqlPod}:$remoteDump" "mysql/travelmate.sql" -c mysql
    }
    finally {
        Pop-Location
    }
}
finally {
    & kubectl --context $KubeContext exec -n $Namespace $mysqlPod -c mysql -- rm -f $remoteDump 2>$null
}

$objectStorageBackedUp = $false
if ($storageType -eq "s3") {
    if ($SkipObjectStorageBackup) {
        Write-Warning "Object storage backup was explicitly skipped. Database backup will continue."
    }
    else {
        $mcPath = Find-MinIoClient

        $bucket = (Get-KubectlText get configmap travelmate-config -n $Namespace `
            --output="jsonpath={.data.S3_BUCKET}").Trim()
        if (-not $bucket) {
            throw "S3_BUCKET is missing from travelmate-config"
        }
        if (-not $ObjectStorageEndpoint) {
            $ObjectStorageEndpoint = (Get-KubectlText get configmap travelmate-config -n $Namespace `
                --output="jsonpath={.data.S3_ENDPOINT}").Trim()
            if ($ObjectStorageEndpoint -eq "http://travelmate-minio:9000") {
                $ObjectStorageEndpoint = "http://127.0.0.1:30900"
            }
        }
        if (-not $ObjectStorageEndpoint) {
            throw "Object storage endpoint is unavailable. Provide -ObjectStorageEndpoint with a host-reachable S3 endpoint."
        }

        $accessKeyBase64 = (Get-KubectlText get secret travelmate-secrets -n $Namespace `
            --output="jsonpath={.data.s3-access-key}").Trim()
        $secretKeyBase64 = (Get-KubectlText get secret travelmate-secrets -n $Namespace `
            --output="jsonpath={.data.s3-secret-key}").Trim()
        if (-not $accessKeyBase64 -or -not $secretKeyBase64) {
            throw "S3 credentials are missing from travelmate-secrets"
        }

        $accessKey = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($accessKeyBase64))
        $secretKey = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($secretKeyBase64))
        $endpoint = [Uri]$ObjectStorageEndpoint
        $endpointAuthority = $endpoint.GetLeftPart([UriPartial]::Authority)
        $encodedAccessKey = [Uri]::EscapeDataString($accessKey)
        $encodedSecretKey = [Uri]::EscapeDataString($secretKey)
        $env:MC_HOST_travelmate_backup = $endpointAuthority.Replace("://", "://$encodedAccessKey`:$encodedSecretKey@")
        $mcConfigDirectory = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-mc-backup-" + [guid]::NewGuid())
        New-Item -ItemType Directory -Path $objectsDirectory -Force | Out-Null
        try {
            & $mcPath --config-dir $mcConfigDirectory mirror --overwrite "travelmate_backup/$bucket" $objectsDirectory
            if ($LASTEXITCODE -ne 0) {
                throw "mc mirror failed while backing up bucket '$bucket'"
            }
            $objectStorageBackedUp = $true
        }
        finally {
            Remove-Item Env:MC_HOST_travelmate_backup -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath $mcConfigDirectory -Recurse -Force -ErrorAction SilentlyContinue
            $accessKey = $null
            $secretKey = $null
        }
    }
}
else {
    Push-Location $backupDirectory
    try {
        Invoke-Kubectl cp "$Namespace/${backendPod}:/app/uploads/." "uploads" -c backend
    }
    finally {
        Pop-Location
    }
}

$sourceManifests = Join-Path $repositoryRoot "deploy\k8s"
Copy-Item -LiteralPath $sourceManifests -Destination (Join-Path $manifestsDirectory "repository") -Recurse

$liveResources = Get-KubectlText get "deployment,statefulset,service,configmap,pvc" -n $Namespace --output=yaml
Write-Utf8File -Path (Join-Path $manifestsDirectory "live-resources-without-secrets.yaml") `
    -Content ($liveResources + [Environment]::NewLine)

$secretNames = Get-KubectlText get secrets -n $Namespace --output="jsonpath={range .items[*]}{.metadata.name}{'\n'}{end}"
Write-Utf8File -Path (Join-Path $manifestsDirectory "secret-names.txt") `
    -Content ($secretNames.TrimEnd() + [Environment]::NewLine)

$metadata = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    kubeContext = $KubeContext
    namespace = $Namespace
    mysqlPod = $mysqlPod
    backendPod = $backendPod
    storageType = $storageType
    objectStorageBackedUp = $objectStorageBackedUp
    objectStorageEndpoint = if ($storageType -eq "s3") { $ObjectStorageEndpoint } else { "" }
    secretsExported = $false
    secretRecovery = @(
        "Run scripts/cd/Configure-TravelMateGhcrCredential.ps1 after recreating the cluster.",
        "Run scripts/cd/Initialize-TravelMateKubernetes.ps1 to create local application secrets."
    )
}
Write-Utf8File -Path (Join-Path $backupDirectory "metadata.json") `
    -Content (($metadata | ConvertTo-Json -Depth 6) + [Environment]::NewLine)

$checksumLines = Get-ChildItem -LiteralPath $backupDirectory -Recurse -File |
    Where-Object { $_.Name -ne "checksums.sha256" } |
    Sort-Object FullName |
    ForEach-Object {
        $relativePath = $_.FullName.Substring($backupDirectory.Length + 1).Replace("\", "/")
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $relativePath"
    }
Write-Utf8File -Path (Join-Path $backupDirectory "checksums.sha256") `
    -Content (($checksumLines -join [Environment]::NewLine) + [Environment]::NewLine)

$dumpInfo = Get-Item -LiteralPath $localDump
$uploadFiles = @(Get-ChildItem -LiteralPath $uploadsDirectory -Recurse -File)
$objectFiles = @(
    if (Test-Path -LiteralPath $objectsDirectory) {
        Get-ChildItem -LiteralPath $objectsDirectory -Recurse -File
    }
)
Write-Output "Backup completed: $backupDirectory"
Write-Output "MySQL dump: $($dumpInfo.Length) bytes"
Write-Output "Upload files: $($uploadFiles.Count)"
Write-Output "Object files: $($objectFiles.Count)"
Write-Output "Secret values were not exported."
