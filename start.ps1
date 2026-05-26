[CmdletBinding()]
param(
    [string]$DbPassword,
    [string]$DeepseekApiKey,
    [switch]$BackendOnly,
    [switch]$FrontendOnly,
    [switch]$SkipRedis,
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

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $connectTask = $client.ConnectAsync($HostName, $Port)
        $connected = $connectTask.Wait(1000) -and $client.Connected
        $client.Dispose()
        return $connected
    } catch {
        return $false
    }
}

function Get-RedisServerPath {
    $command = Get-Command "redis-server" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidatePaths = @(
        (Join-Path $repoRoot "redis\redis-server.exe"),
        (Join-Path $repoRoot "tools\redis\redis-server.exe"),
        "C:\Program Files\Redis\redis-server.exe",
        "C:\Redis\redis-server.exe"
    )

    foreach ($candidate in $candidatePaths) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    return $null
}

function Quote-PowerShellLiteral {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value
    )

    return $Value -replace "'", "''"
}

function Import-EnvFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    $loadedKeys = @()

    if (-not (Test-Path $Path)) {
        return $loadedKeys
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()

        if (-not $trimmed -or $trimmed.StartsWith("#")) {
            continue
        }

        if ($trimmed -match '^([A-Z_][A-Z0-9_]*)\s*=\s*(.+)$') {
            $key = $Matches[1]
            $value = $Matches[2].Trim()

            if ((($value.StartsWith('"')) -and ($value.EndsWith('"'))) -or (($value.StartsWith("'")) -and ($value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            if (-not [Environment]::GetEnvironmentVariable($key, "Process")) {
                [Environment]::SetEnvironmentVariable($key, $value, "Process")
                $loadedKeys += $key
            }
        }
    }

    return $loadedKeys
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

function Start-RedisDependency {
    param(
        [switch]$DryRun
    )

    if (Test-TcpPort -HostName "127.0.0.1" -Port 6379) {
        Write-Host "Redis is already listening on 127.0.0.1:6379"
        return
    }

    $redisServices = Get-Service -Name "Redis", "Redis-x64-3.0", "redis" -ErrorAction SilentlyContinue
    $redisService = $redisServices | Select-Object -First 1
    if ($redisService) {
        if ($DryRun) {
            Write-Host "[Redis] Start-Service -Name $($redisService.Name)"
            return
        }

        if ($redisService.Status -ne "Running") {
            try {
                Start-Service -Name $redisService.Name
                Start-Sleep -Seconds 2
            } catch {
                Write-Warning "Redis service '$($redisService.Name)' was found but could not be started: $($_.Exception.Message)"
            }
        }

        if (Test-TcpPort -HostName "127.0.0.1" -Port 6379) {
            Write-Host "Started Redis service: $($redisService.Name)"
        } else {
            Write-Warning "Redis service '$($redisService.Name)' is not listening on 127.0.0.1:6379."
        }
        return
    }

    $redisServerPath = Get-RedisServerPath
    if ($redisServerPath) {
        $redisDir = Split-Path -Parent $redisServerPath
        $redisCommand = "& '$redisServerPath'"
        Start-ServiceWindow -Title "TravelMate Redis" -WorkingDirectory $redisDir -Command $redisCommand -DryRun:$DryRun

        if (-not $DryRun) {
            Start-Sleep -Seconds 2
            if (Test-TcpPort -HostName "127.0.0.1" -Port 6379) {
                Write-Host "Redis URL   : redis://127.0.0.1:6379"
            } else {
                Write-Warning "redis-server was started, but 127.0.0.1:6379 is still not reachable."
            }
        }
        return
    }

    Write-Warning "Redis is not running and redis-server was not found. Install Redis or start it manually on 127.0.0.1:6379. Use -SkipRedis to suppress this check."
}

if ($BackendOnly -and $FrontendOnly) {
    throw "BackendOnly and FrontendOnly cannot be used together."
}

$repoRoot = $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"
$frontendDir = Join-Path $repoRoot "frontend"
$backendLocalConfig = Join-Path $backendDir "application-local.yml"
$envFile = Join-Path $repoRoot ".env"
$loadedEnvKeys = Import-EnvFile -Path $envFile

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
    $dbPasswordSource = if ($loadedEnvKeys -contains "DB_PASSWORD") { ".env 文件" } else { "当前 PowerShell 环境变量 DB_PASSWORD" }
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

$backendCommand = "$backendLauncher clean spring-boot:run"

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

if ($runBackend -and -not $SkipRedis) {
    Start-RedisDependency -DryRun:$DryRun
}

if ($runBackend) {
    if ($dbPasswordSource) {
        Write-Host "Backend DB password source: $dbPasswordSource"
    }

    if ($DeepseekApiKey) {
        Write-Host "DeepSeek API key source: -DeepseekApiKey 参数"
    } elseif ($env:DEEPSEEK_API_KEY) {
        $deepseekKeySource = if ($loadedEnvKeys -contains "DEEPSEEK_API_KEY") { ".env 文件" } else { "当前 PowerShell 环境变量 DEEPSEEK_API_KEY" }
        Write-Host "DeepSeek API key source: $deepseekKeySource"
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
