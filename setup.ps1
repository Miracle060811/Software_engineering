<#
  TravelMate 一键环境配置脚本
  用法: .\setup.ps1
    功能: 从 .env 文件加载环境变量，可选重建数据库，并启动后端服务
#>
param(
    [switch]$InitDb,      # 是否初始化数据库
        [switch]$ResetDb,     # 是否先删库再重建（用于修复中文导入乱码）
    [switch]$StartBackend # 是否启动后端
)

$envFile = Join-Path $PSScriptRoot ".env"
$backendMavenWrapper = Join-Path $PSScriptRoot "backend\mvnw.cmd"
$backendRunHint = if (Test-Path $backendMavenWrapper) {
    ".\\mvnw.cmd clean spring-boot:run"
} else {
    "mvn clean spring-boot:run"
}

function Get-MySqlExecutable {
    $mysqlCommand = Get-Command "mysql.exe" -ErrorAction SilentlyContinue
    if (-not $mysqlCommand) {
        $mysqlCommand = Get-Command "mysql" -ErrorAction SilentlyContinue
    }
    if ($mysqlCommand) {
        return $mysqlCommand.Source
    }

    $candidatePaths = @(
        "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
        "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe",
        "C:\xampp\mysql\bin\mysql.exe"
    )

    foreach ($candidatePath in $candidatePaths) {
        if (Test-Path $candidatePath) {
            return $candidatePath
        }
    }

    $mariaDbCandidate = Get-ChildItem -Path "C:\Program Files\MariaDB*\bin\mysql.exe" -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($mariaDbCandidate) {
        return $mariaDbCandidate.FullName
    }

    return $null
}

if (-not (Test-Path $envFile)) {
    Write-Host "[错误] 未找到 .env 文件！请先复制 .env.example 为 .env 并填入真实值:" -ForegroundColor Red
    Write-Host "  cp .env.example .env" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/3] 加载环境变量..." -ForegroundColor Cyan
Get-Content $envFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        if ($line -match '^([A-Z_]+)\s*=\s*(.+)$') {
            $key = $matches[1]
            $value = $matches[2].Trim().Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  已加载环境变量: $key"
        }
    }
}
Write-Host "  环境变量加载完成！" -ForegroundColor Green

# 初始化数据库
if ($InitDb) {
    Write-Host "[2/3] 初始化数据库..." -ForegroundColor Cyan
    $initSql = Join-Path $PSScriptRoot "docs\sql\init.sql"
    $databaseName = "travelmate"
    $mysqlExecutable = Get-MySqlExecutable
    if (-not (Test-Path $initSql)) {
        Write-Host "[错误] 未找到 docs\sql\init.sql" -ForegroundColor Red
        exit 1
    }
    if (-not $mysqlExecutable) {
        Write-Host "[错误] 未找到 mysql 客户端，请先安装 MySQL 或将 mysql.exe 加入 PATH" -ForegroundColor Red
        exit 1
    }
    $dbPassword = $env:DB_PASSWORD
    if (-not $dbPassword) {
        $dbPassword = Read-Host "请输入 MySQL root 密码"
    }
    Write-Host "  正在导入 init.sql 到 MySQL..."
    $previousMysqlPwd = [Environment]::GetEnvironmentVariable("MYSQL_PWD", "Process")
    [Environment]::SetEnvironmentVariable("MYSQL_PWD", $dbPassword, "Process")
    try {
        if ($ResetDb) {
            Write-Warning "ResetDb 已启用：将删除并重建数据库 $databaseName，用于修复中文导入乱码。"
            $resetArgs = @(
                "--default-character-set=utf8mb4",
                "-u",
                "root",
                "-e",
                "DROP DATABASE IF EXISTS $databaseName; CREATE DATABASE $databaseName CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
            )
            & $mysqlExecutable @resetArgs
            if ($LASTEXITCODE -ne 0) {
                throw "数据库重建失败，请检查 MySQL root 密码和服务状态。"
            }
        }
        $sourcePath = $initSql -replace '\\', '/'
        $sourceArgs = @(
            "--default-character-set=utf8mb4",
            "-u",
            "root",
            "-e",
            "SOURCE $sourcePath;"
        )
        & $mysqlExecutable @sourceArgs
    } finally {
        [Environment]::SetEnvironmentVariable("MYSQL_PWD", $previousMysqlPwd, "Process")
    }
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  数据库初始化完成！" -ForegroundColor Green
    } else {
        Write-Host "  数据库初始化失败，请检查密码和 MySQL 服务状态" -ForegroundColor Red
    }
} else {
    Write-Host "[2/3] 跳过数据库初始化 (使用 -InitDb 参数启用)" -ForegroundColor DarkGray
}

# 启动后端
if ($StartBackend) {
    Write-Host "[3/3] 启动后端服务..." -ForegroundColor Cyan
    Set-Location (Join-Path $PSScriptRoot "backend")
    $backendLauncher = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }
    & $backendLauncher clean spring-boot:run
} else {
    Write-Host "[3/3] 手动启动后端: cd backend; $backendRunHint" -ForegroundColor DarkGray
}
