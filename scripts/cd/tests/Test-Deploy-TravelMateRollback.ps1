[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$deployScript = (Resolve-Path (Join-Path $PSScriptRoot "..\Deploy-TravelMate.ps1")).Path
$oldCommit = "cccccccccccccccccccccccccccccccccccccccc"
$targetCommit = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
$oldBackendDigest = "sha256:" + ("1" * 64)
$oldFrontendDigest = "sha256:" + ("2" * 64)
$targetBackendDigest = "sha256:" + ("a" * 64)
$targetFrontendDigest = "sha256:" + ("b" * 64)
$oldBackendImage = "ghcr.io/miracle060811/travelmate-backend@$oldBackendDigest"
$oldFrontendImage = "ghcr.io/miracle060811/travelmate-frontend@$oldFrontendDigest"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw "Assertion failed: $Message"
    }
}

function Test-ObjectProperty {
    param([object]$Object, [string]$Name)
    return ($null -ne $Object.PSObject.Properties[$Name])
}

function Invoke-RollbackScenario {
    param(
        [string]$Name,
        [ValidateSet("rollout", "frontend-set")]
        [string]$FailureStage = "rollout",
        [switch]$AnnotationsMissing,
        [switch]$FailFrontendDigestRemoval
    )

    $root = Join-Path ([IO.Path]::GetTempPath()) "travelmate-cd-test-$([guid]::NewGuid().ToString('N'))"
    $bin = Join-Path $root "bin"
    $scriptDirectory = Join-Path $root "script"
    $profile = Join-Path $root "profile"
    [IO.Directory]::CreateDirectory($bin) | Out-Null
    [IO.Directory]::CreateDirectory($scriptDirectory) | Out-Null
    [IO.Directory]::CreateDirectory($profile) | Out-Null

    try {
        Copy-Item -LiteralPath $deployScript -Destination (Join-Path $scriptDirectory "Deploy-TravelMate.ps1")
        Set-Content -LiteralPath (Join-Path $scriptDirectory "Ensure-KindProxy.ps1") -Encoding utf8 -Value 'Write-Output "fake proxy ready"'

        $backendAnnotations = if ($AnnotationsMissing) {
            [ordered]@{}
        }
        else {
            [ordered]@{
                "travelmate.io/commit" = $oldCommit
                "travelmate.io/image-digest" = $oldBackendDigest
            }
        }
        $frontendAnnotations = if ($AnnotationsMissing) {
            [ordered]@{}
        }
        else {
            [ordered]@{
                "travelmate.io/commit" = $oldCommit
                "travelmate.io/image-digest" = $oldFrontendDigest
            }
        }
        $statePath = Join-Path $root "state.json"
        $callLogPath = Join-Path $root "kubectl-calls.log"
        [ordered]@{
            failureStage = $FailureStage
            failFrontendDigestRemoval = [bool]$FailFrontendDigestRemoval
            targetBackendImage = "ghcr.io/miracle060811/travelmate-backend@$targetBackendDigest"
            targetFrontendImage = "ghcr.io/miracle060811/travelmate-frontend@$targetFrontendDigest"
            deployments = [ordered]@{
                "travelmate-backend" = [ordered]@{
                    container = "backend"
                    image = $oldBackendImage
                    annotations = $backendAnnotations
                }
                "travelmate-frontend" = [ordered]@{
                    container = "frontend"
                    image = $oldFrontendImage
                    annotations = $frontendAnnotations
                }
            }
        } | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $statePath -Encoding utf8

        $dockerFake = @'
$Arguments = [string[]]$args
if ($Arguments[0] -eq "info") { Write-Output "fake-docker"; exit 0 }
if ($Arguments[0] -eq "inspect") { Write-Output "{}"; exit 0 }
if ($Arguments[0] -eq "pull") { Write-Output "pulled"; exit 0 }
if ($Arguments[0] -eq "image" -and $Arguments[1] -eq "inspect") {
    $image = $Arguments[2]
    $backend = $image -like "*travelmate-backend*"
    $repository = if ($backend) { "ghcr.io/miracle060811/travelmate-backend" } else { "ghcr.io/miracle060811/travelmate-frontend" }
    $digest = if ($backend) { "sha256:" + ("a" * 64) } else { "sha256:" + ("b" * 64) }
    @([ordered]@{
        Config = [ordered]@{ Labels = [ordered]@{ "org.opencontainers.image.revision" = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" } }
        RepoDigests = @("$repository@$digest")
    }) | ConvertTo-Json -Depth 6 -Compress | Write-Output
    exit 0
}
Write-Error "Unexpected docker arguments: $($Arguments -join ' ')"
exit 90
'@
        Set-Content -LiteralPath (Join-Path $bin "docker-fake.ps1") -Value $dockerFake -Encoding utf8
        Set-Content -LiteralPath (Join-Path $bin "docker.cmd") -Encoding ascii -Value @(
            '@echo off',
            'pwsh -NoLogo -NoProfile -File "%~dp0docker-fake.ps1" %*',
            'exit /b %errorlevel%'
        )

        $kubectlFake = @'
$Arguments = [string[]]$args
$ErrorActionPreference = "Stop"
Add-Content -LiteralPath $env:TM_FAKE_CALL_LOG -Value ($Arguments -join " ") -Encoding utf8
$state = Get-Content -LiteralPath $env:TM_FAKE_STATE -Raw | ConvertFrom-Json

function Save-State {
    $state | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $env:TM_FAKE_STATE -Encoding utf8
}

function Get-DeploymentName([string]$Value) {
    return ($Value -replace '^deployment/', '')
}

if ($Arguments[0] -eq "config" -and $Arguments[1] -eq "get-contexts") { Write-Output "docker-desktop"; exit 0 }
if ($Arguments[0] -eq "config" -and $Arguments[1] -eq "use-context") { Write-Output "docker-desktop"; exit 0 }
if ($Arguments[0] -eq "get" -and $Arguments[1] -eq "nodes") { Write-Output "fake-node"; exit 0 }
if ($Arguments[0] -eq "wait") { Write-Output "ready"; exit 0 }

if ($Arguments[0] -eq "get" -and $Arguments[1] -eq "deployment") {
    $name = Get-DeploymentName $Arguments[2]
    $deployment = $state.deployments.PSObject.Properties[$name].Value
    $outputIndex = [Array]::IndexOf($Arguments, "-o")
    if ($outputIndex -ge 0 -and $Arguments[$outputIndex + 1] -like "jsonpath=*") {
        $commit = $deployment.annotations.PSObject.Properties["travelmate.io/commit"]
        if ($null -ne $commit) { Write-Output -NoEnumerate ([string]$commit.Value) }
        exit 0
    }
    [ordered]@{
        metadata = [ordered]@{ annotations = $deployment.annotations }
        spec = [ordered]@{
            replicas = 2
            template = [ordered]@{
                spec = [ordered]@{
                    containers = @([ordered]@{ name = $deployment.container; image = $deployment.image })
                }
            }
        }
        status = [ordered]@{ availableReplicas = 2 }
    } | ConvertTo-Json -Depth 10 -Compress | Write-Output
    exit 0
}

if ($Arguments[0] -eq "set" -and $Arguments[1] -eq "image") {
    $name = Get-DeploymentName $Arguments[2]
    $assignment = $Arguments[3]
    $image = $assignment.Substring($assignment.IndexOf('=') + 1)
    if ($state.failureStage -eq "frontend-set" -and $name -eq "travelmate-frontend" -and $image -eq $state.targetFrontendImage) {
        Write-Error "simulated frontend set image failure"
        exit 41
    }
    $state.deployments.PSObject.Properties[$name].Value.image = $image
    Save-State
    Write-Output "image updated"
    exit 0
}

if ($Arguments[0] -eq "annotate") {
    $name = Get-DeploymentName $Arguments[1]
    $deployment = $state.deployments.PSObject.Properties[$name].Value
    foreach ($argument in $Arguments[2..($Arguments.Count - 1)]) {
        if ($argument -like "-*" -or $argument -eq "--overwrite") { continue }
        if ($argument -match '^([^=]+)=(.*)$') {
            $key = $Matches[1]
            $value = $Matches[2]
            $property = $deployment.annotations.PSObject.Properties[$key]
            if ($null -eq $property) { $deployment.annotations | Add-Member -NotePropertyName $key -NotePropertyValue $value }
            else { $property.Value = $value }
        }
        elseif ($argument.EndsWith('-')) {
            $key = $argument.Substring(0, $argument.Length - 1)
            if ($state.failFrontendDigestRemoval -and $name -eq "travelmate-frontend" -and $key -eq "travelmate.io/image-digest") {
                Write-Error "simulated annotation removal failure"
                exit 42
            }
            $deployment.annotations.PSObject.Properties.Remove($key)
        }
    }
    Save-State
    Write-Output "annotated"
    exit 0
}

if ($Arguments[0] -eq "rollout" -and $Arguments[1] -eq "status") {
    $name = Get-DeploymentName $Arguments[2]
    $deployment = $state.deployments.PSObject.Properties[$name].Value
    if ($state.failureStage -eq "rollout" -and $name -eq "travelmate-backend" -and $deployment.image -eq $state.targetBackendImage) {
        Write-Error "simulated backend rollout timeout"
        exit 43
    }
    Write-Output "rollout complete"
    exit 0
}

Write-Error "Unexpected kubectl arguments: $($Arguments -join ' ')"
exit 91
'@
        Set-Content -LiteralPath (Join-Path $bin "kubectl-fake.ps1") -Value $kubectlFake -Encoding utf8
        Set-Content -LiteralPath (Join-Path $bin "kubectl.cmd") -Encoding ascii -Value @(
            '@echo off',
            'pwsh -NoLogo -NoProfile -File "%~dp0kubectl-fake.ps1" %*',
            'exit /b %errorlevel%'
        )

        $savedPath = $env:PATH
        $savedProfile = $env:USERPROFILE
        $savedState = $env:TM_FAKE_STATE
        $savedCallLog = $env:TM_FAKE_CALL_LOG
        try {
            $env:PATH = "$bin;$savedPath"
            $env:USERPROFILE = $profile
            $env:TM_FAKE_STATE = $statePath
            $env:TM_FAKE_CALL_LOG = $callLogPath
            $output = @(& pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File (Join-Path $scriptDirectory "Deploy-TravelMate.ps1") -Namespace "tm-test" -RolloutTimeoutSeconds 1 2>&1)
            $exitCode = $LASTEXITCODE
        }
        finally {
            $env:PATH = $savedPath
            $env:USERPROFILE = $savedProfile
            $env:TM_FAKE_STATE = $savedState
            $env:TM_FAKE_CALL_LOG = $savedCallLog
        }

        return [pscustomobject]@{
            Name = $Name
            ExitCode = $exitCode
            State = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
            Calls = if (Test-Path -LiteralPath $callLogPath) { Get-Content -LiteralPath $callLogPath -Raw } else { "" }
            DeployLog = if (Test-Path -LiteralPath (Join-Path $profile "TravelMateCD\deploy.log")) { Get-Content -LiteralPath (Join-Path $profile "TravelMateCD\deploy.log") -Raw } else { "" }
            Output = $output -join [Environment]::NewLine
        }
    }
    finally {
        $resolvedTemp = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
        $resolvedRoot = [IO.Path]::GetFullPath($root)
        if ($resolvedRoot.StartsWith($resolvedTemp, [StringComparison]::OrdinalIgnoreCase) -and
            (Split-Path -Leaf $resolvedRoot) -like "travelmate-cd-test-*") {
            Remove-Item -LiteralPath $resolvedRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
    }
}

$exact = Invoke-RollbackScenario -Name "exact annotations"
Assert-True ($exact.ExitCode -ne 0) "rollout failure must exit non-zero"
Assert-True ($exact.State.deployments."travelmate-backend".image -eq $oldBackendImage) "backend image must be restored"
Assert-True ($exact.State.deployments."travelmate-frontend".image -eq $oldFrontendImage) "frontend image must be restored"
Assert-True ($exact.State.deployments."travelmate-backend".annotations."travelmate.io/commit" -eq $oldCommit) "backend commit must be restored"
Assert-True ($exact.State.deployments."travelmate-frontend".annotations."travelmate.io/commit" -eq $oldCommit) "frontend commit must be restored"
Assert-True ($exact.State.deployments."travelmate-backend".annotations."travelmate.io/image-digest" -eq $oldBackendDigest) "backend digest must be restored"
Assert-True ($exact.State.deployments."travelmate-frontend".annotations."travelmate.io/image-digest" -eq $oldFrontendDigest) "frontend digest must be restored"
Assert-True ($exact.Calls -match [regex]::Escape("travelmate.io/commit=$targetCommit")) "candidate commit must be written before the simulated failure"
Assert-True ($exact.Calls -match [regex]::Escape("travelmate.io/image-digest=$targetBackendDigest")) "candidate digest must be written before the simulated failure"
Assert-True ($exact.DeployLog -notmatch "deployed successfully|already deployed and healthy") "failed deployment must not be logged as successful"

$missing = Invoke-RollbackScenario -Name "missing annotations" -AnnotationsMissing
Assert-True ($missing.ExitCode -ne 0) "missing-annotation rollout failure must exit non-zero"
foreach ($deploymentName in @("travelmate-backend", "travelmate-frontend")) {
    $annotations = $missing.State.deployments.PSObject.Properties[$deploymentName].Value.annotations
    Assert-True (-not (Test-ObjectProperty -Object $annotations -Name "travelmate.io/commit")) "$deploymentName commit must be absent after rollback"
    Assert-True (-not (Test-ObjectProperty -Object $annotations -Name "travelmate.io/image-digest")) "$deploymentName digest must be absent after rollback"
}
Assert-True ($missing.Calls -match [regex]::Escape("travelmate.io/commit-")) "rollback must remove originally absent commit annotations"
Assert-True ($missing.Calls -match [regex]::Escape("travelmate.io/image-digest-")) "rollback must remove originally absent digest annotations"

$partial = Invoke-RollbackScenario -Name "partial image update" -FailureStage frontend-set
Assert-True ($partial.ExitCode -ne 0) "partial update failure must exit non-zero"
Assert-True ($partial.State.deployments."travelmate-backend".image -eq $oldBackendImage) "backend must be restored after frontend update fails"
Assert-True ($partial.State.deployments."travelmate-frontend".image -eq $oldFrontendImage) "frontend must retain its original image after its update fails"
Assert-True ($partial.DeployLog -notmatch "deployed successfully|already deployed and healthy") "partial update failure must not be logged as successful"

$rollbackFailure = Invoke-RollbackScenario -Name "rollback command failure" -AnnotationsMissing -FailFrontendDigestRemoval
Assert-True ($rollbackFailure.ExitCode -ne 0) "rollback command failure must exit non-zero"
Assert-True ($rollbackFailure.DeployLog -match "Rollback step failed: restore travelmate-frontend digest annotation") "rollback failure must identify the failed restoration step"
Assert-True ($rollbackFailure.DeployLog -match "Rollback step succeeded: wait for travelmate-frontend rollback") "rollback must continue after an individual restoration command fails"
Assert-True (-not (Test-ObjectProperty -Object $rollbackFailure.State.deployments."travelmate-frontend".annotations -Name "travelmate.io/commit")) "later rollback steps must still run after a restoration failure"

Write-Output "PASS: Deploy-TravelMate rollback behavior restored images and annotations across 4 deterministic fake-kubectl scenarios."
