[CmdletBinding()]
param(
    [string]$Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $Path) {
    $Path = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"
}

function New-RandomBytes {
    param([int]$Length)
    $bytes = New-Object byte[] $Length
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
        return $bytes
    }
    finally {
        $generator.Dispose()
    }
}

function New-HexSecret {
    param([int]$Length)
    return ((New-RandomBytes -Length $Length) | ForEach-Object { $_.ToString("x2") }) -join ""
}

function Get-ConfiguredKeys {
    param([string[]]$Lines)
    $keys = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    foreach ($line in $Lines) {
        if ($line -match '^\s*([A-Z_][A-Z0-9_]*)\s*=') {
            $null = $keys.Add($Matches[1])
        }
    }
    return ,$keys
}

$fullPath = [IO.Path]::GetFullPath($Path)
if (-not (Test-Path -LiteralPath $fullPath -PathType Leaf)) {
    $example = Join-Path (Split-Path -Parent $fullPath) ".env.example"
    if (-not (Test-Path -LiteralPath $example -PathType Leaf)) {
        throw "Missing .env.example: $example"
    }
    Copy-Item -LiteralPath $example -Destination $fullPath
}

$lines = @(Get-Content -LiteralPath $fullPath)
$keys = Get-ConfiguredKeys -Lines $lines
$additions = [ordered]@{}

if (-not $keys.Contains("APP_ENV")) { $additions.APP_ENV = "local" }
if (-not $keys.Contains("JWT_SECRET")) {
    $additions.JWT_SECRET = [Convert]::ToBase64String((New-RandomBytes -Length 64))
}
if (-not $keys.Contains("JWT_ACCESS_TOKEN_MINUTES")) { $additions.JWT_ACCESS_TOKEN_MINUTES = "30" }
if (-not $keys.Contains("REFRESH_TOKEN_DAYS")) { $additions.REFRESH_TOKEN_DAYS = "14" }
if (-not $keys.Contains("RATE_LIMIT_ENABLED")) { $additions.RATE_LIMIT_ENABLED = "true" }
if (-not $keys.Contains("REFRESH_COOKIE_SECURE")) { $additions.REFRESH_COOKIE_SECURE = "false" }
if (-not $keys.Contains("ADMIN_REGISTER_ENABLED")) { $additions.ADMIN_REGISTER_ENABLED = "false" }
if (-not $keys.Contains("ADMIN_REGISTER_EXPIRES_AT")) { $additions.ADMIN_REGISTER_EXPIRES_AT = "" }
if (-not $keys.Contains("STORAGE_TYPE")) { $additions.STORAGE_TYPE = "local" }
if (-not $keys.Contains("UPLOAD_DIR")) { $additions.UPLOAD_DIR = "uploads" }
if (-not $keys.Contains("S3_ENDPOINT")) { $additions.S3_ENDPOINT = "http://127.0.0.1:9000" }
if (-not $keys.Contains("S3_REGION")) { $additions.S3_REGION = "us-east-1" }
if (-not $keys.Contains("S3_BUCKET")) { $additions.S3_BUCKET = "travelmate" }
if (-not $keys.Contains("S3_ACCESS_KEY")) { $additions.S3_ACCESS_KEY = "travelmate" + (New-HexSecret -Length 6) }
if (-not $keys.Contains("S3_SECRET_KEY")) { $additions.S3_SECRET_KEY = New-HexSecret -Length 32 }
if (-not $keys.Contains("S3_PUBLIC_BASE_URL")) { $additions.S3_PUBLIC_BASE_URL = "http://localhost:9000/travelmate" }
if (-not $keys.Contains("MINIO_API_PORT")) { $additions.MINIO_API_PORT = "9000" }
if (-not $keys.Contains("MINIO_CONSOLE_PORT")) { $additions.MINIO_CONSOLE_PORT = "19001" }

if ($additions.Count -gt 0) {
    $append = @("", "# Added by scripts/Initialize-TravelMateLocalEnv.ps1")
    foreach ($entry in $additions.GetEnumerator()) {
        $append += ('{0}="{1}"' -f $entry.Key, $entry.Value)
    }
    $appendText = ($append -join [Environment]::NewLine) + [Environment]::NewLine
    [IO.File]::AppendAllText($fullPath, $appendText, [Text.UTF8Encoding]::new($false))
}

Write-Output "Local .env is initialized. Added keys: $($additions.Keys -join ', ')"
Write-Output "Secret values were not printed."
