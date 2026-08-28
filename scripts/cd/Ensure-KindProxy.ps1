[CmdletBinding()]
param(
    [string]$NodeContainer = "desktop-control-plane"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$forwarder = Join-Path $PSScriptRoot "kind-proxy-forwarder.py"
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker is not available"
}

function Test-NodeTcpConnection {
    param(
        [string]$TargetHost,
        [int]$Port,
        [int]$TimeoutSeconds = 3
    )

    $previousErrorActionPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        $probe = "timeout $TimeoutSeconds bash -c '</dev/tcp/$TargetHost/$Port'"
        & docker exec $NodeContainer bash -lc $probe 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
}

function Wait-NodeTcpConnection {
    param(
        [string]$TargetHost,
        [int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if (Test-NodeTcpConnection -TargetHost $TargetHost -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    return $false
}

$nodeEnvironment = & docker inspect --format "{{range .Config.Env}}{{println .}}{{end}}" $NodeContainer 2>$null
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop Kind node '$NodeContainer' is not available"
}

$loopbackProxy = @($nodeEnvironment | Where-Object { $_ -match '^HTTPS?_PROXY=http://127\.0\.0\.1:(\d+)/?$' }) |
    Select-Object -First 1
if (-not $loopbackProxy) {
    return
}

$proxyPort = [int]([regex]::Match($loopbackProxy, ':(\d+)/?$').Groups[1].Value)
if (Test-NodeTcpConnection -TargetHost "127.0.0.1" -Port $proxyPort) {
    return
}

if (-not (Wait-NodeTcpConnection -TargetHost "http.docker.internal" -Port 3128)) {
    throw "Docker Desktop internal proxy is not reachable from the Kind node"
}

$previousErrorActionPreference = $ErrorActionPreference
$iptablesAvailable = $false
try {
    $ErrorActionPreference = "Continue"
    & docker exec $NodeContainer bash -lc "command -v iptables >/dev/null 2>&1" | Out-Null
    $iptablesAvailable = $LASTEXITCODE -eq 0
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

if ($iptablesAvailable) {
    $addressOutput = @(& docker exec $NodeContainer getent ahostsv4 http.docker.internal)
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to resolve Docker Desktop's internal proxy address"
    }
    $addressMatch = [regex]::Match(($addressOutput -join "`n"), '(?m)^\s*(\d+\.\d+\.\d+\.\d+)')
    if (-not $addressMatch.Success) {
        throw "Docker Desktop's internal proxy did not resolve to an IPv4 address"
    }
    $targetAddress = $addressMatch.Groups[1].Value
    $ruleArguments = @(
        "OUTPUT", "-p", "tcp", "-d", "127.0.0.1",
        "--dport", "$proxyPort", "-j", "DNAT", "--to-destination", "${targetAddress}:3128"
    )

    $ruleExists = $false
    try {
        $ErrorActionPreference = "Continue"
        & docker exec $NodeContainer iptables -t nat -C @ruleArguments 2>$null | Out-Null
        $ruleExists = $LASTEXITCODE -eq 0
    }
    finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if (-not $ruleExists) {
        & docker exec $NodeContainer iptables -t nat -A @ruleArguments | Out-Null
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to add the Kind node proxy forwarding rule"
        }
    }

    if (-not (Wait-NodeTcpConnection -TargetHost "127.0.0.1" -Port $proxyPort)) {
        throw "Kind proxy forwarding rule did not become ready"
    }
    Write-Output "Repaired the Docker Desktop Kind node's loopback proxy with an iptables rule."
    return
}

if (-not (Test-Path -LiteralPath $forwarder -PathType Leaf)) {
    throw "Kind proxy forwarder is missing: $forwarder"
}
& docker exec $NodeContainer bash -lc "command -v python3 >/dev/null 2>&1" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Kind proxy repair requires either iptables or python3 in the node"
}

& docker cp $forwarder "${NodeContainer}:/usr/local/bin/travelmate-kind-proxy-forwarder.py" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw "Unable to copy the Kind proxy forwarder"
}
& docker exec -d $NodeContainer python3 /usr/local/bin/travelmate-kind-proxy-forwarder.py $proxyPort
if ($LASTEXITCODE -ne 0) {
    throw "Unable to start the Kind proxy forwarder"
}

Start-Sleep -Milliseconds 500
if (-not (Wait-NodeTcpConnection -TargetHost "127.0.0.1" -Port $proxyPort)) {
    throw "Kind proxy forwarder did not become ready"
}

Write-Output "Repaired the Docker Desktop Kind node's loopback proxy forwarding."
