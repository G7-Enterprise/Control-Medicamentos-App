$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)

# Lines 1730-1939 (0-indexed: 1729-1938) are the remember block to replace
$startRemove = 1729  # 0-indexed line 1730
$endRemove = 1938    # 0-indexed line 1939

$newLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($i -eq $startRemove) {
        $newLines.Add('    val s = rememberMedicamentoFormState()')
    } elseif ($i -gt $startRemove -and $i -le $endRemove) {
        # skip - all remember declarations removed
    } else {
        $newLines.Add($lines[$i])
    }
}

[System.IO.File]::WriteAllLines($f, $newLines, [System.Text.Encoding]::UTF8)
Write-Host "Done. File now has $($newLines.Count) lines."
