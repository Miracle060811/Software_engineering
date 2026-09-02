[CmdletBinding()]
param(
    [string]$KubeContext = "docker-desktop",
    [string]$Namespace = "travelmate-microservices",
    [int]$LocalPort = 18082,
    [int]$SampleIntervalSeconds = 5,
    [int]$ScaleUpObservationSeconds = 120,
    [int]$ScaleDownTimeoutSeconds = 600
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$resultDirectory = Join-Path $repoRoot "04_tests\stress\results"
$stressScript = Join-Path $repoRoot "04_tests\stress\flight-search.js"
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$samplePath = Join-Path $resultDirectory "hpa-$timestamp-samples.log"
$summaryPath = Join-Path $resultDirectory "hpa-$timestamp-k6.json"
$resultPath = Join-Path $resultDirectory "hpa-$timestamp-result.json"
$forwardOut = Join-Path $resultDirectory "hpa-$timestamp-port-forward.out.log"
$forwardErr = Join-Path $resultDirectory "hpa-$timestamp-port-forward.err.log"
$k6Out = Join-Path $resultDirectory "hpa-$timestamp-k6.out.log"
$k6Err = Join-Path $resultDirectory "hpa-$timestamp-k6.err.log"
$portForward = $null
$k6Job = $null

[IO.Directory]::CreateDirectory($resultDirectory) | Out-Null

function Get-ReplicaCount {
    $value = (& kubectl --context $KubeContext get deployment traffic-service `
        --namespace $Namespace -o "jsonpath={.status.readyReplicas}").Trim()
    if ($LASTEXITCODE -ne 0 -or -not $value) { return 0 }
    return [int]$value
}

function Add-Sample([string]$Phase) {
    $recordedAt = Get-Date -Format "o"
    "[$recordedAt] phase=$Phase" | Add-Content -LiteralPath $samplePath -Encoding utf8
    & kubectl --context $KubeContext get hpa traffic-service --namespace $Namespace -o wide 2>&1 |
        Add-Content -LiteralPath $samplePath -Encoding utf8
    & kubectl --context $KubeContext get pods --namespace $Namespace `
        -l app.kubernetes.io/name=traffic-service -o wide 2>&1 |
        Add-Content -LiteralPath $samplePath -Encoding utf8
    & kubectl --context $KubeContext top pods --namespace $Namespace `
        -l app.kubernetes.io/name=traffic-service 2>&1 |
        Add-Content -LiteralPath $samplePath -Encoding utf8
}

$k6Command = Get-Command k6 -ErrorAction SilentlyContinue
if (-not $k6Command) {
    throw "未安装 k6；请先执行 winget install GrafanaLabs.k6"
}
$k6Executable = $k6Command.Source
$contexts = & kubectl config get-contexts $KubeContext -o name 2>$null
if ($LASTEXITCODE -ne 0 -or -not $contexts) {
    throw "Kubernetes context '$KubeContext' 不存在"
}
& kubectl --context $KubeContext get --raw /apis/metrics.k8s.io/v1beta1/nodes | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Metrics Server 不可用，HPA 无法读取 CPU 指标"
}
& kubectl --context $KubeContext get hpa traffic-service --namespace $Namespace | Out-Null
if ($LASTEXITCODE -ne 0) { throw "traffic-service HPA 不存在" }

$initialReplicas = Get-ReplicaCount
$maximumReplicas = $initialReplicas
Add-Sample "before-load"

try {
    $portForward = Start-Process -FilePath "kubectl" `
        -ArgumentList @("--context", $KubeContext, "port-forward", "service/traffic-service", "$LocalPort`:8082", "--namespace", $Namespace) `
        -WindowStyle Hidden -RedirectStandardOutput $forwardOut -RedirectStandardError $forwardErr -PassThru

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$LocalPort/actuator/health/readiness" -TimeoutSec 3
            if ($health.status -eq "UP") { $ready = $true; break }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $ready) { throw "traffic-service 端口转发后未就绪" }

    $k6Arguments = @(
        "run", "-e", "BASE_URL=http://127.0.0.1:$LocalPort", "-e", "RUN=hpa-$timestamp",
        "--summary-export", $summaryPath, $stressScript
    )
    # 某些 Windows PowerShell 环境不会稳定填充 Start-Process 返回对象的 ExitCode。
    # 后台任务直接返回原生命令的 $LASTEXITCODE，避免将成功压测误判为失败。
    $k6Job = Start-Job -ScriptBlock {
        param($Executable, $Arguments, $StandardOutputPath, $StandardErrorPath)
        & $Executable @Arguments 1> $StandardOutputPath 2> $StandardErrorPath
        $nativeExitCode = $LASTEXITCODE
        if ($null -eq $nativeExitCode) {
            throw "k6 未返回原生退出码"
        }
        [int]$nativeExitCode
    } -ArgumentList $k6Executable, (,$k6Arguments), $k6Out, $k6Err

    while ($k6Job.State -in @("NotStarted", "Running")) {
        Add-Sample "load"
        $replicas = Get-ReplicaCount
        if ($replicas -gt $maximumReplicas) { $maximumReplicas = $replicas }
        Start-Sleep -Seconds $SampleIntervalSeconds
    }
    $k6Job | Wait-Job | Out-Null
    $k6JobState = $k6Job.State
    $k6JobReason = $k6Job.ChildJobs[0].JobStateInfo.Reason
    $k6JobOutput = @($k6Job | Receive-Job -ErrorAction SilentlyContinue)
    $k6Job | Remove-Job -ErrorAction SilentlyContinue
    $k6Job = $null
    if ($k6JobState -ne "Completed" -or $k6JobOutput.Count -eq 0) {
        throw "无法读取 k6 退出码：后台任务状态为 $k6JobState，原因：$k6JobReason；请查看 $k6Out 和 $k6Err"
    }
    $k6ExitCode = [int]$k6JobOutput[-1]
    if ($k6ExitCode -ne 0) {
        throw "k6 压测失败，退出码 $k6ExitCode"
    }

    Add-Sample "load-finished"
    # Metrics Server 与 HPA 控制器存在采样/决策延迟。即使 k6 已结束，
    # 也要继续观察一段时间，避免漏掉随后发生的真实扩容。
    $scaleUpDeadline = (Get-Date).AddSeconds($ScaleUpObservationSeconds)
    $observedReplicas = Get-ReplicaCount
    while ($observedReplicas -le $initialReplicas -and (Get-Date) -lt $scaleUpDeadline) {
        Add-Sample "scale-up-observation"
        Start-Sleep -Seconds $SampleIntervalSeconds
        $observedReplicas = Get-ReplicaCount
        if ($observedReplicas -gt $maximumReplicas) { $maximumReplicas = $observedReplicas }
    }

    $scaleDownDeadline = (Get-Date).AddSeconds($ScaleDownTimeoutSeconds)
    $finalReplicas = $observedReplicas
    while ($finalReplicas -gt $initialReplicas -and (Get-Date) -lt $scaleDownDeadline) {
        Add-Sample "scale-down"
        if ($finalReplicas -gt $maximumReplicas) { $maximumReplicas = $finalReplicas }
        Start-Sleep -Seconds $SampleIntervalSeconds
        $finalReplicas = Get-ReplicaCount
    }
    Add-Sample "finished"

    $evidence = [ordered]@{
        experiment = "TRAFFIC_HPA"
        executedAt = (Get-Date).ToString("o")
        context = $KubeContext
        namespace = $Namespace
        scaleUpObservationSeconds = $ScaleUpObservationSeconds
        initialReplicas = $initialReplicas
        maximumReplicas = $maximumReplicas
        finalReplicas = $finalReplicas
        scaledUp = $maximumReplicas -gt $initialReplicas
        scaledDown = ($maximumReplicas -gt $initialReplicas) -and ($finalReplicas -le $initialReplicas)
        k6ExitCode = $k6ExitCode
        passed = ($maximumReplicas -gt $initialReplicas) -and ($finalReplicas -le $initialReplicas) -and ($k6ExitCode -eq 0)
        sampleLog = [IO.Path]::GetFileName($samplePath)
        k6Summary = [IO.Path]::GetFileName($summaryPath)
    }
    [IO.File]::WriteAllText($resultPath, ($evidence | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
    $evidence | ConvertTo-Json -Depth 4
    if (-not $evidence.passed) { exit 1 }
}
finally {
    if ($k6Job) {
        if ($k6Job.State -in @("NotStarted", "Running")) { $k6Job | Stop-Job }
        $k6Job | Remove-Job -Force -ErrorAction SilentlyContinue
    }
    if ($portForward -and -not $portForward.HasExited) { Stop-Process -Id $portForward.Id -Force }
}
