$ErrorActionPreference = 'Stop'
$src = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\export\TravelMate云原生改造-最终答辩-第5组_已修改.pptx"
$outBase = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render\slides"
$app = New-Object -ComObject PowerPoint.Application
try {
    $pres = $app.Presentations.Open($src, $true, $false, $false)
    # 17 = ppSaveAsPNG
    $pres.SaveAs($outBase, 17)
    Write-Host "PNG_EXPORT_OK"
    $pres.Close()
} catch {
    Write-Host "PNG_EXPORT_FAIL: $($_.Exception.Message)"
} finally {
    $app.Quit()
}
