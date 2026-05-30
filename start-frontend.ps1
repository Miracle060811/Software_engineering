[CmdletBinding()]
param(
    [switch]$SkipFrontendInstall,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootScript = Join-Path $PSScriptRoot "start.ps1"

if (-not (Test-Path -LiteralPath $rootScript)) {
    throw "start.ps1 was not found next to start-frontend.ps1."
}

$startParams = @{
    FrontendOnly = $true
}

if ($SkipFrontendInstall) {
    $startParams.SkipFrontendInstall = $true
}

if ($DryRun) {
    $startParams.DryRun = $true
}

& $rootScript @startParams
