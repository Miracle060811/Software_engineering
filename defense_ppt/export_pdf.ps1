$ErrorActionPreference = 'Stop'
$src = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\export\TravelMate云原生改造-最终答辩-第5组_已修改.pptx"
$outDir = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$pdfPath = Join-Path $outDir "modified.pdf"
$app = New-Object -ComObject PowerPoint.Application
try {
    $pres = $app.Presentations.Open($src, $true, $false, $false)
    # 32 = ppSaveAsPDF
    $pres.SaveAs($pdfPath, 32)
    Write-Host "PDF_EXPORT_OK: $pdfPath"
    $pres.Close()
} catch {
    Write-Host "PDF_EXPORT_FAIL: $($_.Exception.Message)"
} finally {
    $app.Quit()
}
