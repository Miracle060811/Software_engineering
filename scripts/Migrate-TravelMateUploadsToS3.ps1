[CmdletBinding()]
param(
    [string]$SourceDirectory = "",
    [string]$Endpoint = "",
    [string]$Bucket = "",
    [string]$AccessKey = "",
    [string]$SecretKey = "",
    [string]$ReportDirectory = "",
    [switch]$Execute
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Import-LocalEnvironment {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $values
    }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*)\s*$') {
            $value = $Matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
                ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$Matches[1]] = $value
        }
    }
    return $values
}

function Find-MinIoClient {
    $command = Get-Command mc -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    $wingetPackages = Join-Path $env:LOCALAPPDATA "Microsoft\WinGet\Packages"
    if (Test-Path -LiteralPath $wingetPackages -PathType Container) {
        $candidate = Get-ChildItem -LiteralPath $wingetPackages -Recurse -Filter "mc.exe" -File `
            -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match 'MinIO\.Client' } |
            Select-Object -First 1
        if ($candidate) {
            return $candidate.FullName
        }
    }
    throw "MinIO Client 'mc' is required. Install it with: winget install --id MinIO.Client --exact"
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$localEnvironment = Import-LocalEnvironment -Path (Join-Path $repositoryRoot ".env")
if (-not $SourceDirectory) { $SourceDirectory = Join-Path $repositoryRoot "uploads" }
if (-not $Endpoint) { $Endpoint = $localEnvironment["S3_ENDPOINT"] }
if (-not $Bucket) { $Bucket = $localEnvironment["S3_BUCKET"] }
if (-not $AccessKey) { $AccessKey = $localEnvironment["S3_ACCESS_KEY"] }
if (-not $SecretKey) { $SecretKey = $localEnvironment["S3_SECRET_KEY"] }
if (-not $ReportDirectory) {
    $ReportDirectory = Join-Path $repositoryRoot ("backups\storage-migrations\uploads-" + (Get-Date -Format "yyyyMMdd-HHmmss"))
}

$SourceDirectory = [IO.Path]::GetFullPath($SourceDirectory)
$ReportDirectory = [IO.Path]::GetFullPath($ReportDirectory)
if (-not (Test-Path -LiteralPath $SourceDirectory -PathType Container)) {
    New-Item -ItemType Directory -Path $SourceDirectory -Force | Out-Null
}
New-Item -ItemType Directory -Path $ReportDirectory -Force | Out-Null

$allowedExtensions = [Collections.Generic.HashSet[string]]::new(
    [string[]]@(".jpg", ".jpeg", ".png", ".gif", ".webp"),
    [StringComparer]::OrdinalIgnoreCase)
$sourceFiles = @(Get-ChildItem -LiteralPath $SourceDirectory -Recurse -File |
    Where-Object { $allowedExtensions.Contains($_.Extension) } |
    Sort-Object FullName)
$rows = [Collections.Generic.List[object]]::new()
$mcPath = $null
$mcConfigDirectory = $null

try {
    if ($Execute) {
        foreach ($value in @($Endpoint, $Bucket, $AccessKey, $SecretKey)) {
            if (-not $value) { throw "Endpoint, bucket and credentials are required for execution" }
        }
        $mcPath = Find-MinIoClient
        $endpointUri = [Uri]$Endpoint
        $authority = $endpointUri.GetLeftPart([UriPartial]::Authority)
        $encodedAccess = [Uri]::EscapeDataString($AccessKey)
        $encodedSecret = [Uri]::EscapeDataString($SecretKey)
        $env:MC_HOST_travelmate_migration = $authority.Replace("://", "://$encodedAccess`:$encodedSecret@")
        $mcConfigDirectory = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-mc-migrate-" + [guid]::NewGuid())
    }

    foreach ($file in $sourceFiles) {
        $relativePath = $file.FullName.Substring($SourceDirectory.Length).TrimStart('\', '/').Replace('\', '/')
        if (-not $relativePath -or $relativePath.Contains("..")) {
            throw "Unsafe relative path: $($file.FullName)"
        }
        $objectKey = "uploads/$relativePath"
        $sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $status = "planned"

        if ($Execute) {
            $temporaryDownload = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-object-" + [guid]::NewGuid())
            try {
                & $mcPath --config-dir $mcConfigDirectory cp --attr "sha256=$sha256" `
                    $file.FullName "travelmate_migration/$Bucket/$objectKey"
                if ($LASTEXITCODE -ne 0) { throw "Upload failed: $objectKey" }
                & $mcPath --config-dir $mcConfigDirectory cp `
                    "travelmate_migration/$Bucket/$objectKey" $temporaryDownload
                if ($LASTEXITCODE -ne 0) { throw "Verification download failed: $objectKey" }
                $remoteHash = (Get-FileHash -LiteralPath $temporaryDownload -Algorithm SHA256).Hash.ToLowerInvariant()
                if ($remoteHash -ne $sha256) { throw "Hash mismatch: $objectKey" }
                $status = "verified"
            }
            finally {
                Remove-Item -LiteralPath $temporaryDownload -Force -ErrorAction SilentlyContinue
            }
        }

        $rows.Add([pscustomobject]@{
            SourcePath = $file.FullName
            LegacyUrl = "/uploads/$relativePath"
            ObjectKey = $objectKey
            Bytes = $file.Length
            Sha256 = $sha256
            Status = $status
        })
    }
}
finally {
    Remove-Item Env:MC_HOST_travelmate_migration -ErrorAction SilentlyContinue
    if ($mcConfigDirectory) {
        Remove-Item -LiteralPath $mcConfigDirectory -Recurse -Force -ErrorAction SilentlyContinue
    }
    $AccessKey = $null
    $SecretKey = $null
    $localEnvironment.Clear()
}

$reportPath = Join-Path $ReportDirectory "mapping.csv"
$rows | Export-Csv -LiteralPath $reportPath -NoTypeInformation -Encoding utf8
$totalBytes = if ($sourceFiles.Count -eq 0) {
    0
}
else {
    [long](($sourceFiles | Measure-Object Length -Sum).Sum)
}
$summary = [ordered]@{
    createdAt = (Get-Date).ToString("o")
    sourceDirectory = $SourceDirectory
    bucket = $Bucket
    executed = [bool]$Execute
    discoveredFiles = $sourceFiles.Count
    verifiedFiles = @($rows | Where-Object Status -eq "verified").Count
    totalBytes = $totalBytes
    mappingFile = $reportPath
    databaseReferencesUpdated = $false
    databaseUpdateNote = "Use ObjectKey from mapping.csv through an application-aware migration; this script never rewrites database fields automatically."
}
$summary | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $ReportDirectory "summary.json") -Encoding utf8

Write-Output "Upload migration completed. Files: $($sourceFiles.Count); executed: $([bool]$Execute)"
Write-Output "Mapping report: $reportPath"
Write-Output "Database references were not modified."
