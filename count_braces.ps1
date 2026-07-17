$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
$depth = 0
$maxDepth = 0
$maxLine = 0
for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    $opens = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
    $closes = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
    $depth += $opens - $closes
    if ($depth -gt $maxDepth) { $maxDepth = $depth; $maxLine = $i+1 }
    # Report lines around 8300 where depth changes
    if ($i -ge 8290 -and $i -le 8320) {
        Write-Host ($i+1).ToString().PadLeft(5) + " depth=$depth  " + $line.Substring(0, [Math]::Min(60,$line.Length))
    }
}
Write-Host ""
Write-Host "Final depth: $depth (should be 0)"
Write-Host "Max depth at line: $maxLine ($maxDepth)"
