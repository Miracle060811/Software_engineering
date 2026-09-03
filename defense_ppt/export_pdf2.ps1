$ErrorActionPreference = 'Stop'
$src = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render\modified.pptx"
$pdfPath = "D:\Campus\grade2\26Spring\Software_engineering\defense_ppt\.preview\render\modified.pdf"
$app = New-Object -ComObject PowerPoint.Application
try {
    $pres = $app.Presentations.Open($src, $true, $false, $false)
    Write-Host "OPEN_OK slides=$($pres.Slides.Count)"
    $pres.SaveAs($pdfPath, 32)
    Write-Host "PDF_EXPORT_OK"
    $pres.Close()
} catch {
    Write-Host "FAIL: $($_.Exception.Message)"
} finally {
    $app.Quit()
}
