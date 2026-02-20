param(
    [string]$Root = (Resolve-Path ".").Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
$scanRoots = @(
    (Join-Path $rootPath "src/client"),
    (Join-Path $rootPath "src/dwgx")
)

$issues = New-Object System.Collections.Generic.List[object]

function Add-Issue([string]$PathValue, [int]$Line, [string]$Rule, [string]$Code) {
    $issues.Add([PSCustomObject]@{
        Path = $PathValue
        Line = $Line
        Rule = $Rule
        Code = $Code.Trim()
    }) | Out-Null
}

function Rel([string]$fullPath) {
    if ($fullPath.StartsWith($rootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $fullPath.Substring($rootPath.Length + 1)
    }
    return $fullPath
}

foreach ($scanRoot in $scanRoots) {
    if (-not (Test-Path -LiteralPath $scanRoot)) {
        continue
    }

    Get-ChildItem -LiteralPath $scanRoot -Recurse -File -Filter *.java | ForEach-Object {
        $path = $_.FullName
        $rel = Rel $path
        $lines = Get-Content -Path $path

        for ($i = 0; $i -lt $lines.Length; $i++) {
            $lineNo = $i + 1
            $line = $lines[$i]

            if ($line -match '^\s*import\s+[^;]+\.\*\s*;') {
                $isLegacyNanoStatic = $line -match '^\s*import\s+static\s+org\.lwjgl\.nanovg\.NanoVG\.\*\s*;'
                if (-not $isLegacyNanoStatic) {
                    Add-Issue -PathValue $rel -Line $lineNo -Rule "avoid-star-import" -Code $line
                }
            }

            if ($line -match '^\s*catch\s*\([^)]*\)\s*\{\s*\}$') {
                Add-Issue -PathValue $rel -Line $lineNo -Rule "empty-catch-block" -Code $line
            }
        }

        $joined = [string]::Join("`n", $lines)
        [regex]::Matches($joined, '(?m)^\s*public\s+(?:final\s+)?(?:class|interface|enum)\s+([A-Za-z0-9_]+)') | ForEach-Object {
            $name = $_.Groups[1].Value
            if ($name.Length -gt 0 -and -not [char]::IsUpper($name[0])) {
                $lineNo = ($joined.Substring(0, $_.Index) -split "`n").Length
                Add-Issue -PathValue $rel -Line $lineNo -Rule "type-name-pascal-case" -Code $_.Value
            }
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Output "[OK] Checkstyle-lite checks passed."
    exit 0
}

Write-Output ("[FAIL] Checkstyle-lite found {0} issue(s)." -f $issues.Count)
$issues | Sort-Object Path, Line, Rule | ForEach-Object {
    Write-Output (" - [{0}] {1}:{2} :: {3}" -f $_.Rule, $_.Path, $_.Line, $_.Code)
}

exit 1
