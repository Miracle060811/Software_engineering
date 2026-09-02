[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ReleaseEvidencePath,
    [string]$ExpectedCommit = "",
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [string]$EvidenceDirectory = "",
    [int]$DatabaseBootstrapTimeoutSeconds = 180,
    [int]$RolloutTimeoutSeconds = 300,
    [string]$Service = ""
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

function Get-DeploymentState {
    param([Parameter(Mandatory = $true)][string]$Deployment)

    $raw = (Invoke-Kubectl -Arguments @("get", "deployment/$Deployment", "-n", $Namespace, "-o", "json")) -join "`n"
    $resource = $raw | ConvertFrom-Json
    $container = @($resource.spec.template.spec.containers | Where-Object { [string]$_.name -eq $Deployment })
    if ($container.Count -ne 1) {
        throw "Deployment $Deployment must contain exactly one container named $Deployment"
    }
    $annotations = $resource.metadata.annotations
    $commitProperty = if ($null -ne $annotations) { $annotations.PSObject.Properties["travelmate.io/commit"] } else { $null }
    $digestProperty = if ($null -ne $annotations) { $annotations.PSObject.Properties["travelmate.io/image-digest"] } else { $null }

    return [pscustomobject]@{
        Service = $Deployment
        Image = [string]$container[0].image
        CommitExists = $null -ne $commitProperty
        Commit = if ($null -ne $commitProperty) { [string]$commitProperty.Value } else { "" }
        DigestExists = $null -ne $digestProperty
        Digest = if ($null -ne $digestProperty) { [string]$digestProperty.Value } else { "" }
    }
}

function Restore-DeploymentAnnotation {
    param(
        [Parameter(Mandatory = $true)][string]$Deployment,
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][bool]$Exists,
        [string]$Value = ""
    )

    $annotationArgument = if ($Exists) { "$Name=$Value" } else { "$Name-" }
    Invoke-Kubectl -Arguments @(
        "annotate", "deployment/$Deployment", "-n", $Namespace,
        $annotationArgument, "--overwrite"
    ) | Out-Null
}

function Invoke-RollbackStep {
    param(
        [Parameter(Mandatory = $true)][string]$Description,
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[string]]$Failures,
        [Parameter(Mandatory = $true)][scriptblock]$Action
    )

    try {
        & $Action
    }
    catch {
        $Failures.Add("${Description}: $($_.Exception.Message)")
    }
}

function Write-DeploymentStateEvidence {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][object]$State
    )

    Write-Utf8NoBom -Path $Path -Content (($State | ConvertTo-Json -Depth 5) + "`n")
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

if ($Service -and -not $serviceSpecs.Contains($Service)) {
    throw "Unsupported service '$Service'. Expected one of: $($serviceSpecs.Keys -join ', ')"
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
    $recordService = [string]$record.service
    if (-not $serviceSpecs.Contains($recordService)) {
        throw "Release evidence contains an unexpected service: $recordService"
    }
    if ($imageRecords.ContainsKey($recordService)) {
        throw "Release evidence contains a duplicate service: $recordService"
    }
    if ([string]$record.image -ne [string]$serviceSpecs[$recordService].Image) {
        throw "Release evidence image mismatch for $recordService"
    }
    if ([string]$record.commit -ne $releaseCommit -or [string]$record.tag -ne "sha-$releaseCommit") {
        throw "Release evidence version mismatch for $recordService"
    }
    if ([string]$record.digest -notmatch '^sha256:[0-9a-f]{64}$') {
        throw "Release evidence digest is invalid for $recordService"
    }
    $imageRecords[$recordService] = $record
}

if ($Service) {
    $record = $imageRecords[$Service]
    $targetImage = "$([string]$record.image)@$([string]$record.digest)"
    $previousState = Get-DeploymentState -Deployment $Service
    Write-DeploymentStateEvidence -Path (Join-Path $EvidenceDirectory "$Service-before.json") -State $previousState

    $filteredRelease = [ordered]@{
        commit = $releaseCommit
        tag = "sha-$releaseCommit"
        images = @($record)
    }
    Write-Utf8NoBom -Path (Join-Path $EvidenceDirectory "$Service-release.json") `
        -Content (($filteredRelease | ConvertTo-Json -Depth 8) + "`n")

    try {
        Invoke-Kubectl -Arguments @(
            "set", "image", "deployment/$Service", "$Service=$targetImage", "-n", $Namespace
        ) | Out-Null
        Invoke-Kubectl -Arguments @(
            "annotate", "deployment/$Service", "-n", $Namespace,
            "travelmate.io/commit=$releaseCommit",
            "travelmate.io/image-digest=$([string]$record.digest)",
            "--overwrite"
        ) | Out-Null
        $rolloutOutput = Invoke-Kubectl -Arguments @(
            "rollout", "status", "deployment/$Service", "-n", $Namespace,
            "--timeout=${RolloutTimeoutSeconds}s"
        )
        $rolloutOutput | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "$Service-rollout.log") -Encoding utf8

        $currentState = Get-DeploymentState -Deployment $Service
        if ($currentState.Image -ne $targetImage -or
            $currentState.Commit -ne $releaseCommit -or
            $currentState.Digest -ne [string]$record.digest) {
            throw "Deployment $Service state does not match the requested immutable release after rollout"
        }
        Write-DeploymentStateEvidence -Path (Join-Path $EvidenceDirectory "$Service-after.json") -State $currentState
        "$Service=$targetImage" | Set-Content -LiteralPath (Join-Path $EvidenceDirectory "microservice-images.txt") -Encoding utf8
    }
    catch {
        $rolloutFailure = $_.Exception.Message
        Write-Utf8NoBom -Path (Join-Path $EvidenceDirectory "$Service-failure.txt") -Content ($rolloutFailure + "`n")
        $rollbackFailures = [System.Collections.Generic.List[string]]::new()

        Invoke-RollbackStep -Description "restore $Service image" -Failures $rollbackFailures -Action {
            Invoke-Kubectl -Arguments @(
                "set", "image", "deployment/$Service", "$Service=$($previousState.Image)", "-n", $Namespace
            ) | Out-Null
        }
        Invoke-RollbackStep -Description "restore $Service commit annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment $Service -Name "travelmate.io/commit" `
                -Exists $previousState.CommitExists -Value $previousState.Commit
        }
        Invoke-RollbackStep -Description "restore $Service digest annotation" -Failures $rollbackFailures -Action {
            Restore-DeploymentAnnotation -Deployment $Service -Name "travelmate.io/image-digest" `
                -Exists $previousState.DigestExists -Value $previousState.Digest
        }
        Invoke-RollbackStep -Description "wait for $Service rollback" -Failures $rollbackFailures -Action {
            Invoke-Kubectl -Arguments @(
                "rollout", "status", "deployment/$Service", "-n", $Namespace,
                "--timeout=${RolloutTimeoutSeconds}s"
            ) | Out-Null
        }
        try {
            $rollbackState = Get-DeploymentState -Deployment $Service
            Write-DeploymentStateEvidence -Path (Join-Path $EvidenceDirectory "$Service-rollback.json") -State $rollbackState
        }
        catch {
            $rollbackFailures.Add("capture $Service rollback state: $($_.Exception.Message)")
        }

        if ($rollbackFailures.Count -gt 0) {
            throw "Deployment rollout failed for ${Service}: $rolloutFailure Rollback also encountered: $($rollbackFailures -join ' | ')"
        }
        throw "Deployment rollout failed for $Service; previous image and annotations were restored. Original failure: $rolloutFailure"
    }

    Write-Output "Deployed $Service for commit $releaseCommit from immutable GHCR digest; no other service was updated."
    return
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
