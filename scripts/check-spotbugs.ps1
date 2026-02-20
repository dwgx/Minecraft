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

$rules = @(
    @{ Pattern = '\bThread\.stop\s*\('; Rule = 'forbidden-thread-stop' },
    @{ Pattern = '\bRuntime\.getRuntime\(\)\.halt\s*\('; Rule = 'forbidden-runtime-halt' },
    @{ Pattern = '\bSystem\.gc\s*\('; Rule = 'forbidden-system-gc' },
    @{ Pattern = '\bClass\.forName\s*\('; Rule = 'reflection-entrypoint-review' }
)

$issues = New-Object System.Collections.Generic.List[object]

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

        foreach ($rule in $rules) {
            Select-String -Path $path -Pattern $rule.Pattern | ForEach-Object {
                $issues.Add([PSCustomObject]@{
                    Path = $rel
                    Line = $_.LineNumber
                    Rule = $rule.Rule
                    Code = $_.Line.Trim()
                }) | Out-Null
            }
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Output "[OK] SpotBugs-lite checks passed."
    exit 0
}

Write-Output ("[FAIL] SpotBugs-lite found {0} issue(s)." -f $issues.Count)
$issues | Sort-Object Path, Line, Rule | ForEach-Object {
    Write-Output (" - [{0}] {1}:{2} :: {3}" -f $_.Rule, $_.Path, $_.Line, $_.Code)
}

exit 1
