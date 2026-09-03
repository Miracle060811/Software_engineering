$ErrorActionPreference = 'SilentlyContinue'
try {
    $app = New-Object -ComObject PowerPoint.Application
    Write-Host "PPT_COM_OK version=$($app.Version)"
    $app.Quit()
} catch {
    Write-Host "PPT_COM_FAIL: $($_.Exception.Message)"
}
