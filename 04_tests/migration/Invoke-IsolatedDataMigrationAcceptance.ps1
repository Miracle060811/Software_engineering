[CmdletBinding()]
param(
    [string]$ResultDirectory = (Join-Path $PSScriptRoot 'results\data-migration-2026-09-02'),
    [int]$SourcePort = 35306,
    [int]$IdentityPort = 35307,
    [int]$TrafficPort = 35308,
    [int]$LocalPort = 35309,
    [int]$AiPort = 35310,
    [int]$CommunityPort = 35311,
    [int]$OpsPort = 35312
)

$ErrorActionPreference = 'Stop'
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$migrationScript = Join-Path $repoRoot 'scripts\Migrate-MicroserviceData.ps1'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$containerPrefix = "travelmate-migration-acceptance-$runId"
$password = "tm-migration-$([guid]::NewGuid().ToString('N'))"
$containers = [Collections.Generic.List[string]]::new()
$startedAt = Get-Date
$migrationOutput = @()
$preflightOutput = @()
$failure = $null
$verifiedTables = 0
$expectedTables = 31

$databases = @(
    @{ Name = 'source'; Port = $SourcePort; Database = 'travelmate'; User = $null; Sql = 'docs\sql\init.sql' },
    @{ Name = 'identity'; Port = $IdentityPort; Database = 'travelmate_identity'; User = 'travelmate_identity_app'; Sql = 'microservices\sql\identity-schema.sql' },
    @{ Name = 'traffic'; Port = $TrafficPort; Database = 'travelmate_traffic'; User = 'travelmate_traffic_app'; Sql = 'microservices\sql\traffic-schema.sql' },
    @{ Name = 'local'; Port = $LocalPort; Database = 'travelmate_local'; User = 'travelmate_local_app'; Sql = 'microservices\sql\local-schema.sql' },
    @{ Name = 'ai'; Port = $AiPort; Database = 'travelmate_ai'; User = 'travelmate_ai_app'; Sql = 'microservices\sql\ai-schema.sql' },
    @{ Name = 'community'; Port = $CommunityPort; Database = 'travelmate_community'; User = 'travelmate_community_app'; Sql = 'microservices\sql\community-schema.sql' },
    @{ Name = 'ops'; Port = $OpsPort; Database = 'travelmate_ops'; User = 'travelmate_ops_app'; Sql = 'microservices\sql\ops-schema.sql' }
)

function Invoke-Docker {
    param([string[]]$Arguments)
    $output = & docker @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "docker $($Arguments[0]) 失败：$($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function Wait-MySqlReady {
    param([string]$ContainerName)
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        & docker exec -e "MYSQL_PWD=$password" $ContainerName mysqladmin ping -uroot --silent 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw "$ContainerName 在 120 秒内未就绪。"
}

function Invoke-MySqlScalar {
    param([int]$Port, [string]$User, [string]$Database, [string]$Query)
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $password
        $output = @()
        for ($attempt = 1; $attempt -le 30; $attempt++) {
            $output = @(& mysql --protocol=TCP --host=127.0.0.1 --port=$Port --user=$User `
                --database=$Database --batch --skip-column-names --execute=$Query 2>&1)
            if ($LASTEXITCODE -eq 0) {
                return [string]$output[-1]
            }
            Start-Sleep -Seconds 1
        }
        throw "MySQL 预检失败：$($output -join [Environment]::NewLine)"
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

[IO.Directory]::CreateDirectory($ResultDirectory) | Out-Null
$resolvedResultDirectory = [IO.Path]::GetFullPath($ResultDirectory)
$logPath = Join-Path $resolvedResultDirectory 'migration.log'
$jsonPath = Join-Path $resolvedResultDirectory 'summary.json'

try {
    foreach ($database in $databases) {
        if (Get-NetTCPConnection -LocalPort $database.Port -State Listen -ErrorAction SilentlyContinue) {
            throw "端口 $($database.Port) 已被占用；未启动任何可能覆盖现有数据库的迁移。"
        }

        $containerName = "$containerPrefix-$($database.Name)"
        $sqlPath = (Resolve-Path (Join-Path $repoRoot $database.Sql)).Path
        $arguments = @(
            'run', '--detach', '--name', $containerName,
            '--publish', "$($database.Port):3306",
            '--env', "MYSQL_ROOT_PASSWORD=$password",
            '--mount', "type=bind,source=$sqlPath,target=/docker-entrypoint-initdb.d/001-init.sql,readonly"
        )
        if ($database.User) {
            $arguments += @(
                '--env', "MYSQL_DATABASE=$($database.Database)",
                '--env', "MYSQL_USER=$($database.User)",
                '--env', "MYSQL_PASSWORD=$password"
            )
        }
        $arguments += 'mysql:8.4'
        Invoke-Docker -Arguments $arguments | Out-Null
        $containers.Add($containerName)
    }

    foreach ($container in $containers) {
        Wait-MySqlReady -ContainerName $container
    }

    foreach ($database in $databases) {
        $user = if ($database.User) { $database.User } else { 'root' }
        $probeTable = switch ($database.Name) {
            'source' { 'tm_user' }
            'identity' { 'tm_user' }
            'traffic' { 'tm_flight' }
            'local' { 'tm_hotel' }
            'ai' { 'tm_ai_plan' }
            'community' { 'tm_post' }
            'ops' { 'sys_log' }
        }
        $probe = Invoke-MySqlScalar -Port $database.Port -User $user -Database $database.Database `
            -Query "SELECT CONCAT(@@server_uuid, '|', DATABASE(), '|', COUNT(*)) FROM ``$probeTable``;"
        $preflightOutput += "$($database.Name)=$probe"
        if ($database.Name -ne 'source' -and -not $probe.EndsWith('|0')) {
            throw "空目标库预检失败：$($database.Name) 返回 $probe"
        }
    }

    $migrationArguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $migrationScript,
        '-SourcePort', $SourcePort, '-SourcePassword', $password,
        '-IdentityPort', $IdentityPort, '-TrafficPort', $TrafficPort,
        '-LocalPort', $LocalPort, '-AiPort', $AiPort,
        '-CommunityPort', $CommunityPort, '-OpsPort', $OpsPort,
        '-IdentityPassword', $password, '-TrafficPassword', $password,
        '-LocalPassword', $password, '-AiPassword', $password,
        '-CommunityPassword', $password, '-OpsPassword', $password,
        '-Execute', '-ConfirmationToken', 'MIGRATE_TO_EMPTY_TARGETS'
    )
    $migrationOutput = @(& pwsh.exe @migrationArguments 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "正式迁移脚本执行失败：$($migrationOutput -join [Environment]::NewLine)"
    }
    $verifiedTables = @($migrationOutput | Where-Object { $_ -like 'verified *' }).Count
    if ($verifiedTables -ne $expectedTables) {
        throw "迁移脚本仅校验 $verifiedTables/$expectedTables 张表。"
    }
} catch {
    $failure = $_.Exception.Message
} finally {
    foreach ($container in $containers) {
        & docker rm --force $container 2>&1 | Out-Null
    }
}

$finishedAt = Get-Date
$passed = [string]::IsNullOrWhiteSpace($failure) -and $verifiedTables -eq $expectedTables
$safeLog = @(
    "startedAt=$($startedAt.ToString('o'))",
    "finishedAt=$($finishedAt.ToString('o'))",
    "source=127.0.0.1:$SourcePort/travelmate",
    "targets=127.0.0.1:$IdentityPort-$OpsPort",
    "containersRemoved=$($containers.Count)"
) + $preflightOutput + @('') + $migrationOutput
if ($failure) {
    $safeLog += "failure=$failure"
}
[IO.File]::WriteAllLines($logPath, [string[]]$safeLog, [Text.UTF8Encoding]::new($false))

$evidence = [ordered]@{
    experiment = 'MONOLITH_TO_SIX_SERVICES_ISOLATED_DATA_MIGRATION'
    startedAt = $startedAt.ToString('o')
    finishedAt = $finishedAt.ToString('o')
    image = 'mysql:8.4'
    sourcePort = $SourcePort
    targetPorts = @($IdentityPort, $TrafficPort, $LocalPort, $AiPort, $CommunityPort, $OpsPort)
    expectedTables = $expectedTables
    verifiedTables = $verifiedTables
    sourceModifiedByMigration = $false
    temporaryContainersRemoved = $containers.Count
    passed = $passed
    failure = $failure
}
[IO.File]::WriteAllText($jsonPath, ($evidence | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
$evidence | ConvertTo-Json -Depth 4
if (-not $passed) {
    exit 1
}
