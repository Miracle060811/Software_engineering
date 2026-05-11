[CmdletBinding()]
param(
    [string]$DbPassword,
    [string]$DeepseekApiKey,
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$SkipFrontendInstall,
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Test-CommandExists {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Quote-PowerShellLiteral {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return $Value -replace "'", "''"
}

function Get-BackendWaitCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$ProbeUrl,
        [int]$TimeoutSeconds = 60
    )

    $escapedProbeUrl = Quote-PowerShellLiteral -Value $ProbeUrl

    return @"
`$backendReady = `$false
`$backendDeadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt `$backendDeadline) {
    try {
        `$null = Invoke-RestMethod -Uri '$escapedProbeUrl' -Method Get -TimeoutSec 3 -ErrorAction Stop
        `$backendReady = `$true
        break
    } catch {
        Start-Sleep -Seconds 1
    }
}
if (-not `$backendReady) {
    Write-Warning 'Backend did not become ready within $TimeoutSeconds seconds. Starting frontend anyway.'
}
"@
}

function Get-DbPasswordFromLocalConfig {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $inSpring = $false
    $inDatasource = $false

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match '^\s*#') {
            continue
        }

        if ($line -match '^\S') {
            $inSpring = $line -match '^spring:\s*$'
            $inDatasource = $false
            continue
        }

        if ($inSpring -and $line -match '^\s{2}datasource:\s*$') {
            $inDatasource = $true
            continue
        }

        if ($inDatasource -and $line -match '^\s{4}password:\s*(?<Value>.+?)\s*$') {
            $password = $Matches['Value'].Trim()

            if ((($password.StartsWith('"')) -and ($password.EndsWith('"'))) -or (($password.StartsWith("'")) -and ($password.EndsWith("'")))) {
                $password = $password.Substring(1, $password.Length - 2)
            }

            return $password
        }

        if ($inSpring -and $line -match '^\s{2}\S') {
            $inDatasource = $false
        }
    }

    return $null
}

function Start-ServiceWindow {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Title,
        [Parameter(Mandatory = $true)]
        [string]$WorkingDirectory,
        [Parameter(Mandatory = $true)]
        [string]$Command,
        [switch]$DryRun
    )

    $escapedWorkingDirectory = Quote-PowerShellLiteral -Value $WorkingDirectory
    $escapedTitle = Quote-PowerShellLiteral -Value $Title
    $bootstrapCommand = "Set-Location -LiteralPath '$escapedWorkingDirectory'; `$Host.UI.RawUI.WindowTitle = '$escapedTitle'; $Command"

    if ($DryRun) {
        Write-Host "[$Title] $bootstrapCommand"
        return
    }

    Start-Process -FilePath "powershell.exe" -WorkingDirectory $WorkingDirectory -ArgumentList @(
        "-NoExit",
        "-Command",
        $bootstrapCommand
    ) | Out-Null

    Write-Host "Started $Title"
}

if ($BackendOnly -and $FrontendOnly) {
    throw "BackendOnly and FrontendOnly cannot be used together."
}

$repoRoot = $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"
$backendLocalConfig = Join-Path $backendDir "application-local.yml"

$runBackend = -not $FrontendOnly
$runFrontend = -not $BackendOnly
$dbPasswordSource = $null
$resolvedDbPassword = $null

if ($DbPassword) {
    $resolvedDbPassword = $DbPassword
    $dbPasswordSource = "-DbPassword 参数"
} elseif ($env:SPRING_DATASOURCE_PASSWORD) {
    $resolvedDbPassword = $env:SPRING_DATASOURCE_PASSWORD
    $dbPasswordSource = "当前 PowerShell 环境变量 SPRING_DATASOURCE_PASSWORD"
} elseif ($env:DB_PASSWORD) {
    $resolvedDbPassword = $env:DB_PASSWORD
    $dbPasswordSource = "当前 PowerShell 环境变量 DB_PASSWORD"
} elseif (Test-Path $backendLocalConfig) {
    $localDbPassword = Get-DbPasswordFromLocalConfig -Path $backendLocalConfig

    if ($localDbPassword) {
        $resolvedDbPassword = $localDbPassword
        $dbPasswordSource = "backend/application-local.yml"
    }
}

if ($resolvedDbPassword) {
    $env:SPRING_DATASOURCE_PASSWORD = $resolvedDbPassword
}

if ($DeepseekApiKey) {
    $env:DEEPSEEK_API_KEY = $DeepseekApiKey
}

if ($runBackend) {
    if (-not (Test-Path $backendDir)) {
        throw "Backend directory not found: $backendDir"
    }

    if (-not (Test-CommandExists -Name "java")) {
        throw "Java is required before starting the backend."
    }

    if ((-not (Test-Path (Join-Path $backendDir "mvnw.cmd"))) -and (-not (Test-CommandExists -Name "mvn"))) {
        throw "Neither mvnw.cmd nor mvn was found for the backend."
    }
}

if ($runFrontend) {
    if (-not (Test-Path $frontendDir)) {
        throw "Frontend directory not found: $frontendDir"
    }

    if (-not (Test-CommandExists -Name "node")) {
        throw "Node.js is required before starting the frontend."
    }

    if (-not (Test-CommandExists -Name "npm")) {
        throw "npm is required before starting the frontend."
    }
}

$backendLauncher = if (Test-Path (Join-Path $backendDir "mvnw.cmd")) {
    ".\\mvnw.cmd"
} else {
    "mvn"
}

$backendCommand = "$backendLauncher spring-boot:run"

$frontendNeedsInstall = -not (Test-Path (Join-Path $frontendDir "node_modules"))

if ($frontendNeedsInstall -and -not $SkipFrontendInstall) {
    $frontendCommand = "npm install; npm run dev"
} else {
    $frontendCommand = "npm run dev"
}

if ($runBackend -and $runFrontend) {
    $backendWaitCommand = Get-BackendWaitCommand -ProbeUrl "http://127.0.0.1:8080/api/post/list?page=1&size=1&keyword="
    $frontendCommand = "$backendWaitCommand`n$frontendCommand"
}

if ($runBackend -and -not $resolvedDbPassword -and -not (Test-Path $backendLocalConfig)) {
    Write-Warning "SPRING_DATASOURCE_PASSWORD / DB_PASSWORD is not set and backend/application-local.yml was not found."
}

if ($runBackend -and (Test-Path $backendLocalConfig) -and -not $resolvedDbPassword) {
    Write-Warning "backend/application-local.yml was found, but spring.datasource.password could not be read from it."
}

if ($runFrontend -and $frontendNeedsInstall -and $SkipFrontendInstall) {
    Write-Warning "frontend/node_modules was not found. The frontend may fail because -SkipFrontendInstall was used."
}

if ($runBackend) {
    if ($dbPasswordSource) {
        Write-Host "Backend DB password source: $dbPasswordSource"
    }

    Start-ServiceWindow -Title "TravelMate Backend" -WorkingDirectory $backendDir -Command $backendCommand -DryRun:$DryRun
}

if ($runFrontend) {
    Start-ServiceWindow -Title "TravelMate Frontend" -WorkingDirectory $frontendDir -Command $frontendCommand -DryRun:$DryRun
}

if ($DryRun) {
    return
}

Write-Host ""

if ($runBackend) {
    Write-Host "Backend URL : http://localhost:8080"
}

if ($runFrontend) {
    Write-Host "Frontend URL: http://localhost:3000"
}