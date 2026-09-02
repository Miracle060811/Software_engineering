[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$deployScript = (Resolve-Path (Join-Path $PSScriptRoot "..\Deploy-TravelMateMicroservices.ps1")).Path
$targetCommit = "b" * 40
$oldCommit = "c" * 40
$targetDigest = "sha256:" + ("a" * 64)
$oldDigest = "sha256:" + ("1" * 64)
$repository = "ghcr.io/miracle060811/travelmate-traffic-service"
$targetImage = "$repository@$targetDigest"
$oldImage = "$repository@$oldDigest"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw "Assertion failed: $Message"
    }
}

function Invoke-ServiceScenario {
    param(
        [Parameter(Mandatory = $true)][bool]$FailTargetRollout,
        [string]$SelectedService = "traffic-service"
    )

    $root = Join-Path ([IO.Path]::GetTempPath()) "travelmate-microservice-cd-test-$([guid]::NewGuid().ToString('N'))"
    $bin = Join-Path $root "bin"
    $evidenceDirectory = Join-Path $root "evidence"
    [IO.Directory]::CreateDirectory($bin) | Out-Null
    [IO.Directory]::CreateDirectory($evidenceDirectory) | Out-Null

    try {
        $statePath = Join-Path $root "state.json"
        $callLogPath = Join-Path $root "kubectl-calls.log"
        [ordered]@{
            failTargetRollout = $FailTargetRollout
            targetImage = $targetImage
            service = "traffic-service"
            image = $oldImage
            annotations = [ordered]@{
                "travelmate.io/commit" = $oldCommit
                "travelmate.io/image-digest" = $oldDigest
            }
        } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $statePath -Encoding utf8

        $services = @(
            "identity-service", "traffic-service", "local-service",
            "ai-service", "community-service", "ops-service"
        )
        $images = foreach ($service in $services) {
            $imageName = "ghcr.io/miracle060811/travelmate-$service"
            if ($service -eq "traffic-service") {
                $imageName = $repository
            }
            [ordered]@{
                service = $service
                image = $imageName
                commit = $targetCommit
                tag = "sha-$targetCommit"
                digest = $targetDigest
            }
        }
        $releasePath = Join-Path $root "release.json"
        [ordered]@{
            commit = $targetCommit
            tag = "sha-$targetCommit"
            images = $images
        } | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $releasePath -Encoding utf8

        $kubectlFake = @'
$Arguments = [string[]]$args
$ErrorActionPreference = "Stop"
if ($Arguments.Count -ge 2 -and $Arguments[0] -eq "--context") {
    $Arguments = $Arguments[2..($Arguments.Count - 1)]
}
Add-Content -LiteralPath $env:TM_MICRO_CALL_LOG -Value ($Arguments -join " ") -Encoding utf8
$state = Get-Content -LiteralPath $env:TM_MICRO_STATE -Raw | ConvertFrom-Json

function Save-State {
    $state | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $env:TM_MICRO_STATE -Encoding utf8
}

if ($Arguments[0] -eq "get" -and $Arguments[1] -eq "deployment/traffic-service") {
    [ordered]@{
        metadata = [ordered]@{ annotations = $state.annotations }
        spec = [ordered]@{
            template = [ordered]@{
                spec = [ordered]@{
                    containers = @([ordered]@{ name = "traffic-service"; image = $state.image })
                }
            }
        }
    } | ConvertTo-Json -Depth 8 -Compress | Write-Output
    exit 0
}

if ($Arguments[0] -eq "set" -and $Arguments[1] -eq "image" -and $Arguments[2] -eq "deployment/traffic-service") {
    $state.image = ($Arguments[3] -split "=", 2)[1]
    Save-State
    Write-Output "image updated"
    exit 0
}

if ($Arguments[0] -eq "annotate" -and $Arguments[1] -eq "deployment/traffic-service") {
    foreach ($argument in $Arguments) {
        if ($argument -match '^(travelmate\.io/[^=]+)=(.*)$') {
            $state.annotations | Add-Member -NotePropertyName $Matches[1] -NotePropertyValue $Matches[2] -Force
        }
        elseif ($argument -match '^(travelmate\.io/.+)-$') {
            $state.annotations.PSObject.Properties.Remove($Matches[1])
        }
    }
    Save-State
    Write-Output "annotations updated"
    exit 0
}

if ($Arguments[0] -eq "rollout" -and $Arguments[1] -eq "status" -and $Arguments[2] -eq "deployment/traffic-service") {
    if ([bool]$state.failTargetRollout -and [string]$state.image -eq [string]$state.targetImage) {
        Write-Error "simulated rollout timeout"
        exit 1
    }
    Write-Output "deployment successfully rolled out"
    exit 0
}

Write-Error "Unexpected kubectl arguments: $($Arguments -join ' ')"
exit 90
'@
        Set-Content -LiteralPath (Join-Path $bin "kubectl-fake.ps1") -Value $kubectlFake -Encoding utf8
        $runningOnWindows = [Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
            [Runtime.InteropServices.OSPlatform]::Windows
        )
        if ($runningOnWindows) {
            Set-Content -LiteralPath (Join-Path $bin "kubectl.cmd") -Encoding ascii -Value @(
                '@echo off',
                'pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0kubectl-fake.ps1" %*',
                'exit /b %errorlevel%'
            )
        }
        else {
            $shimPath = Join-Path $bin "kubectl"
            $shim = @'
#!/usr/bin/env bash
pwsh -NoLogo -NoProfile -File "$(dirname "$0")/kubectl-fake.ps1" "$@"
'@
            [IO.File]::WriteAllText($shimPath, $shim, [Text.UTF8Encoding]::new($false))
            & chmod +x $shimPath
            if ($LASTEXITCODE -ne 0) {
                throw "Unable to make fake kubectl executable"
            }
        }

        $oldPath = $env:PATH
        $oldState = $env:TM_MICRO_STATE
        $oldCallLog = $env:TM_MICRO_CALL_LOG
        try {
            $env:PATH = "$bin$([IO.Path]::PathSeparator)$oldPath"
            $env:TM_MICRO_STATE = $statePath
            $env:TM_MICRO_CALL_LOG = $callLogPath
            $output = @(& pwsh -NoLogo -NoProfile -ExecutionPolicy Bypass -File $deployScript `
                -ReleaseEvidencePath $releasePath `
                -ExpectedCommit $targetCommit `
                -Service $SelectedService `
                -EvidenceDirectory $evidenceDirectory `
                -RolloutTimeoutSeconds 1 2>&1)
            $exitCode = $LASTEXITCODE
        }
        finally {
            $env:PATH = $oldPath
            $env:TM_MICRO_STATE = $oldState
            $env:TM_MICRO_CALL_LOG = $oldCallLog
        }

        return [pscustomobject]@{
            ExitCode = $exitCode
            Output = ($output -join "`n")
            State = Get-Content -LiteralPath $statePath -Raw | ConvertFrom-Json
            Calls = if (Test-Path -LiteralPath $callLogPath) { Get-Content -LiteralPath $callLogPath -Raw } else { "" }
            EvidenceDirectory = $evidenceDirectory
            Root = $root
        }
    }
    catch {
        Remove-Item -LiteralPath $root -Recurse -Force -ErrorAction SilentlyContinue
        throw
    }
}

$success = Invoke-ServiceScenario -FailTargetRollout $false
try {
    Assert-True ($success.ExitCode -eq 0) "single-service deployment should succeed. Output: $($success.Output)"
    Assert-True ([string]$success.State.image -eq $targetImage) "target image should be updated"
    Assert-True ([string]$success.State.annotations."travelmate.io/commit" -eq $targetCommit) "commit annotation should be updated"
    Assert-True ($success.Calls -notmatch "identity-service|local-service|ai-service|community-service|ops-service") "other services must not be touched"
    $filteredRelease = Get-Content -LiteralPath (Join-Path $success.EvidenceDirectory "traffic-service-release.json") -Raw | ConvertFrom-Json
    Assert-True (@($filteredRelease.images).Count -eq 1) "evidence should contain only the selected service"
    Assert-True ([string]$filteredRelease.images[0].service -eq "traffic-service") "evidence service should match selection"
}
finally {
    Remove-Item -LiteralPath $success.Root -Recurse -Force -ErrorAction SilentlyContinue
}

$failure = Invoke-ServiceScenario -FailTargetRollout $true
try {
    Assert-True ($failure.ExitCode -ne 0) "rollout failure should exit non-zero"
    Assert-True ([string]$failure.State.image -eq $oldImage) "rollback should restore the old image. Output: $($failure.Output) Calls: $($failure.Calls) State: $($failure.State | ConvertTo-Json -Depth 5 -Compress)"
    Assert-True ([string]$failure.State.annotations."travelmate.io/commit" -eq $oldCommit) "rollback should restore the old commit annotation"
    Assert-True ([string]$failure.State.annotations."travelmate.io/image-digest" -eq $oldDigest) "rollback should restore the old digest annotation"
    Assert-True (Test-Path -LiteralPath (Join-Path $failure.EvidenceDirectory "traffic-service-rollback.json")) "rollback evidence should be written"
    Assert-True ($failure.Calls -notmatch "identity-service|local-service|ai-service|community-service|ops-service") "rollback must not touch other services"
}
finally {
    Remove-Item -LiteralPath $failure.Root -Recurse -Force -ErrorAction SilentlyContinue
}

$invalid = Invoke-ServiceScenario -FailTargetRollout $false -SelectedService "unknown-service"
try {
    Assert-True ($invalid.ExitCode -ne 0) "unsupported service should exit non-zero"
    Assert-True ($invalid.Output -match "Unsupported service 'unknown-service'") "unsupported service error should list the rejected value"
    Assert-True (-not $invalid.Calls) "unsupported service must be rejected before kubectl is invoked"
}
finally {
    Remove-Item -LiteralPath $invalid.Root -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Output "PASS: single-service deployment validates input, isolates traffic-service and restores it after a simulated rollout failure."
