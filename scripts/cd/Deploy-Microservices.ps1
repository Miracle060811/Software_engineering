[CmdletBinding()]
param(
    [string]$KubeContext = "docker-desktop",
    [string]$Namespace = "travelmate-microservices",
    [string]$EnvFile,
    [string]$ImageTag,
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$manifestDirectory = Join-Path $repoRoot "microservices\k8s"
$microservicesDirectory = Join-Path $repoRoot "microservices"
if ($Namespace -ne "travelmate-microservices") {
    throw "当前 Kustomize 清单固定使用命名空间 travelmate-microservices"
}
if (-not $EnvFile) {
    $EnvFile = Join-Path $microservicesDirectory ".env"
}

function Invoke-Checked([string]$Command, [string[]]$Arguments) {
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Command 执行失败，退出码 $LASTEXITCODE"
    }
}

function Read-DotEnv([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "缺少环境文件：$Path"
    }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding utf8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith("#")) { continue }
        $separator = $trimmed.IndexOf("=")
        if ($separator -lt 1) { continue }
        $values[$trimmed.Substring(0, $separator).Trim()] = $trimmed.Substring($separator + 1)
    }
    return $values
}

function ConvertTo-Base64([string]$Value) {
    return [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Value))
}

function Apply-Json([object]$Object) {
    $json = $Object | ConvertTo-Json -Depth 8 -Compress
    $json | & kubectl --context $KubeContext apply -f -
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl apply 失败"
    }
}

$contexts = & kubectl config get-contexts $KubeContext -o name 2>$null
if ($LASTEXITCODE -ne 0 -or -not $contexts) {
    throw "Kubernetes context '$KubeContext' 不存在；请先在 Docker Desktop 中启用 Kubernetes"
}

$settings = Read-DotEnv $EnvFile
$requiredSettings = @(
    "MYSQL_ROOT_PASSWORD",
    "IDENTITY_DB_PASSWORD",
    "TRAFFIC_DB_PASSWORD",
    "LOCAL_DB_PASSWORD",
    "AI_DB_PASSWORD",
    "JWT_SECRET",
    "INTERNAL_SERVICE_TOKEN"
)
foreach ($name in $requiredSettings) {
    if (-not $settings.ContainsKey($name) -or [string]::IsNullOrWhiteSpace($settings[$name])) {
        throw "环境文件缺少必填配置：$name"
    }
}
if (-not $settings.ContainsKey("ADMIN_REGISTER_SECRET") -or [string]::IsNullOrWhiteSpace($settings["ADMIN_REGISTER_SECRET"])) {
    $settings["ADMIN_REGISTER_SECRET"] = "disabled"
}

if (-not $ImageTag) {
    $commit = (& git -C $repoRoot rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw "无法读取 Git 提交号" }
    $ImageTag = "sha-$commit"
}
if ($ImageTag -eq "latest") {
    throw "镜像标签不能使用 latest"
}

$images = [ordered]@{
    "identity-service" = "travelmate/identity-service:$ImageTag"
    "traffic-service" = "travelmate/traffic-service:$ImageTag"
    "local-service" = "travelmate/local-service:$ImageTag"
    "ai-service" = "travelmate/ai-service:$ImageTag"
}

if (-not $SkipBuild) {
    Invoke-Checked (Join-Path $repoRoot "backend\mvnw.cmd") @(
        "-f", (Join-Path $microservicesDirectory "pom.xml"),
        "--batch-mode", "--no-transfer-progress", "clean", "verify"
    )
    foreach ($service in $images.Keys) {
        Invoke-Checked "docker" @(
            "build",
            "--tag", $images[$service],
            (Join-Path $microservicesDirectory "services\$service")
        )
    }
}

Invoke-Checked "kubectl" @("--context", $KubeContext, "apply", "-f", (Join-Path $manifestDirectory "namespace.yaml"))

$secretData = [ordered]@{
    "mysql-root-password" = ConvertTo-Base64 $settings["MYSQL_ROOT_PASSWORD"]
    "identity-db-password" = ConvertTo-Base64 $settings["IDENTITY_DB_PASSWORD"]
    "traffic-db-password" = ConvertTo-Base64 $settings["TRAFFIC_DB_PASSWORD"]
    "local-db-password" = ConvertTo-Base64 $settings["LOCAL_DB_PASSWORD"]
    "ai-db-password" = ConvertTo-Base64 $settings["AI_DB_PASSWORD"]
    "jwt-secret" = ConvertTo-Base64 $settings["JWT_SECRET"]
    "internal-service-token" = ConvertTo-Base64 $settings["INTERNAL_SERVICE_TOKEN"]
    "admin-register-secret" = ConvertTo-Base64 $settings["ADMIN_REGISTER_SECRET"]
}
Apply-Json ([ordered]@{
    apiVersion = "v1"
    kind = "Secret"
    metadata = [ordered]@{ name = "travelmate-microservices-secrets"; namespace = $Namespace }
    type = "Opaque"
    data = $secretData
})

$databaseInit = [ordered]@{
    "identity-db-init" = @("identity-schema.sql", "identity-seed.sql")
    "traffic-db-init" = @("traffic-schema.sql", "traffic-seed.sql")
    "local-db-init" = @("local-schema.sql", "local-seed.sql")
    "ai-db-init" = @("ai-schema.sql", "ai-seed.sql")
}
foreach ($configMapName in $databaseInit.Keys) {
    $files = $databaseInit[$configMapName]
    $arguments = @(
        "--context", $KubeContext, "create", "configmap", $configMapName,
        "--namespace", $Namespace,
        "--from-file=001-schema.sql=$(Join-Path $microservicesDirectory "sql\$($files[0])")",
        "--from-file=002-seed.sql=$(Join-Path $microservicesDirectory "sql\$($files[1])")",
        "--dry-run=client", "-o", "json"
    )
    $configMapJson = & kubectl @arguments
    if ($LASTEXITCODE -ne 0) { throw "生成 ConfigMap $configMapName 失败" }
    $configMapJson | & kubectl --context $KubeContext apply -f -
    if ($LASTEXITCODE -ne 0) { throw "应用 ConfigMap $configMapName 失败" }
}

Invoke-Checked "kubectl" @("--context", $KubeContext, "apply", "-k", $manifestDirectory)
foreach ($service in $images.Keys) {
    Invoke-Checked "kubectl" @(
        "--context", $KubeContext, "set", "image",
        "deployment/$service", "$service=$($images[$service])",
        "--namespace", $Namespace
    )
}

foreach ($statefulSet in @("identity-db", "traffic-db", "local-db", "ai-db", "redis")) {
    Invoke-Checked "kubectl" @(
        "--context", $KubeContext, "rollout", "status", "statefulset/$statefulSet",
        "--namespace", $Namespace, "--timeout=10m"
    )
}
foreach ($service in $images.Keys) {
    Invoke-Checked "kubectl" @(
        "--context", $KubeContext, "rollout", "status", "deployment/$service",
        "--namespace", $Namespace, "--timeout=10m"
    )
}

Invoke-Checked "kubectl" @(
    "--context", $KubeContext, "get", "pods,services,pvc,hpa",
    "--namespace", $Namespace, "-o", "wide"
)
