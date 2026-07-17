$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
for ($i = 1938; $i -lt 2650; $i++) {
    $line = $lines[$i].Trim()
    if ($line -match '^(val |var |LaunchedEffect|rememberLauncher|Disposable)') {
        Write-Host ($i+1).ToString() + ': ' + $line.Substring(0, [Math]::Min(80, $line.Length))
    }
}
