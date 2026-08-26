[CmdletBinding()]
param(
    [string]$NodeContainer = "desktop-control-plane"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$forwarder = Join-Path $PSScriptRoot "kind-proxy-forwarder.py"
if (-not (Test-Path -LiteralPath $forwarder -PathType Leaf)) {
    throw "Kind proxy forwarder is missing: $forwarder"
}
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "docker is not available"
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
& docker exec $NodeContainer python3 -c `
    "import socket; connection=socket.create_connection(('127.0.0.1',$proxyPort),2); connection.close()" 2>$null
if ($LASTEXITCODE -eq 0) {
    return
}

& docker exec $NodeContainer python3 -c `
    "import socket; connection=socket.create_connection(('http.docker.internal',3128),5); connection.close()"
if ($LASTEXITCODE -ne 0) {
    throw "Docker Desktop internal proxy is not reachable from the Kind node"
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
& docker exec $NodeContainer python3 -c `
    "import socket; connection=socket.create_connection(('127.0.0.1',$proxyPort),2); connection.close()"
if ($LASTEXITCODE -ne 0) {
    throw "Kind proxy forwarder did not become ready"
}

Write-Output "Repaired the Docker Desktop Kind node's loopback proxy forwarding."
