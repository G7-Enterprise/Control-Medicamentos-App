$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
$patterns = @('CriticalAlertConfig','CriticalAlertSettings','MedicalAiSettings','MedicalAiConfig','ReportDraftSnapshot','MedicalAppointmentScheduler','FakeVademecumRepository','hoursToCycle','medicationToVademecum')
foreach ($p in $patterns) {
    $found = $false
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i] -match "^import.*$p" -or $lines[$i] -match "^(private |internal |public )?fun $p|^(private |internal |public )?class $p|^(private |internal |public )?data class $p|^(private |internal )?object $p") {
            Write-Host "$p => Line $($i+1): $($lines[$i].Trim())"
            $found = $true
            break
        }
    }
    if (-not $found) { Write-Host "$p => NOT FOUND AS DECLARATION/IMPORT" }
}
