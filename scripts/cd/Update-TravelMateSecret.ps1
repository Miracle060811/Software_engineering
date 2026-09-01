[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("deepseek-api-key", "jwt-secret", "admin-register-secret", "s3-access-key", "s3-secret-key")]
    [string]$Key,
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [switch]$SkipBackendRestart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$secretInput = Read-Host "Input new value for $Key" -AsSecureString
$bstr = [IntPtr]::Zero
$plain = $null
$patchFile = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-secret-" + [guid]::NewGuid() + ".json")

try {
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretInput)
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    if ([string]::IsNullOrWhiteSpace($plain)) {
        throw "Secret value cannot be empty"
    }
    if ($Key -eq "jwt-secret") {
        try {
            $decoded = [Convert]::FromBase64String($plain)
        }
        catch {
            throw "jwt-secret must be valid Base64"
        }
        if ($decoded.Length -lt 32) {
            throw "jwt-secret must decode to at least 32 bytes"
        }
    }
    elseif ($plain.Length -lt 16) {
        throw "$Key must contain at least 16 characters"
    }

    $patch = @{ stringData = @{ $Key = $plain } } | ConvertTo-Json -Depth 4 -Compress
    [IO.File]::WriteAllText($patchFile, $patch, [Text.UTF8Encoding]::new($false))
    & kubectl --context $KubeContext patch secret travelmate-secrets -n $Namespace `
        --type=merge --patch-file=$patchFile
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to update Kubernetes Secret"
    }
}
finally {
    if ($bstr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
    if ($null -ne $plain) {
        Remove-Variable plain -ErrorAction SilentlyContinue
    }
    Remove-Item -LiteralPath $patchFile -Force -ErrorAction SilentlyContinue
    Remove-Variable secretInput,bstr,patchFile -ErrorAction SilentlyContinue
}

if (-not $SkipBackendRestart) {
    & kubectl --context $KubeContext rollout restart deployment/travelmate-backend -n $Namespace
    if ($LASTEXITCODE -ne 0) {
        throw "Secret was updated, but backend restart failed"
    }
    & kubectl --context $KubeContext rollout status deployment/travelmate-backend -n $Namespace --timeout=360s
    if ($LASTEXITCODE -ne 0) {
        throw "Secret was updated, but backend rollout did not complete"
    }
}

Write-Output "Updated '$Key' without printing its value."
