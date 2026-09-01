[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ReleaseEvidencePath,
    [string]$ExpectedCommit = "",
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$EvidenceDirectory = "",
    [int]$DatabaseBootstrapTimeoutSeconds = 180,
    [int]$RolloutTimeoutSeconds = 300
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Invoke-Kubectl {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & kubectl --context $KubeContext @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "kubectl failed: kubectl $($Arguments -join ' ')`n$($output -join [Environment]::NewLine)"
    }
    return @($output)
}

function New-HexSecret {
    param([int]$Length = 32)

    $bytes = [byte[]]::new($Length)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    }
    finally {
        $generator.Dispose()
    }
    return ([BitConverter]::ToString($bytes)).Replace("-", "").ToLowerInvariant()
}

function Write-Utf8NoBom {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Content
    )

    [IO.File]::WriteAllText($Path, $Content, [Text.UTF8Encoding]::new($false))
}

if (-not (Test-Path -LiteralPath $ReleaseEvidencePath -PathType Leaf)) {
    throw "Microservice release evidence is missing: $ReleaseEvidencePath"
}
if (-not (Get-Command kubectl -ErrorAction SilentlyContinue)) {
    throw "kubectl is not available"
}

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$kubernetesDirectory = Join-Path $repositoryRoot "deploy\k8s"
$schemaDirectory = Join-Path $repositoryRoot "microservices\sql"
if (-not $EvidenceDirectory) {
    $EvidenceDirectory = Join-Path $env:TEMP ("travelmate-microservice-deploy-" + [guid]::NewGuid())
}
[IO.Directory]::CreateDirectory($EvidenceDirectory) | Out-Null

$serviceSpecs = [ordered]@{
    "identity-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-identity-service"
        Manifest = "identity-service.yaml"
    }
    "traffic-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-traffic-service"
        Manifest = "traffic-service.yaml"
    }
    "local-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-local-service"
        Manifest = "local-service.yaml"
    }
    "ai-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-ai-service"
        Manifest = "ai-service.yaml"
    }
    "community-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-community-service"
        Manifest = "community-service.yaml"
    }
    "ops-service" = [ordered]@{
        Image = "ghcr.io/miracle060811/travelmate-ops-service"
        Manifest = "ops-service.yaml"
    }
}

$release = Get-Content -LiteralPath $ReleaseEvidencePath -Raw | ConvertFrom-Json
$releaseCommit = [string]$release.commit
if ($releaseCommit -notmatch '^[0-9a-f]{40}$') {
    throw "Release evidence contains an invalid commit SHA"
}
if ($ExpectedCommit -and $releaseCommit -ne $ExpectedCommit) {
    throw "Release evidence commit mismatch: expected $ExpectedCommit, found $releaseCommit"
}
if ([string]$release.tag -ne "sha-$releaseCommit") {
    throw "Release evidence tag is not the immutable SHA tag for $releaseCommit"
}

$records = @($release.images)
if ($records.Count -ne $serviceSpecs.Count) {
    throw "Release evidence must contain exactly six service images"
}
$imageRecords = @{}
foreach ($record in $records) {
    $service = [string]$record.service
    if (-not $serviceSpecs.Contains($service)) {
        throw "Release evidence contains an unexpected service: $service"
    }
    if ($imageRecords.ContainsKey($service)) {
        throw "Release evidence contains a duplicate service: $service"
    }
    if ([string]$record.image -ne [string]$serviceSpecs[$service].Image) {
        throw "Release evidence image mismatch for $service"
    }
    if ([string]$record.commit -ne $releaseCommit -or [string]$record.tag -ne "sha-$releaseCommit") {
        throw "Release evidence version mismatch for $service"
    }
    if ([string]$record.digest -notmatch '^sha256:[0-9a-f]{64}$') {
        throw "Release evidence digest is invalid for $service"
    }
    $imageRecords[$service] = $record
}

$secretRaw = (Invoke-Kubectl -Arguments @("get", "secret", "travelmate-secrets", "-n", $Namespace, "-o", "json")) -join "`n"
$secret = $secretRaw | ConvertFrom-Json
$secretKeys = @($secret.data.PSObject.Properties.Name)
foreach ($requiredKey in @("mysql-root-password", "mysql-password", "jwt-secret", "admin-register-secret")) {
    if ($secretKeys -notcontains $requiredKey) {
        throw "travelmate-secrets is missing required key '$requiredKey'"
    }
}
if ($secretKeys -notcontains "internal-service-token") {
    $token = New-HexSecret
    $encodedToken = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($token))
    $patchJson = @{ data = @{ "internal-service-token" = $encodedToken } } | ConvertTo-Json -Depth 3 -Compress
    $patchPath = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-internal-token-" + [guid]::NewGuid() + ".json")
    try {
        Write-Utf8NoBom -Path $patchPath -Content $patchJson
        Invoke-Kubectl -Arguments @(
            "patch", "secret", "travelmate-secrets", "-n", $Namespace,
            "--type=merge", "--patch-file=$patchPath"
        ) | Out-Null
    }
    finally {
        Remove-Item -LiteralPath $patchPath -Force -ErrorAction SilentlyContinue
    }
    Write-Output "Added the missing internal service token without printing its value."
}

$schemaArguments = @("create", "configmap", "travelmate-microservice-schemas", "-n", $Namespace)
foreach ($service in @("identity", "traffic", "local", "ai", "community", "ops")) {
    foreach ($kind in @("schema", "seed")) {
        $fileName = "$service-$kind.sql"
        $filePath = Join-Path $schemaDirectory $fileName
        if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) {
            throw "Required microservice SQL file is missing: $filePath"
        }
        $schemaArguments += "--from-file=$fileName=$filePath"
    }
}
$schemaArguments += @("--dry-run=client", "-o", "yaml")
$schemaManifest = Invoke-Kubectl -Arguments $schemaArguments
$schemaManifest | & kubectl --context $KubeContext apply -f -
if ($LASTEXITCODE -ne 0) {
    throw "Unable to apply travelmate-microservice-schemas"
}

Invoke-Kubectl -Arguments @(
    "rollout", "status", "statefulset/travelmate-mysql", "-n", $Namespace,
    "--timeout=${DatabaseBootstrapTimeoutSeconds}s"
) | Out-Null
Invoke-Kubectl -Arguments @(
    "delete", "job", "travelmate-microservice-db-bootstrap", "-n", $Namespace,
    "--ignore-not-found=true", "--wait=true"
) | Out-Null
Invoke-Kubectl -Arguments @(
    "apply", "-f", (Join-Path $kubernetesDirectory "microservice-database-bootstrap.yaml"),
    "-n", $Namespace
) | Out-Null

$databaseReady = $false
$databaseDeadline = (Get-Date).AddSeconds($DatabaseBootstrapTimeoutSeconds)
do {
    $succeededValue = (Invoke-Kubectl -Arguments @(
        "get", "job", "travelmate-microservice-db-bootstrap", "-n", $Namespace,
        "-o", "jsonpath={.status.succeeded}"
    )) -join ""
    $failedValue = (Invoke-Kubectl -Arguments @(
        "get", "job", "travelmate-microservice-db-bootstrap", "-n", $Namespace,
        "-o", "jsonpath={.status.failed}"
    )) -join ""
    $succeeded = if ($succeededValue) { [int]$succeededValue } else { 0 }
    $failed = if ($failedValue) { [int]$failedValue } else { 0 }
    if ($succeeded -ge 1) {
        $databaseReady = $true
        break
    }
    if ($failed -ge 1) {
        break
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $databaseDeadline)

$databaseLog = & kubectl --context $KubeContext logs job/travelmate-microservice-db-bootstrap -n $Namespace 2>&1
$databaseLog | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "microservice-database-bootstrap.log") -Encoding utf8
if (-not $databaseReady) {
    & kubectl --context $KubeContext describe job travelmate-microservice-db-bootstrap -n $Namespace 2>&1 |
        Set-Content -LiteralPath (Join-Path $EvidenceDirectory "microservice-database-bootstrap-describe.txt") -Encoding utf8
    throw "Microservice database bootstrap did not complete within $DatabaseBootstrapTimeoutSeconds seconds"
}

$renderDirectory = Join-Path $EvidenceDirectory "microservice-render"
[IO.Directory]::CreateDirectory($renderDirectory) | Out-Null
Copy-Item -LiteralPath (Join-Path $kubernetesDirectory "microservices-configmap.yaml") -Destination $renderDirectory -Force
foreach ($service in $serviceSpecs.Keys) {
    $manifest = [string]$serviceSpecs[$service].Manifest
    Copy-Item -LiteralPath (Join-Path $kubernetesDirectory $manifest) -Destination $renderDirectory -Force
}

$kustomizationLines = @(
    "apiVersion: kustomize.config.k8s.io/v1beta1",
    "kind: Kustomization",
    "namespace: $Namespace",
    "resources:",
    "  - microservices-configmap.yaml"
)
foreach ($service in $serviceSpecs.Keys) {
    $kustomizationLines += "  - $([string]$serviceSpecs[$service].Manifest)"
}
$kustomizationLines += "images:"
foreach ($service in $serviceSpecs.Keys) {
    $record = $imageRecords[$service]
    $kustomizationLines += @(
        "  - name: $([string]$serviceSpecs[$service].Image)",
        "    newName: $([string]$record.image)",
        "    digest: $([string]$record.digest)"
    )
}
$kustomizationPath = Join-Path $renderDirectory "kustomization.yaml"
Write-Utf8NoBom -Path $kustomizationPath -Content (($kustomizationLines -join "`n") + "`n")

$rendered = Invoke-Kubectl -Arguments @("kustomize", $renderDirectory)
$renderedPath = Join-Path $EvidenceDirectory "microservices-rendered.yaml"
$rendered | Set-Content -LiteralPath $renderedPath -Encoding utf8
$rendered | & kubectl --context $KubeContext apply -f -
if ($LASTEXITCODE -ne 0) {
    throw "Unable to apply the six microservice Kubernetes resources"
}

$imageSummary = [System.Collections.Generic.List[string]]::new()
foreach ($service in $serviceSpecs.Keys) {
    $record = $imageRecords[$service]
    Invoke-Kubectl -Arguments @(
        "annotate", "deployment/$service", "-n", $Namespace,
        "travelmate.io/commit=$releaseCommit",
        "travelmate.io/image-digest=$([string]$record.digest)",
        "--overwrite"
    ) | Out-Null
    Invoke-Kubectl -Arguments @(
        "rollout", "status", "deployment/$service", "-n", $Namespace,
        "--timeout=${RolloutTimeoutSeconds}s"
    ) | Out-Null
    $imageSummary.Add("$service=$([string]$record.image)@$([string]$record.digest)")
}
$imageSummary | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "microservice-images.txt") -Encoding utf8
Copy-Item -LiteralPath $ReleaseEvidencePath -Destination (Join-Path $EvidenceDirectory "microservice-release.json") -Force

Write-Output "Deployed six microservices for commit $releaseCommit from immutable GHCR digests."
