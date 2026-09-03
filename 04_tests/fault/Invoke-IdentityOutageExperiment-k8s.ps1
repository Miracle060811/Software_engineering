param(
    [string]$Namespace = "travelmate",
    [int]$IdentityLocalPort = 18081,
    [int]$TrafficLocalPort = 18082,
    [ValidateRange(0, 600)]
    [int]$OutageHoldSeconds = 0,
    [string]$ResultDirectory = (Join-Path $PSScriptRoot "results")
)

$ErrorActionPreference = "Stop"

if ($PSVersionTable.PSVersion.Major -lt 7) {
    throw "本脚本需要 PowerShell 7 或更高版本（pwsh），因为需要读取 HTTP 503 响应正文。"
}

$startedAt = Get-Date
$timestamp = $startedAt.ToString("yyyyMMdd-HHmmss")
$identityBaseUrl = "http://127.0.0.1:$IdentityLocalPort"
$trafficBaseUrl = "http://127.0.0.1:$TrafficLocalPort"
$resultPath = Join-Path $ResultDirectory "identity-outage-k8s-$timestamp.json"
$logPath = Join-Path $ResultDirectory "identity-outage-k8s-$timestamp.log"
$hpaBackupPath = Join-Path ([IO.Path]::GetTempPath()) "travelmate-identity-hpa-$timestamp.json"

$identityForward = $null
$trafficForward = $null
$identityScaledDown = $false
$identityRestored = $false
$hpaExisted = $false
$hpaSuspended = $false
$hpaRestored = $false
$hpaSpecBeforeJson = $null
$originalIdentityReplicas = $null
$failureMessage = $null
$cleanupErrors = [System.Collections.Generic.List[string]]::new()
$identityLogsBefore = [System.Collections.Generic.List[string]]::new()
$headers = $null
$passengerId = $null
$webSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$username = "fault_identity_$($startedAt.ToString('yyyyMMddHHmmssfff'))"
$password = "TravelMate123!"
$train = $null
$ordersBefore = $null
$ordersAfter = $null
$inventoryBefore = $null
$inventoryAfter = $null
$orderResponse = $null
$orderResult = $null
$trafficHealth = $null
$searchDuringOutage = $null
$otherServiceHealth = [ordered]@{}
$contextName = $null
$identityPodsBefore = @()
$identityDesiredReplicasDuringOutage = $null
$identityReadyEndpointsDuringOutage = $null

[IO.Directory]::CreateDirectory($ResultDirectory) | Out-Null

function Invoke-Kubectl {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = & kubectl @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl $($Arguments -join ' ') 执行失败：$($output -join [Environment]::NewLine)"
    }
    return $output
}

function Test-LocalPortAvailable {
    param([Parameter(Mandatory)][int]$Port)

    $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $Port)
    try {
        $listener.Start()
        return $true
    } catch {
        return $false
    } finally {
        $listener.Stop()
    }
}

function Wait-ForHealth {
    param(
        [Parameter(Mandatory)][string]$Url,
        [int]$Attempts = 45
    )

    for ($index = 0; $index -lt $Attempts; $index++) {
        try {
            $health = Invoke-RestMethod -Uri $Url -TimeoutSec 3 -ErrorAction Stop
            if ($health.status -eq "UP") {
                return $true
            }
        } catch {
            # 服务启动或端口转发建立期间的短暂失败属于预期重试。
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Wait-ForIdentityUnavailable {
    param([int]$Attempts = 60)

    for ($index = 0; $index -lt $Attempts; $index++) {
        $desiredRaw = Invoke-Kubectl @(
            "get", "deployment", "identity-service", "-n", $Namespace,
            "-o", "jsonpath={.spec.replicas}"
        )
        $readyEndpoints = Get-IdentityReadyEndpointCount
        if ([int]($desiredRaw -join "") -eq 0 -and $readyEndpoints -eq 0) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Get-IdentityReadyEndpointCount {
    $raw = & kubectl get endpointslice -n $Namespace `
        -l "kubernetes.io/service-name=identity-service" -o json 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "无法读取 identity-service EndpointSlice。"
    }
    $object = ($raw -join "") | ConvertFrom-Json
    return @(
        $object.items | ForEach-Object { $_.endpoints } |
            Where-Object {
                $null -ne $_ -and $_.conditions.ready -eq $true -and @($_.addresses).Count -gt 0
            }
    ).Count
}

function Start-PortForward {
    param(
        [Parameter(Mandatory)][string]$Service,
        [Parameter(Mandatory)][int]$LocalPort,
        [Parameter(Mandatory)][int]$RemotePort
    )

    return Start-Process -FilePath "kubectl" -ArgumentList @(
        "port-forward", "svc/$Service", "${LocalPort}:${RemotePort}", "-n", $Namespace
    ) -WindowStyle Hidden -PassThru
}

function Stop-PortForward {
    param($Process)

    if ($null -ne $Process -and -not $Process.HasExited) {
        Stop-Process -Id $Process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Get-PodNames {
    param([Parameter(Mandatory)][string]$LabelValue)

    $raw = Invoke-Kubectl @(
        "get", "pods", "-n", $Namespace,
        "-l", "app.kubernetes.io/name=$LabelValue",
        "-o", "jsonpath={.items[*].metadata.name}"
    )
    $joined = $raw -join ""
    if ([string]::IsNullOrWhiteSpace($joined)) {
        return @()
    }
    return @($joined.Split(' ', [StringSplitOptions]::RemoveEmptyEntries))
}

function Get-ServiceHealth {
    param(
        [Parameter(Mandatory)][string]$Service,
        [Parameter(Mandatory)][int]$Port
    )

    $path = "/api/v1/namespaces/$Namespace/services/http:${Service}:${Port}/proxy/actuator/health/liveness"
    $raw = Invoke-Kubectl @("get", "--raw", $path)
    return (($raw -join "") | ConvertFrom-Json).status
}

function Add-PodLogs {
    param(
        [Parameter(Mandatory)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Target,
        [Parameter(Mandatory)][string]$Service,
        [Parameter(Mandatory)][string[]]$Pods,
        [Parameter(Mandatory)][string]$SinceTime
    )

    $Target.Add("=== $Service ===")
    foreach ($pod in $Pods) {
        $Target.Add("--- pod: $pod ---")
        $lines = & kubectl logs -n $Namespace $pod --since-time=$SinceTime 2>&1
        if ($LASTEXITCODE -eq 0) {
            foreach ($line in $lines) {
                $Target.Add(($line -replace '(Using generated security password:)\s+\S+', '$1 [REDACTED]').TrimEnd())
            }
        } else {
            $Target.Add("[日志读取失败] $($lines -join ' ')")
        }
    }
}

try {
    Write-Host "[1/10] 检查 Kubernetes 环境与端口 ..."
    $contextName = (Invoke-Kubectl @("config", "current-context") | Select-Object -First 1).ToString().Trim()
    Invoke-Kubectl @("get", "namespace", $Namespace, "-o", "name") | Out-Null
    Invoke-Kubectl @("get", "deployment", "identity-service", "-n", $Namespace, "-o", "name") | Out-Null
    Invoke-Kubectl @("get", "deployment", "traffic-service", "-n", $Namespace, "-o", "name") | Out-Null
    if (-not (Test-LocalPortAvailable $IdentityLocalPort)) {
        throw "本地端口 $IdentityLocalPort 已被占用，请通过 -IdentityLocalPort 指定其他端口。"
    }
    if (-not (Test-LocalPortAvailable $TrafficLocalPort)) {
        throw "本地端口 $TrafficLocalPort 已被占用，请通过 -TrafficLocalPort 指定其他端口。"
    }

    $replicaOutput = Invoke-Kubectl @(
        "get", "deployment", "identity-service", "-n", $Namespace,
        "-o", "jsonpath={.spec.replicas}"
    )
    $originalIdentityReplicas = [int]($replicaOutput -join "")
    if ($originalIdentityReplicas -lt 1) {
        throw "identity-service 当前副本数小于 1，不能建立健康基线。"
    }

    $hpaRaw = & kubectl get hpa identity-service -n $Namespace -o json 2>$null
    if ($LASTEXITCODE -eq 0) {
        $hpaExisted = $true
        $hpaObject = ($hpaRaw -join "") | ConvertFrom-Json
        $hpaSpecBeforeJson = $hpaObject.spec | ConvertTo-Json -Depth 30 -Compress
        $hpaBackup = [ordered]@{
            apiVersion = $hpaObject.apiVersion
            kind = $hpaObject.kind
            metadata = [ordered]@{
                name = $hpaObject.metadata.name
                namespace = $hpaObject.metadata.namespace
                labels = $hpaObject.metadata.labels
                annotations = $hpaObject.metadata.annotations
            }
            spec = $hpaObject.spec
        }
        [IO.File]::WriteAllText(
            $hpaBackupPath,
            ($hpaBackup | ConvertTo-Json -Depth 30),
            [Text.UTF8Encoding]::new($false)
        )
    }

    Write-Host "[2/10] 建立 identity-service 与 traffic-service 端口转发 ..."
    $identityForward = Start-PortForward "identity-service" $IdentityLocalPort 8081
    $trafficForward = Start-PortForward "traffic-service" $TrafficLocalPort 8082
    if (-not (Wait-ForHealth "$identityBaseUrl/actuator/health/liveness")) {
        throw "identity-service 未就绪或端口转发失败。"
    }
    if (-not (Wait-ForHealth "$trafficBaseUrl/actuator/health/liveness")) {
        throw "traffic-service 未就绪或端口转发失败。"
    }
    $identityRestored = $true

    Write-Host "[3/10] 创建隔离的临时用户与旅客 ..."
    $register = Invoke-RestMethod -Method Post -WebSession $webSession `
        -Uri "$identityBaseUrl/user/register?username=$username&password=$([uri]::EscapeDataString($password))"
    if ($register.code -ne 200) {
        throw "联调用户注册失败：$($register.msg)"
    }
    $login = Invoke-RestMethod -Method Post -WebSession $webSession `
        -Uri "$identityBaseUrl/user/login?username=$username&password=$([uri]::EscapeDataString($password))"
    if ($login.code -ne 200 -or [string]::IsNullOrWhiteSpace($login.data)) {
        throw "联调用户登录失败：$($login.msg)"
    }
    $csrfCookie = $webSession.Cookies.GetCookies($identityBaseUrl)["XSRF-TOKEN"]
    if ($null -eq $csrfCookie) {
        throw "登录后未获得 XSRF-TOKEN。"
    }
    $headers = @{
        Authorization = "Bearer $($login.data)"
        "X-XSRF-TOKEN" = $csrfCookie.Value
    }

    $passport = "F$($startedAt.ToString('yyyyMMddHHmmssfff'))"
    $passengerBody = @{
        name = "故障实验旅客"
        idCard = $passport
        phone = "13500135000"
        type = 0
    } | ConvertTo-Json
    $addPassenger = Invoke-RestMethod -Method Post -Uri "$identityBaseUrl/api/passenger/add" `
        -Headers $headers -WebSession $webSession -ContentType "application/json; charset=utf-8" `
        -Body $passengerBody
    if ($addPassenger.code -ne 200) {
        throw "旅客创建失败：$($addPassenger.msg)"
    }
    $passengers = Invoke-RestMethod -Method Get -Uri "$identityBaseUrl/api/passenger/list" `
        -Headers $headers -WebSession $webSession
    $passengerId = ($passengers.data | Where-Object { $_.idCard -eq $passport } | Select-Object -First 1).id
    if ($null -eq $passengerId) {
        throw "无法读取刚创建的旅客 ID。"
    }

    Write-Host "[4/10] 记录订单、库存、Pod 与服务健康基线 ..."
    $trains = Invoke-RestMethod -Method Get -Uri "$trafficBaseUrl/api/train/search" -TimeoutSec 20
    $train = $trains.data | Where-Object { [int]$_.secondClassSeats -gt 1 } | Select-Object -First 1
    if ($null -eq $train) {
        throw "当前 Kubernetes 数据库中没有可用于实验的火车余票。"
    }
    $ordersBefore = Invoke-RestMethod -Method Get -Uri "$trafficBaseUrl/api/order/list" `
        -Headers $headers -WebSession $webSession
    $inventoryBefore = [int]$train.secondClassSeats
    $identityPodsBefore = Get-PodNames "identity-service"
    Add-PodLogs $identityLogsBefore "identity-service（故障前）" $identityPodsBefore $startedAt.ToUniversalTime().ToString("o")

    Write-Host "[5/10] 暂停 identity-service HPA，避免自动恢复停机副本 ..."
    if ($hpaExisted) {
        Invoke-Kubectl @("delete", "hpa", "identity-service", "-n", $Namespace, "--wait=true") | Out-Null
        $hpaSuspended = $true
    }

    Write-Host "[6/10] 将 identity-service 缩容至 0，注入依赖停机故障 ..."
    Invoke-Kubectl @("scale", "deployment", "identity-service", "--replicas=0", "-n", $Namespace) | Out-Null
    $identityScaledDown = $true
    $identityRestored = $false
    Stop-PortForward $identityForward
    $identityForward = $null
    if (-not (Wait-ForIdentityUnavailable)) {
        throw "identity-service 未能在预期时间内变为 0 副本且 0 个就绪端点。"
    }
    $identityDesiredReplicasDuringOutage = [int]((Invoke-Kubectl @(
        "get", "deployment", "identity-service", "-n", $Namespace,
        "-o", "jsonpath={.spec.replicas}"
    )) -join "")
    $identityReadyEndpointsDuringOutage = Get-IdentityReadyEndpointCount

    Write-Host "[7/10] 发起下单并验证 503、数据一致性与故障隔离 ..."
    $orderBody = @{
        trainId = $train.id
        passengerId = $passengerId
        seatType = "SecondClass"
        ticketCount = 1
        userCouponId = $null
    } | ConvertTo-Json
    $orderResponse = Invoke-WebRequest -Method Post -Uri "$trafficBaseUrl/api/order/train/create" `
        -Headers $headers -WebSession $webSession -ContentType "application/json; charset=utf-8" `
        -Body $orderBody -SkipHttpErrorCheck -TimeoutSec 10
    $orderResult = $orderResponse.Content | ConvertFrom-Json

    $trafficHealth = Invoke-RestMethod -Uri "$trafficBaseUrl/actuator/health/liveness" -TimeoutSec 3
    $searchDuringOutage = Invoke-RestMethod -Method Get -Uri "$trafficBaseUrl/api/train/search" -TimeoutSec 20
    $ordersAfter = Invoke-RestMethod -Method Get -Uri "$trafficBaseUrl/api/order/list" `
        -Headers $headers -WebSession $webSession
    $trainAfter = Invoke-RestMethod -Method Get -Uri "$trafficBaseUrl/api/train/$($train.id)"
    $inventoryAfter = [int]$trainAfter.data.secondClassSeats

    foreach ($service in @(
        @{ Name = "traffic-service"; Port = 8082 },
        @{ Name = "local-service"; Port = 8083 },
        @{ Name = "ai-service"; Port = 8084 },
        @{ Name = "community-service"; Port = 8085 },
        @{ Name = "ops-service"; Port = 8086 }
    )) {
        $otherServiceHealth[$service.Name] = Get-ServiceHealth $service.Name $service.Port
    }

    if ($OutageHoldSeconds -gt 0) {
        Write-Host "      故障验证完成，继续保持 identity-service 为 0 副本 $OutageHoldSeconds 秒，供录屏观察 ..."
        for ($remaining = $OutageHoldSeconds; $remaining -gt 0) {
            Write-Host "      identity-service 故障保持中：剩余 $remaining 秒"
            $sleepSeconds = [Math]::Min(5, $remaining)
            Start-Sleep -Seconds $sleepSeconds
            $remaining -= $sleepSeconds
        }
    }
} catch {
    $failureMessage = $_.Exception.Message
} finally {
    Write-Host "[8/10] 恢复 identity-service 原副本数 ..."
    if ($identityScaledDown -and $null -ne $originalIdentityReplicas) {
        try {
            Invoke-Kubectl @(
                "scale", "deployment", "identity-service",
                "--replicas=$originalIdentityReplicas", "-n", $Namespace
            ) | Out-Null
            Invoke-Kubectl @(
                "rollout", "status", "deployment/identity-service",
                "-n", $Namespace, "--timeout=180s"
            ) | Out-Null
            $identityForward = Start-PortForward "identity-service" $IdentityLocalPort 8081
            $identityRestored = Wait-ForHealth "$identityBaseUrl/actuator/health/liveness" 60
            if (-not $identityRestored) {
                $cleanupErrors.Add("identity-service 已恢复副本，但健康检查未在时限内变为 UP。")
            }
        } catch {
            $cleanupErrors.Add("恢复 identity-service 失败：$($_.Exception.Message)")
        }
    }

    Write-Host "[9/10] 恢复 identity-service HPA 原配置 ..."
    if ($hpaSuspended) {
        try {
            Invoke-Kubectl @("apply", "-f", $hpaBackupPath) | Out-Null
            $hpaRestored = $true
        } catch {
            $cleanupErrors.Add("恢复 identity-service HPA 失败：$($_.Exception.Message)")
        }
    }
    if ($hpaExisted) {
        $hpaAfterRaw = & kubectl get hpa identity-service -n $Namespace -o json 2>$null
        $hpaRestored = $LASTEXITCODE -eq 0
        if ($hpaRestored) {
            $hpaSpecAfterJson = (($hpaAfterRaw -join "") | ConvertFrom-Json).spec |
                ConvertTo-Json -Depth 30 -Compress
            $hpaRestored = $hpaSpecAfterJson -eq $hpaSpecBeforeJson
        }
        if (-not $hpaRestored) {
            $cleanupErrors.Add("identity-service HPA 未按实验前的实际 spec 完整恢复。备份保留在 $hpaBackupPath")
        }
    } else {
        $hpaRestored = $true
    }

    Write-Host "[10/10] 清理临时用户、旅客与端口转发 ..."
    if (($identityRestored -or -not $identityScaledDown) -and $null -ne $headers) {
        try {
            if ($null -ne $passengerId) {
                $deletePassengerResponse = Invoke-WebRequest -Method Delete -Uri "$identityBaseUrl/api/passenger/$passengerId" `
                    -Headers $headers -WebSession $webSession -SkipHttpErrorCheck
                if ([int]$deletePassengerResponse.StatusCode -lt 200 -or [int]$deletePassengerResponse.StatusCode -ge 300) {
                    $cleanupErrors.Add("清理临时旅客失败：HTTP $([int]$deletePassengerResponse.StatusCode)")
                }
            }
            $deleteAccountResponse = Invoke-WebRequest -Method Delete `
                -Uri "$identityBaseUrl/user/account?password=$([uri]::EscapeDataString($password))" `
                -Headers $headers -WebSession $webSession -SkipHttpErrorCheck
            if ([int]$deleteAccountResponse.StatusCode -lt 200 -or [int]$deleteAccountResponse.StatusCode -ge 300) {
                $cleanupErrors.Add("清理临时用户失败：HTTP $([int]$deleteAccountResponse.StatusCode)")
            }
        } catch {
            $cleanupErrors.Add("清理临时业务数据失败：$($_.Exception.Message)")
        }
    }

    Stop-PortForward $identityForward
    Stop-PortForward $trafficForward

    if ((-not $hpaSuspended -or $hpaRestored) -and [IO.File]::Exists($hpaBackupPath)) {
        [IO.File]::Delete($hpaBackupPath)
    }
}

$sinceUtc = $startedAt.ToUniversalTime().ToString("o")
$logLines = [System.Collections.Generic.List[string]]::new()
foreach ($line in $identityLogsBefore) {
    $logLines.Add($line)
}
try {
    Add-PodLogs $logLines "traffic-service" (Get-PodNames "traffic-service") $sinceUtc
    if ($identityRestored) {
        Add-PodLogs $logLines "identity-service（恢复后）" (Get-PodNames "identity-service") $sinceUtc
    }
} catch {
    $logLines.Add("[实验后日志采集失败] $($_.Exception.Message)")
}
[IO.File]::WriteAllLines($logPath, [string[]]$logLines, [Text.UTF8Encoding]::new($false))

$orderHttpStatusValue = if ($null -ne $orderResponse) { [int]$orderResponse.StatusCode } else { $null }
$orderBusinessCodeValue = if ($null -ne $orderResult) { [int]$orderResult.code } else { $null }
$orderMessageValue = if ($null -ne $orderResult) { $orderResult.msg } else { $null }
$trafficSearchCodeValue = if ($null -ne $searchDuringOutage) { [int]$searchDuringOutage.code } else { $null }

$identityStatusDescription = if (
    $null -ne $identityDesiredReplicasDuringOutage -and
    $null -ne $identityReadyEndpointsDuringOutage
) {
    "identity-service 故障期间实际为 $identityDesiredReplicasDuringOutage 副本、$identityReadyEndpointsDuringOutage 个就绪端点"
} else {
    "未完整取得 identity-service 故障期间的副本与就绪端点状态"
}
$systemResponseDescription = if ($null -ne $orderHttpStatusValue) {
    "下单接口实际返回 HTTP $orderHttpStatusValue、业务码 $orderBusinessCodeValue：$orderMessageValue"
} else {
    "未取得下单接口的故障响应"
}
$otherServiceStatesText = if ($otherServiceHealth.Count -gt 0) {
    @($otherServiceHealth.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join "，"
} else {
    "未取得其他服务状态"
}
$otherServicesDescription = "交通查询实际返回 $trafficSearchCodeValue；$otherServiceStatesText"

$evidence = [ordered]@{
    experiment = "TRAFFIC_TO_IDENTITY_OUTAGE_K8S"
    executedAt = $startedAt.ToString("o")
    kubernetesContext = $contextName
    namespace = $Namespace
    outageHoldSeconds = $OutageHoldSeconds
    identityConnectTimeoutMs = 1000
    identityReadTimeoutMs = 2000
    originalIdentityReplicas = $originalIdentityReplicas
    identityPodsBefore = @($identityPodsBefore).Count
    identityDesiredReplicasDuringOutage = $identityDesiredReplicasDuringOutage
    identityReadyEndpointsDuringOutage = $identityReadyEndpointsDuringOutage
    identityStatusDescription = $identityStatusDescription
    identityHpaExisted = $hpaExisted
    identityHpaRestored = $hpaRestored
    orderHttpStatus = $orderHttpStatusValue
    orderBusinessCode = $orderBusinessCodeValue
    orderMessage = $orderMessageValue
    systemResponseDescription = $systemResponseDescription
    trafficHealthDuringOutage = if ($null -ne $trafficHealth) { $trafficHealth.status } else { $null }
    trafficSearchDuringOutage = $trafficSearchCodeValue
    otherServiceHealthDuringOutage = $otherServiceHealth
    otherServicesDescription = $otherServicesDescription
    ordersBefore = if ($null -ne $ordersBefore) { @($ordersBefore.data).Count } else { $null }
    ordersAfter = if ($null -ne $ordersAfter) { @($ordersAfter.data).Count } else { $null }
    inventoryBefore = $inventoryBefore
    inventoryAfter = $inventoryAfter
    identityRestored = $identityRestored
    cleanupErrors = @($cleanupErrors)
    failure = $failureMessage
}

$allOtherServicesUp = $otherServiceHealth.Count -eq 5 `
    -and @($otherServiceHealth.Values | Where-Object { $_ -ne "UP" }).Count -eq 0
$evidence.passed = $null -eq $failureMessage `
    -and $evidence.identityDesiredReplicasDuringOutage -eq 0 `
    -and $evidence.identityReadyEndpointsDuringOutage -eq 0 `
    -and $evidence.orderHttpStatus -eq 503 `
    -and $evidence.orderBusinessCode -eq 503 `
    -and $evidence.orderMessage -eq "身份服务暂不可用，请稍后重试" `
    -and $evidence.trafficHealthDuringOutage -eq "UP" `
    -and $evidence.trafficSearchDuringOutage -eq 200 `
    -and $allOtherServicesUp `
    -and $evidence.ordersBefore -eq $evidence.ordersAfter `
    -and $evidence.inventoryBefore -eq $evidence.inventoryAfter `
    -and $evidence.identityRestored `
    -and $evidence.identityHpaRestored `
    -and $evidence.cleanupErrors.Count -eq 0

[IO.File]::WriteAllText(
    $resultPath,
    ($evidence | ConvertTo-Json -Depth 8),
    [Text.UTF8Encoding]::new($false)
)
$evidence | ConvertTo-Json -Depth 8

if (-not $evidence.passed) {
    Write-Error "Kubernetes 故障处理实验未通过。结果：$resultPath"
    exit 1
}
