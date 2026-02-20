param(
    [string]$Root = (Resolve-Path ".").Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
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

$requiredFiles = @(
    "docs/refactor-contract.md",
    "docs/config-identity-freeze.md",
    "docs/net-org-patch-manifest.md",
    "config/checkstyle/checkstyle.xml",
    "config/spotbugs/exclude.xml",
    "scripts/check-checkstyle.ps1",
    "scripts/check-spotbugs.ps1",
    "scripts/check-all.ps1",
    "src/client/bridge/GameBridge.java",
    "src/client/bridge/PlayerBridge.java",
    "src/client/bridge/WorldBridge.java"
)

foreach ($file in $requiredFiles) {
    $path = Join-Path $rootPath $file
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Issue -PathValue $file -Line 1 -Rule "missing-required-file" -Code "required by refactor guard"
    }
}

$foundationPath = Join-Path $rootPath "src/dwgx/foundation"
if (Test-Path -LiteralPath $foundationPath) {
    Get-ChildItem -LiteralPath $foundationPath -Recurse -File -Filter *.java | ForEach-Object {
        $filePath = $_.FullName
        $rel = Rel $filePath
        Select-String -Path $filePath -Pattern '^\s*import\s+net\.minecraft\.' | ForEach-Object {
            Add-Issue -PathValue $rel -Line $_.LineNumber -Rule "foundation-no-direct-net-import" -Code $_.Line
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Output "[OK] Refactor guard checks passed."
    exit 0
}

Write-Output ("[FAIL] Refactor guards found {0} issue(s)." -f $issues.Count)
$issues | Sort-Object Path, Line, Rule | ForEach-Object {
    Write-Output (" - [{0}] {1}:{2} :: {3}" -f $_.Rule, $_.Path, $_.Line, $_.Code)
}

exit 1

