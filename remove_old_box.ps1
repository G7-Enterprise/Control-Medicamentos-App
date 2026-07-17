$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
# Remove lines 3669 to 4277 (0-indexed: 3668 to 4276)
$startRemove = 3668  # 0-indexed line 3669
$endRemove = 4276    # 0-indexed line 4277
$newLines = [System.Collections.Generic.List[string]]::new()
for ($i = 0; $i -lt $lines.Length; $i++) {
    if ($i -lt $startRemove -or $i -gt $endRemove) {
        $newLines.Add($lines[$i])
    }
}
[System.IO.File]::WriteAllLines($f, $newLines, [System.Text.Encoding]::UTF8)
Write-Host "Removed lines 3669-4277. File now has $($newLines.Count) lines."
