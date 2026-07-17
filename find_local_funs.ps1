$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
for ($i = 2643; $i -lt 3180; $i++) {
    if ($lines[$i] -match '^\s+fun [a-zA-Z]') {
        Write-Host ($i+1).ToString() + ': ' + $lines[$i].Trim()
    }
}
