$f = 'D:\Controlmedicamentos\app\src\main\java\com\carlos\controlmedicamentos\MainActivity.kt'
$lines = [System.IO.File]::ReadAllLines($f)
$depth = 0
$stack = [System.Collections.Generic.Stack[object]]::new()

for ($i = 0; $i -lt $lines.Length; $i++) {
    $line = $lines[$i]
    foreach ($c in $line.ToCharArray()) {
        if ($c -eq '{') {
            $depth++
            $stack.Push(@{ line = ($i+1); depth = $depth; text = $line.Trim().Substring(0, [Math]::Min(60,$line.Trim().Length)) })
        }
        elseif ($c -eq '}') {
            if ($stack.Count -gt 0) { $stack.Pop() | Out-Null }
            $depth--
        }
    }
    # Report depth around line 8315
    if ($i -eq 8315) {
        Write-Host "Depth at line 8316: $depth"
        Write-Host "Unclosed blocks (innermost first):"
        $arr = $stack.ToArray()
        for ($j = 0; $j -lt [Math]::Min(5, $arr.Length); $j++) {
            Write-Host ("  Line " + $arr[$j].line + " (d=" + $arr[$j].depth + "): " + $arr[$j].text)
        }
        break
    }
}
