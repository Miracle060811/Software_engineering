param(
    [switch]$SkipInstall,
    [switch]$SkipE2E
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Backend = Join-Path $Root "backend"
$Frontend = Join-Path $Root "frontend"

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Action
    )

    Write-Host ""
    Write-Host "==> $Name" -ForegroundColor Cyan
    & $Action
}

function Invoke-NativeCommand {
    param(
        [string]$Command,
        [string[]]$Arguments = @()
    )

    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $Command $($Arguments -join ' ')"
    }
}

Invoke-Step "Backend JUnit and MockMvc tests" {
    Push-Location $Backend
    try {
        Invoke-NativeCommand ".\mvnw.cmd" @("clean", "verify")
    } finally {
        Pop-Location
    }
}

Invoke-Step "Frontend dependency install" {
    if ($SkipInstall) {
        Write-Host "Skipped by -SkipInstall"
        return
    }

    Push-Location $Frontend
    try {
        Invoke-NativeCommand "npm" @("install")
    } finally {
        Pop-Location
    }
}

Invoke-Step "Frontend production build" {
    Push-Location $Frontend
    try {
        Invoke-NativeCommand "npm" @("run", "build")
    } finally {
        Pop-Location
    }
}

if (-not $SkipE2E) {
    Invoke-Step "Frontend Playwright E2E smoke tests" {
        Push-Location $Frontend
        try {
            Invoke-NativeCommand "npx" @("playwright", "test", "--reporter=list", "--workers=1")
        } finally {
            Pop-Location
        }
    }
}

Invoke-Step "Startup script dry run" {
    Push-Location $Root
    try {
        .\start.ps1 -DryRun
    } finally {
        Pop-Location
    }
}
