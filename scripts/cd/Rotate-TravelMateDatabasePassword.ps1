[CmdletBinding()]
param(
    [string]$Namespace = "travelmate",
    [string]$KubeContext = "docker-desktop",
    [switch]$Generate
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function New-RandomPassword {
    $bytes = [Security.Cryptography.RandomNumberGenerator]::GetBytes(36)
    return [Convert]::ToBase64String($bytes).Replace("+", "A").Replace("/", "b")
}

function ConvertTo-PlainText {
    param([Security.SecureString]$Value)
    $pointer = [IntPtr]::Zero
    try {
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        if ($pointer -ne [IntPtr]::Zero) {
            [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
        }
    }
}

function Invoke-MySqlPasswordChange {
    param(
        [string]$Pod,
        [string]$Password
    )
    $remoteCommand = "mysql --protocol=socket -uroot -p`"`$MYSQL_ROOT_PASSWORD`" -e `"ALTER USER 'travelmate'@'%' IDENTIFIED BY '$Password';`""
    & kubectl --context $KubeContext exec -n $Namespace $Pod -- sh -lc $remoteCommand
    if ($LASTEXITCODE -ne 0) { throw "Unable to change the MySQL application-user password" }
}

function Test-MySqlPassword {
    param(
        [string]$Pod,
        [string]$Password
    )
    if ($Password.Contains("'")) { throw "Database passwords cannot contain a single quote" }
    $remoteCommand = "MYSQL_PWD='$Password' mysql --protocol=tcp -h127.0.0.1 -utravelmate -Nse 'SELECT 1' travelmate"
    $result = & kubectl --context $KubeContext exec -n $Namespace $Pod -- sh -lc $remoteCommand
    return ($LASTEXITCODE -eq 0 -and ($result -join "").Trim() -eq "1")
}

$secretJson = & kubectl --context $KubeContext get secret travelmate-secrets -n $Namespace -o json | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw "Unable to read travelmate-secrets" }
$oldEncoded = $secretJson.data.'mysql-password'
if (-not $oldEncoded) { throw "mysql-password is missing from travelmate-secrets" }
$oldPassword = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($oldEncoded))

$newPassword = if ($Generate) {
    New-RandomPassword
}
else {
    $first = Read-Host "Input the new TravelMate MySQL password" -AsSecureString
    $second = Read-Host "Input it again" -AsSecureString
    $firstText = ConvertTo-PlainText $first
    $secondText = ConvertTo-PlainText $second
    if ($firstText -cne $secondText) { throw "The two password values do not match" }
    $firstText
}
if ($newPassword.Length -lt 24) { throw "The new database password must contain at least 24 characters" }
if ($newPassword -notmatch '^[A-Za-z0-9._~!@#%^*+=:-]+$') {
    throw "The database password contains unsupported shell-sensitive characters"
}
if ($newPassword -ceq $oldPassword) { throw "The new database password must differ from the current password" }

$mysqlPod = (& kubectl --context $KubeContext get pods -n $Namespace `
    -l app.kubernetes.io/name=travelmate-mysql `
    -o jsonpath='{.items[0].metadata.name}').Trim()
if ($LASTEXITCODE -ne 0 -or -not $mysqlPod) { throw "The MySQL pod is unavailable" }

$patchFile = Join-Path ([IO.Path]::GetTempPath()) ("travelmate-db-password-" + [guid]::NewGuid() + ".json")
$databaseChanged = $false
$secretChanged = $false
try {
    if (-not (Test-MySqlPassword -Pod $mysqlPod -Password $oldPassword)) {
        Invoke-MySqlPasswordChange -Pod $mysqlPod -Password $oldPassword
        if (-not (Test-MySqlPassword -Pod $mysqlPod -Password $oldPassword)) {
            throw "Unable to reconcile the database password with the current Kubernetes Secret"
        }
        Write-Output "Recovered the MySQL application-user password from the existing Kubernetes Secret without printing it."
    }

    Invoke-MySqlPasswordChange -Pod $mysqlPod -Password $newPassword
    $databaseChanged = $true
    if (-not (Test-MySqlPassword -Pod $mysqlPod -Password $newPassword)) {
        throw "The new database password failed verification"
    }

    $patch = @{ stringData = @{ "mysql-password" = $newPassword } } | ConvertTo-Json -Depth 4 -Compress
    [IO.File]::WriteAllText($patchFile, $patch, [Text.UTF8Encoding]::new($false))
    & kubectl --context $KubeContext patch secret travelmate-secrets -n $Namespace `
        --type=merge --patch-file=$patchFile | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Unable to update the Kubernetes Secret" }
    $secretChanged = $true

    & kubectl --context $KubeContext rollout restart deployment/travelmate-backend -n $Namespace | Out-Null
    & kubectl --context $KubeContext rollout status deployment/travelmate-backend -n $Namespace --timeout=360s
    if ($LASTEXITCODE -ne 0) { throw "Backend rollout failed after database password rotation" }
}
catch {
    if ($databaseChanged) {
        Invoke-MySqlPasswordChange -Pod $mysqlPod -Password $oldPassword
    }
    if ($secretChanged) {
        $rollbackPatch = @{ stringData = @{ "mysql-password" = $oldPassword } } | ConvertTo-Json -Depth 4 -Compress
        [IO.File]::WriteAllText($patchFile, $rollbackPatch, [Text.UTF8Encoding]::new($false))
        & kubectl --context $KubeContext patch secret travelmate-secrets -n $Namespace `
            --type=merge --patch-file=$patchFile | Out-Null
        & kubectl --context $KubeContext rollout restart deployment/travelmate-backend -n $Namespace | Out-Null
    }
    throw
}
finally {
    Remove-Item -LiteralPath $patchFile -Force -ErrorAction SilentlyContinue
    $newPassword = $null
    $oldPassword = $null
    $patch = $null
    $rollbackPatch = $null
}

Write-Output "Database password rotated, verified, stored in Kubernetes Secret, and rolled out without printing its value."
