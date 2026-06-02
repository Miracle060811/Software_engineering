[CmdletBinding()]
param(
    [string]$DbPassword,
    [string]$DeepseekApiKey,
    [switch]$SkipRedis,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootScript = Join-Path $PSScriptRoot "start.ps1"

if (-not (Test-Path -LiteralPath $rootScript)) {
    throw "start.ps1 was not found next to start-backend.ps1."
}

$startParams = @{
    BackendOnly = $true
}

if ($DbPassword) {
    $startParams.DbPassword = $DbPassword
}

if ($DeepseekApiKey) {
    $startParams.DeepseekApiKey = $DeepseekApiKey
}

if ($SkipRedis) {
    $startParams.SkipRedis = $true
}

if ($DryRun) {
    $startParams.DryRun = $true
}

& $rootScript @startParams
