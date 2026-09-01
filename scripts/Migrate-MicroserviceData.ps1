[CmdletBinding()]
param(
    [string]$SourceHost = '127.0.0.1',
    [int]$SourcePort = 3306,
    [string]$SourceDatabase = 'travelmate',
    [string]$SourceUser = 'root',
    [string]$SourcePassword = $env:DB_PASSWORD,

    [string]$TargetHost = '127.0.0.1',
    [int]$IdentityPort = 3307,
    [int]$TrafficPort = 3308,
    [int]$LocalPort = 3309,
    [int]$AiPort = 3310,
    [int]$CommunityPort = 3311,
    [int]$OpsPort = 3312,
    [string]$IdentityPassword = $env:IDENTITY_DB_PASSWORD,
    [string]$TrafficPassword = $env:TRAFFIC_DB_PASSWORD,
    [string]$LocalPassword = $env:LOCAL_DB_PASSWORD,
    [string]$AiPassword = $env:AI_DB_PASSWORD,
    [string]$CommunityPassword = $env:COMMUNITY_DB_PASSWORD,
    [string]$OpsPassword = $env:OPS_DB_PASSWORD,

    [switch]$Execute,
    [string]$ConfirmationToken
)

$ErrorActionPreference = 'Stop'

$services = [ordered]@{
    identity = [ordered]@{
        Database = 'travelmate_identity'
        User = 'travelmate_identity_app'
        Port = $IdentityPort
        Password = $IdentityPassword
        Tables = @('tm_user', 'tm_passenger', 'tm_follow')
    }
    traffic = [ordered]@{
        Database = 'travelmate_traffic'
        User = 'travelmate_traffic_app'
        Port = $TrafficPort
        Password = $TrafficPassword
        Tables = @('tm_flight', 'tm_train', 'tm_traffic_order', 'tm_train_waitlist', 'tm_price_history')
    }
    local = [ordered]@{
        Database = 'travelmate_local'
        User = 'travelmate_local_app'
        Port = $LocalPort
        Password = $LocalPassword
        Tables = @(
            'tm_hotel', 'tm_hotel_room', 'tm_hotel_order',
            'tm_attraction', 'tm_attraction_order', 'tm_destination',
            'tm_review', 'tm_reply', 'tm_review_report',
            'tm_tour_product', 'tm_tour_product_step',
            'tm_coupon', 'tm_user_coupon'
        )
    }
    ai = [ordered]@{
        Database = 'travelmate_ai'
        User = 'travelmate_ai_app'
        Port = $AiPort
        Password = $AiPassword
        Tables = @('tm_ai_plan', 'tm_ai_chat', 'tm_notification', 'tm_private_message', 'tm_private_contact')
    }
    community = [ordered]@{
        Database = 'travelmate_community'
        User = 'travelmate_community_app'
        Port = $CommunityPort
        Password = $CommunityPassword
        Tables = @('tm_post', 'tm_comment', 'tm_like')
    }
    ops = [ordered]@{
        Database = 'travelmate_ops'
        User = 'travelmate_ops_app'
        Port = $OpsPort
        Password = $OpsPassword
        Tables = @('sys_log', 'sys_sensitive_word')
    }
}

Write-Output 'TravelMate 单体数据迁移计划'
Write-Output "源库: ${SourceHost}:${SourcePort}/$SourceDatabase"
foreach ($name in $services.Keys) {
    $service = $services[$name]
    Write-Output ("{0}: {1}:{2}/{3}, {4} tables" -f $name, $TargetHost, $service.Port, $service.Database, $service.Tables.Count)
    Write-Output ("  " + ($service.Tables -join ', '))
}

if (-not $Execute) {
    Write-Output 'DRY-RUN：未连接数据库，未写入任何数据。'
    Write-Output '实际执行前必须先用 microservices/sql/*-schema.sql 初始化空目标库。'
    Write-Output '执行时追加：-Execute -ConfirmationToken MIGRATE_TO_EMPTY_TARGETS'
    return
}

if ($ConfirmationToken -ne 'MIGRATE_TO_EMPTY_TARGETS') {
    throw '执行迁移必须提供 -ConfirmationToken MIGRATE_TO_EMPTY_TARGETS。'
}

$mysql = (Get-Command mysql -ErrorAction Stop).Source
$mysqldump = (Get-Command mysqldump -ErrorAction Stop).Source
if ([string]::IsNullOrWhiteSpace($SourcePassword)) {
    throw '缺少源库密码：请设置 DB_PASSWORD 或传入 -SourcePassword。'
}
foreach ($name in $services.Keys) {
    if ([string]::IsNullOrWhiteSpace($services[$name].Password)) {
        throw "缺少 $name 目标库密码。"
    }
}

function Invoke-MySqlQuery {
    param(
        [string]$Executable,
        [string]$HostName,
        [int]$Port,
        [string]$User,
        [string]$Password,
        [string]$Database,
        [string]$Query
    )
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $Password
        $result = & $Executable --host=$HostName --port=$Port --user=$User --default-character-set=utf8mb4 --batch --skip-column-names --database=$Database --execute=$Query 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL 命令失败：$($result -join [Environment]::NewLine)"
        }
        return @($result)
    } finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Get-TableCount {
    param([hashtable]$Connection, [string]$Table)
    $rows = Invoke-MySqlQuery -Executable $mysql -HostName $Connection.Host -Port $Connection.Port `
        -User $Connection.User -Password $Connection.Password -Database $Connection.Database `
        -Query "SELECT COUNT(*) FROM ``$Table``;"
    return [long]$rows[-1]
}

$source = @{
    Host = $SourceHost; Port = $SourcePort; User = $SourceUser;
    Password = $SourcePassword; Database = $SourceDatabase
}

# 目标库必须预先建表且为空，避免与正在使用的数据发生覆盖或重复写入。
foreach ($name in $services.Keys) {
    $service = $services[$name]
    $target = @{
        Host = $TargetHost; Port = $service.Port; User = $service.User;
        Password = $service.Password; Database = $service.Database
    }
    foreach ($table in $service.Tables) {
        [void](Get-TableCount -Connection $source -Table $table)
        $targetCount = Get-TableCount -Connection $target -Table $table
        if ($targetCount -ne 0) {
            throw "目标表 $($service.Database).$table 非空（$targetCount 行），迁移已停止。"
        }
    }
}

$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$migrationTemp = Join-Path $tempRoot ("travelmate-migration-" + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $migrationTemp | Out-Null

try {
    foreach ($name in $services.Keys) {
        $service = $services[$name]
        $dumpFile = Join-Path $migrationTemp "$name-data.sql"
        $previousPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $SourcePassword
            $dumpArgs = @(
                "--host=$SourceHost", "--port=$SourcePort", "--user=$SourceUser",
                '--default-character-set=utf8mb4', '--single-transaction', '--quick',
                '--no-create-info', '--skip-triggers', '--set-gtid-purged=OFF',
                "--result-file=$dumpFile", $SourceDatabase
            ) + $service.Tables
            & $mysqldump @dumpArgs
            if ($LASTEXITCODE -ne 0) {
                throw "导出 $name 数据失败。"
            }
        } finally {
            $env:MYSQL_PWD = $previousPassword
        }

        $previousPassword = $env:MYSQL_PWD
        try {
            $env:MYSQL_PWD = $service.Password
            $mysqlArgs = @(
                "--host=$TargetHost", "--port=$($service.Port)", "--user=$($service.User)",
                '--default-character-set=utf8mb4', "--database=$($service.Database)"
            )
            $process = Start-Process -FilePath $mysql -ArgumentList $mysqlArgs -NoNewWindow -Wait -PassThru `
                -RedirectStandardInput $dumpFile
            if ($process.ExitCode -ne 0) {
                throw "导入 $name 数据失败。"
            }
        } finally {
            $env:MYSQL_PWD = $previousPassword
        }

        $target = @{
            Host = $TargetHost; Port = $service.Port; User = $service.User;
            Password = $service.Password; Database = $service.Database
        }
        foreach ($table in $service.Tables) {
            $sourceCount = Get-TableCount -Connection $source -Table $table
            $targetCount = Get-TableCount -Connection $target -Table $table
            if ($sourceCount -ne $targetCount) {
                throw "行数校验失败：$table 源库=$sourceCount，目标库=$targetCount。"
            }
            Write-Output "verified $name.$table rows=$targetCount"
        }
    }
    Write-Output '迁移完成：所有目标表行数与源库一致。源库未被修改。'
} finally {
    $resolvedTemp = [IO.Path]::GetFullPath($migrationTemp)
    if ($resolvedTemp.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -and
        (Split-Path $resolvedTemp -Leaf).StartsWith('travelmate-migration-', [StringComparison]::Ordinal)) {
        Remove-Item -LiteralPath $resolvedTemp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
