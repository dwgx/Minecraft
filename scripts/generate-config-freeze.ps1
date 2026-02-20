param(
    [string]$Root = (Resolve-Path ".").Path,
    [string]$Output = "docs/config-identity-freeze.generated.md"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
$srcPath = Join-Path $rootPath "src"
$moduleImplPath = Join-Path $srcPath "client/module/impl"
$hudPath = Join-Path $srcPath "client/hud"
$outputPath = Join-Path $rootPath $Output

function Read-AllText([string]$filePath) {
    return [System.IO.File]::ReadAllText($filePath)
}

function Rel([string]$path) {
    if ($path.StartsWith($rootPath, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $path.Substring($rootPath.Length + 1)
    }
    return $path
}

$moduleRows = New-Object System.Collections.Generic.List[object]
Get-ChildItem -LiteralPath $moduleImplPath -Recurse -File -Filter *.java | ForEach-Object {
    $text = Read-AllText $_.FullName
    $moduleMatch = [regex]::Match($text, 'super\("(?<id>[a-z0-9_]+)",\s*"(?<name>[^"]+)",\s*Category\.(?<cat>[A-Z_]+)\)')
    if (-not $moduleMatch.Success) {
        return
    }

    $keys = New-Object System.Collections.Generic.List[string]
    [regex]::Matches($text, 'new\s+[A-Za-z0-9_]+Setting(?:<[^>]+>)?\("(?<key>[a-z0-9_]+)"') | ForEach-Object {
        $k = $_.Groups["key"].Value
        if (-not [string]::IsNullOrWhiteSpace($k) -and -not $keys.Contains($k)) {
            $keys.Add($k) | Out-Null
        }
    }

    $moduleRows.Add([PSCustomObject]@{
        Id = $moduleMatch.Groups["id"].Value
        Name = $moduleMatch.Groups["name"].Value
        Category = $moduleMatch.Groups["cat"].Value
        Source = Rel $_.FullName
        Keys = [string]::Join(", ", $keys)
    }) | Out-Null
}

$hudRows = New-Object System.Collections.Generic.List[object]
Get-ChildItem -LiteralPath $hudPath -Recurse -File -Filter *.java | ForEach-Object {
    $text = Read-AllText $_.FullName
    $idMatch = [regex]::Match($text, 'super\("(?<id>[a-z0-9_]+)",\s*"(?<name>[^"]+)"')
    if (-not $idMatch.Success) {
        return
    }

    $keys = New-Object System.Collections.Generic.List[string]
    [regex]::Matches($text, 'new\s+[A-Za-z0-9_]+Setting(?:<[^>]+>)?\("(?<key>[a-z0-9_]+)"') | ForEach-Object {
        $k = $_.Groups["key"].Value
        if (-not [string]::IsNullOrWhiteSpace($k) -and -not $keys.Contains($k)) {
            $keys.Add($k) | Out-Null
        }
    }

    $hudRows.Add([PSCustomObject]@{
        Id = $idMatch.Groups["id"].Value
        Name = $idMatch.Groups["name"].Value
        Source = Rel $_.FullName
        Keys = [string]::Join(", ", $keys)
    }) | Out-Null
}

$enumRows = New-Object System.Collections.Generic.List[object]
$enumScanRoots = @(
    $moduleImplPath,
    $hudPath,
    (Join-Path $srcPath "dwgx/scaffold")
)
foreach ($enumRoot in $enumScanRoots) {
    if (-not (Test-Path -LiteralPath $enumRoot)) {
        continue
    }

    Get-ChildItem -LiteralPath $enumRoot -Recurse -File -Filter *.java | ForEach-Object {
        $source = Rel $_.FullName
        $text = Read-AllText $_.FullName
        [regex]::Matches($text, 'enum\s+(?<name>[A-Za-z0-9_]+)\s*\{') | ForEach-Object {
            $enumRows.Add([PSCustomObject]@{
                Enum = $_.Groups["name"].Value
                Source = $source
            }) | Out-Null
        }
    }
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Config Identity Freeze Snapshot") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("Generated at: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("## Modules") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("| id | name | category | source | setting keys |") | Out-Null
$lines.Add("| --- | --- | --- | --- | --- |") | Out-Null
$moduleRows | Sort-Object Id | ForEach-Object {
    $src = [string]$_.Source
    $lines.Add("| $($_.Id) | $($_.Name) | $($_.Category) | `"$src`" | $($_.Keys) |") | Out-Null
}
$lines.Add("") | Out-Null
$lines.Add("## HUD Elements") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("| id | name | source | setting keys |") | Out-Null
$lines.Add("| --- | --- | --- | --- |") | Out-Null
$hudRows | Sort-Object Id | ForEach-Object {
    $src = [string]$_.Source
    $lines.Add("| $($_.Id) | $($_.Name) | `"$src`" | $($_.Keys) |") | Out-Null
}
$lines.Add("") | Out-Null
$lines.Add("## Enum Sources (persisted names must stay stable when serialized)") | Out-Null
$lines.Add("") | Out-Null
$lines.Add("| enum | source |") | Out-Null
$lines.Add("| --- | --- |") | Out-Null
$enumRows | Sort-Object Enum, Source | ForEach-Object {
    $src = [string]$_.Source
    $lines.Add("| $($_.Enum) | `"$src`" |") | Out-Null
}

$dir = Split-Path -Parent $outputPath
if (-not (Test-Path -LiteralPath $dir)) {
    New-Item -ItemType Directory -Path $dir | Out-Null
}

$lines | Set-Content -Path $outputPath -Encoding UTF8
Write-Output "[OK] Generated $Output"
