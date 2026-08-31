param(
    [string]$IdentityBaseUrl = "http://127.0.0.1:8081",
    [string]$TrafficBaseUrl = "http://127.0.0.1:8082",
    [string]$ResultDirectory = (Join-Path $PSScriptRoot "results")
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$composeFile = Join-Path $repoRoot "microservices\compose.yml"
$composeEnv = Join-Path $repoRoot "microservices\.env"
$startedAt = Get-Date
$identityStopped = $false
$identityRestored = $false
$passengerId = $null
$webSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$username = "fault_identity_$($startedAt.ToString('yyyyMMddHHmmssfff'))"
$password = "TravelMate123!"
$headers = $null

[IO.Directory]::CreateDirectory($ResultDirectory) | Out-Null
$resultPath = Join-Path $ResultDirectory "identity-outage-2026-08-31.json"
$logPath = Join-Path $ResultDirectory "identity-outage-2026-08-31.log"
$composeArgs = @("compose", "--env-file", $composeEnv, "-f", $composeFile)

function Wait-ForHealth([string]$Url, [bool]$ExpectedUp, [int]$Attempts = 15) {
    for ($index = 0; $index -lt $Attempts; $index++) {
        $isUp = $false
        try {
            $health = Invoke-RestMethod -Uri $Url -TimeoutSec 2
            $isUp = $health.status -eq "UP"
        } catch {
            $isUp = $false
        }
        if ($isUp -eq $ExpectedUp) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

try {
    if (-not (Wait-ForHealth "$IdentityBaseUrl/actuator/health/liveness" $true)) {
        throw "identity-service 未就绪"
    }
    if (-not (Wait-ForHealth "$TrafficBaseUrl/actuator/health/liveness" $true)) {
        throw "traffic-service 未就绪"
    }

    $register = Invoke-RestMethod -Method Post -WebSession $webSession `
        -Uri "$IdentityBaseUrl/user/register?username=$username&password=$([uri]::EscapeDataString($password))"
    if ($register.code -ne 200) {
        throw "联调用户注册失败：$($register.msg)"
    }
    $login = Invoke-RestMethod -Method Post -WebSession $webSession `
        -Uri "$IdentityBaseUrl/user/login?username=$username&password=$([uri]::EscapeDataString($password))"
    $csrfToken = $webSession.Cookies.GetCookies($IdentityBaseUrl)["XSRF-TOKEN"].Value
    $headers = @{
        Authorization = "Bearer $($login.data)"
        "X-XSRF-TOKEN" = $csrfToken
    }

    $passport = "F$($startedAt.ToString('yyyyMMddHHmmssfff'))"
    $passengerBody = @{
        name = "故障实验旅客"
        idCard = $passport
        phone = "13500135000"
        type = 0
    } | ConvertTo-Json
    $addPassenger = Invoke-RestMethod -Method Post -Uri "$IdentityBaseUrl/api/passenger/add" `
        -Headers $headers -WebSession $webSession -ContentType "application/json; charset=utf-8" `
        -Body $passengerBody
    if ($addPassenger.code -ne 200) {
        throw "旅客创建失败：$($addPassenger.msg)"
    }
    $passengers = Invoke-RestMethod -Method Get -Uri "$IdentityBaseUrl/api/passenger/list" `
        -Headers $headers -WebSession $webSession
    $passengerId = ($passengers.data | Where-Object { $_.idCard -eq $passport } | Select-Object -First 1).id

    $trains = Invoke-RestMethod -Method Get `
        -Uri "$TrafficBaseUrl/api/train/search?depStation=$([uri]::EscapeDataString('北京南'))&arrStation=$([uri]::EscapeDataString('上海虹桥'))"
    $train = $trains.data | Where-Object { [int]$_.secondClassSeats -gt 1 } | Select-Object -First 1
    if (-not $train) {
        throw "没有可用于实验的火车余票"
    }
    $ordersBefore = Invoke-RestMethod -Method Get -Uri "$TrafficBaseUrl/api/order/list" `
        -Headers $headers -WebSession $webSession
    $inventoryBefore = [int]$train.secondClassSeats

    & docker @composeArgs stop identity-service | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "停止 identity-service 失败"
    }
    $identityStopped = $true
    if (-not (Wait-ForHealth "$IdentityBaseUrl/actuator/health/liveness" $false)) {
        throw "identity-service 未按预期停止"
    }

    $orderBody = @{
        trainId = $train.id
        passengerId = $passengerId
        seatType = "SecondClass"
        ticketCount = 1
        userCouponId = $null
    } | ConvertTo-Json
    $orderResponse = Invoke-WebRequest -Method Post -Uri "$TrafficBaseUrl/api/order/train/create" `
        -Headers $headers -WebSession $webSession -ContentType "application/json; charset=utf-8" `
        -Body $orderBody -SkipHttpErrorCheck
    $orderResult = $orderResponse.Content | ConvertFrom-Json

    $trafficHealth = Invoke-RestMethod -Uri "$TrafficBaseUrl/actuator/health/liveness" -TimeoutSec 3
    $searchDuringOutage = Invoke-RestMethod -Method Get `
        -Uri "$TrafficBaseUrl/api/train/search?depStation=$([uri]::EscapeDataString('北京南'))&arrStation=$([uri]::EscapeDataString('上海虹桥'))"
    $ordersAfter = Invoke-RestMethod -Method Get -Uri "$TrafficBaseUrl/api/order/list" `
        -Headers $headers -WebSession $webSession
    $trainAfter = Invoke-RestMethod -Method Get -Uri "$TrafficBaseUrl/api/train/$($train.id)"
} finally {
    if ($identityStopped) {
        & docker @composeArgs up -d identity-service | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $identityRestored = Wait-ForHealth "$IdentityBaseUrl/actuator/health/liveness" $true 20
        }
    }

    if ($identityRestored -and $headers) {
        if ($passengerId) {
            Invoke-WebRequest -Method Delete -Uri "$IdentityBaseUrl/api/passenger/$passengerId" `
                -Headers $headers -WebSession $webSession -SkipHttpErrorCheck | Out-Null
        }
        Invoke-WebRequest -Method Delete `
            -Uri "$IdentityBaseUrl/user/account?password=$([uri]::EscapeDataString($password))" `
            -Headers $headers -WebSession $webSession -SkipHttpErrorCheck | Out-Null
    }
}

$evidence = [ordered]@{
    experiment = "TRAFFIC_TO_IDENTITY_OUTAGE"
    executedAt = $startedAt.ToString("o")
    identityConnectTimeoutMs = 1000
    identityReadTimeoutMs = 2000
    orderHttpStatus = [int]$orderResponse.StatusCode
    orderBusinessCode = [int]$orderResult.code
    orderMessage = $orderResult.msg
    trafficHealthDuringOutage = $trafficHealth.status
    trafficSearchDuringOutage = [int]$searchDuringOutage.code
    ordersBefore = @($ordersBefore.data).Count
    ordersAfter = @($ordersAfter.data).Count
    inventoryBefore = $inventoryBefore
    inventoryAfter = [int]$trainAfter.data.secondClassSeats
    identityRestored = $identityRestored
}
$evidence.passed = $evidence.orderHttpStatus -eq 503 `
    -and $evidence.orderBusinessCode -eq 503 `
    -and $evidence.orderMessage -eq "身份服务暂不可用，请稍后重试" `
    -and $evidence.trafficHealthDuringOutage -eq "UP" `
    -and $evidence.trafficSearchDuringOutage -eq 200 `
    -and $evidence.ordersBefore -eq $evidence.ordersAfter `
    -and $evidence.inventoryBefore -eq $evidence.inventoryAfter `
    -and $evidence.identityRestored

[IO.File]::WriteAllText($resultPath, ($evidence | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
$logs = & docker @composeArgs logs --since $startedAt.ToUniversalTime().ToString("o") traffic-service identity-service 2>&1
$sanitizedLogs = $logs | ForEach-Object {
    ($_ -replace '(Using generated security password:)\s+\S+', '$1 [REDACTED]') -replace '\s+$', ''
}
[IO.File]::WriteAllLines($logPath, [string[]]$sanitizedLogs, [Text.UTF8Encoding]::new($false))

$evidence | ConvertTo-Json -Depth 4
if (-not $evidence.passed) {
    exit 1
}
