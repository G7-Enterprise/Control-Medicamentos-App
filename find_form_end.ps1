$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
$depth = 0

# Find MedicamentoForm start (around line 1730)
$formStart = -1
for ($i = 1720; $i -lt 1740; $i++) {
    if ($lines[$i] -match 'fun MedicamentoForm\(') {
        $formStart = $i
        Write-Host "MedicamentoForm starts at line " + ($i+1)
        break
    }
}

# Calculate depth before formStart
for ($i = 0; $i -lt $formStart; $i++) {
    $line = $lines[$i]
    $opens = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closes = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    $depth += $opens - $closes
}
Write-Host "Depth at MedicamentoForm start: $depth (should be 1)"

# Now trace through MedicamentoForm to find where depth returns to 1
$formDepth = 1  # entry depth
for ($i = $formStart; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $opens = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closes = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    $depth += $opens - $closes
    if ($depth -le 1 -and $i -gt $formStart + 10) {
        Write-Host "MedicamentoForm ends (depth=$depth) at line " + ($i+1) + ": " + $line.Trim()
        break
    }
}
