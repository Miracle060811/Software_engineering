[CmdletBinding()]
param(
    [string]$TaskName = "TravelMate-CD",
    [int]$IntervalMinutes = 5,
    [switch]$DoNotStart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if ($IntervalMinutes -lt 1) {
    throw "IntervalMinutes must be at least 1"
}

$supportFiles = @(
    "Deploy-TravelMate.ps1",
    "Ensure-KindProxy.ps1",
    "kind-proxy-forwarder.py"
)
foreach ($file in $supportFiles) {
    $source = Join-Path $PSScriptRoot $file
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        throw "Deployment support file is missing: $source"
    }
}

$installDirectory = Join-Path $env:LOCALAPPDATA "TravelMateCD"
[IO.Directory]::CreateDirectory($installDirectory) | Out-Null
$installedScript = Join-Path $installDirectory "Deploy-TravelMate.ps1"
foreach ($file in $supportFiles) {
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination (Join-Path $installDirectory $file) -Force
}

$pwsh = (Get-Command pwsh.exe -ErrorAction Stop).Source
$arguments = "-NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$installedScript`""
$action = New-ScheduledTaskAction -Execute $pwsh -Argument $arguments
$logonTrigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$repeatTrigger = New-ScheduledTaskTrigger `
    -Once `
    -At (Get-Date).AddMinutes(1) `
    -RepetitionInterval (New-TimeSpan -Minutes $IntervalMinutes) `
    -RepetitionDuration (New-TimeSpan -Days 3650)
$settings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Minutes 15) `
    -MultipleInstances IgnoreNew
$principal = New-ScheduledTaskPrincipal `
    -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive `
    -RunLevel Limited

Register-ScheduledTask `
    -TaskName $TaskName `
    -Action $action `
    -Trigger @($logonTrigger, $repeatTrigger) `
    -Settings $settings `
    -Principal $principal `
    -Description "Poll approved TravelMate GHCR images and roll out matching commits to Docker Desktop Kubernetes." `
    -Force | Out-Null

if (-not $DoNotStart) {
    Start-ScheduledTask -TaskName $TaskName
}

Write-Output "Installed scheduled task '$TaskName'. Script: $installedScript"
