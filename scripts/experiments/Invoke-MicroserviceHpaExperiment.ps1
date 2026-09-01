[CmdletBinding()]
param(
    [string]$KubeContext = "docker-desktop",
    [string]$Namespace = "travelmate-microservices",
    [int]$LocalPort = 18082,
    [int]$SampleIntervalSeconds = 5,
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
$k6Process = $null

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

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    throw "未安装 k6；请先执行 winget install GrafanaLabs.k6"
}
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

    $k6Process = Start-Process -FilePath "k6" `
        -ArgumentList @(
            "run", "-e", "BASE_URL=http://127.0.0.1:$LocalPort", "-e", "RUN=hpa-$timestamp",
            "--summary-export", $summaryPath, $stressScript
        ) `
        -WindowStyle Hidden -RedirectStandardOutput $k6Out -RedirectStandardError $k6Err -PassThru

    while (-not $k6Process.HasExited) {
        Add-Sample "load"
        $replicas = Get-ReplicaCount
        if ($replicas -gt $maximumReplicas) { $maximumReplicas = $replicas }
        Start-Sleep -Seconds $SampleIntervalSeconds
        $k6Process.Refresh()
    }
    if ($k6Process.ExitCode -ne 0) {
        throw "k6 压测失败，退出码 $($k6Process.ExitCode)"
    }

    Add-Sample "load-finished"
    $scaleDownDeadline = (Get-Date).AddSeconds($ScaleDownTimeoutSeconds)
    $finalReplicas = Get-ReplicaCount
    while ($finalReplicas -gt $initialReplicas -and (Get-Date) -lt $scaleDownDeadline) {
        Add-Sample "scale-down"
        Start-Sleep -Seconds $SampleIntervalSeconds
        $finalReplicas = Get-ReplicaCount
    }
    Add-Sample "finished"

    $evidence = [ordered]@{
        experiment = "TRAFFIC_HPA"
        executedAt = (Get-Date).ToString("o")
        context = $KubeContext
        namespace = $Namespace
        initialReplicas = $initialReplicas
        maximumReplicas = $maximumReplicas
        finalReplicas = $finalReplicas
        scaledUp = $maximumReplicas -gt $initialReplicas
        scaledDown = $finalReplicas -le $initialReplicas
        k6ExitCode = $k6Process.ExitCode
        passed = ($maximumReplicas -gt $initialReplicas) -and ($finalReplicas -le $initialReplicas) -and ($k6Process.ExitCode -eq 0)
        sampleLog = [IO.Path]::GetFileName($samplePath)
        k6Summary = [IO.Path]::GetFileName($summaryPath)
    }
    [IO.File]::WriteAllText($resultPath, ($evidence | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
    $evidence | ConvertTo-Json -Depth 4
    if (-not $evidence.passed) { exit 1 }
}
finally {
    if ($k6Process -and -not $k6Process.HasExited) { Stop-Process -Id $k6Process.Id -Force }
    if ($portForward -and -not $portForward.HasExited) { Stop-Process -Id $portForward.Id -Force }
}
