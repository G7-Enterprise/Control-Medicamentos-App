$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)

# Find MedicamentoForm start
$formStart = -1
for ($i = 1720; $i -lt 1740; $i++) {
    if ($lines[$i] -match 'fun MedicamentoForm\(') {
        $formStart = $i
        break
    }
}

# Count depth starting from the { opening of MedicamentoForm (line after params)
$depth = 0
$inForm = $false
for ($i = $formStart; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    # Count braces character by character to handle strings correctly
    foreach ($c in $line.ToCharArray()) {
        if ($c -eq '{') { $depth++ }
        elseif ($c -eq '}') { $depth-- }
    }
    if ($depth -eq 0 -and $i -gt $formStart + 5) {
        Write-Host "MedicamentoForm closing } at line " + ($i+1) + ": " + $line.Trim().Substring(0, [Math]::Min(60, $line.Trim().Length))
        break
    }
}

# Check what follows
if ($i -lt $lines.Length - 1) {
    Write-Host "Next few lines after close:"
    for ($j = $i+1; $j -le $i+5; $j++) {
        Write-Host "  " + ($j+1) + ": " + $lines[$j]
    }
}
