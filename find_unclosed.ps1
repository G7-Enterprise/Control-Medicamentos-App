$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
$depth = 0
$prevDepth = 0
for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $opens = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closes = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    $prevDepth = $depth
    $depth += $opens - $closes
    # Find where depth hits 2 from below 2 (rising to 2 after being at 1)
    if ($prevDepth -le 1 -and $depth -eq 2 -and $i -gt 1000) {
        Write-Host "Depth rises to 2 at line " + ($i+1) + ": " + $line.Substring(0, [Math]::Min(80,$line.Length))
    }
    # Also find all @Composable or fun lines where depth >= 2
    if ($depth -ge 2 -and ($line -match '^\s*@Composable' -or $line -match '^\s*(private|internal|public)?\s*fun ') -and $i -gt 8000) {
        Write-Host ($i+1).ToString().PadLeft(5) + " d=$depth " + $line.Trim().Substring(0, [Math]::Min(70,$line.Trim().Length))
    }
}
Write-Host "Final depth: $depth"
