[CmdletBinding()]
param(
    [string]$OutputPath,
    [switch]$ExcludePpt
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $suffix = if ($ExcludePpt) { 'non-ppt-draft' } else { 'final' }
    $OutputPath = Join-Path $repoRoot "deliverables\TravelMate-2026-09-02-$suffix.zip"
}
$absoluteOutput = [IO.Path]::GetFullPath($OutputPath)
$deliverablesRoot = [IO.Path]::GetFullPath((Join-Path $repoRoot 'deliverables'))
if (-not $absoluteOutput.StartsWith($deliverablesRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw '归档输出必须位于仓库 deliverables 目录。'
}

$relativeFiles = @(& git -C $repoRoot -c core.quotepath=false ls-files --cached --others --exclude-standard)
if ($LASTEXITCODE -ne 0) {
    throw '无法读取 Git 文件清单。'
}
$relativeFiles = @($relativeFiles | Where-Object {
    $_ -and
    -not $_.StartsWith('deliverables/') -and
    (-not $ExcludePpt -or ([IO.Path]::GetExtension($_) -notin @('.ppt', '.pptx')))
} | Sort-Object -Unique)

[IO.Directory]::CreateDirectory((Split-Path $absoluteOutput -Parent)) | Out-Null
if (Test-Path -LiteralPath $absoluteOutput) {
    Remove-Item -LiteralPath $absoluteOutput -Force
}

Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$stream = [IO.File]::Open($absoluteOutput, [IO.FileMode]::CreateNew)
try {
    $archive = [IO.Compression.ZipArchive]::new($stream, [IO.Compression.ZipArchiveMode]::Create, $false)
    try {
        foreach ($relative in $relativeFiles) {
            $source = Join-Path $repoRoot $relative
            if (Test-Path -LiteralPath $source -PathType Leaf) {
                [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
                    $archive,
                    $source,
                    ($relative -replace '\\', '/'),
                    [IO.Compression.CompressionLevel]::Optimal
                ) | Out-Null
            }
        }
    } finally {
        $archive.Dispose()
    }
} finally {
    $stream.Dispose()
}

$hash = (Get-FileHash -LiteralPath $absoluteOutput -Algorithm SHA256).Hash.ToLowerInvariant()
$manifestPath = Join-Path $deliverablesRoot 'archive-manifest.txt'
$manifest = @(
    "archive=$([IO.Path]::GetFileName($absoluteOutput))",
    "generatedAt=$((Get-Date).ToString('o'))",
    "sourceHead=$(& git -C $repoRoot rev-parse HEAD)",
    "excludePpt=$([bool]$ExcludePpt)",
    "files=$($relativeFiles.Count)",
    "bytes=$((Get-Item -LiteralPath $absoluteOutput).Length)",
    "sha256=$hash",
    'status=draft-until-member-confirmation-and-remaining-day-materials'
)
[IO.File]::WriteAllLines($manifestPath, [string[]]$manifest, [Text.UTF8Encoding]::new($false))
$manifest
