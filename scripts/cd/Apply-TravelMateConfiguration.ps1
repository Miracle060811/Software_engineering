[CmdletBinding()]
param(
    [ValidateSet("local", "server")]
    [string]$Environment = "local",
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$overlay = Join-Path $repositoryRoot "deploy\k8s-overlays\$Environment"
if (-not (Test-Path -LiteralPath (Join-Path $overlay "kustomization.yaml") -PathType Leaf)) {
    throw "Kubernetes overlay does not exist: $overlay"
}

$rendered = & kubectl kustomize $overlay
if ($LASTEXITCODE -ne 0) {
    throw "Unable to render Kubernetes configuration"
}
$configBytes = [Security.Cryptography.SHA256]::HashData(
    [Text.Encoding]::UTF8.GetBytes(($rendered -join "`n")))
$configHash = [Convert]::ToHexString($configBytes).ToLowerInvariant()

& kubectl --context $KubeContext apply -k $overlay
if ($LASTEXITCODE -ne 0) {
    throw "Unable to apply Kubernetes configuration"
}
& kubectl --context $KubeContext annotate deployment/travelmate-backend `
    -n $Namespace --overwrite "travelmate.io/config-hash=$configHash"
if ($LASTEXITCODE -ne 0) {
    throw "Unable to annotate backend deployment with the configuration hash"
}
& kubectl --context $KubeContext rollout status deployment/travelmate-backend -n $Namespace --timeout=360s
if ($LASTEXITCODE -ne 0) {
    throw "Backend rollout did not complete"
}

Write-Output "Applied '$Environment' configuration and rolled out hash $configHash."
