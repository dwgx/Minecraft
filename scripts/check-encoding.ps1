param(
    [string]$Root = (Resolve-Path ".").Path,
    [string[]]$Targets = @("src", "docs", "README.md", "AGENTS.md", ".gitignore"),
    [string[]]$ExcludeDirs = @(".git", ".idea", "out", "build", "target", "run", "natives", "lib"),
    [string[]]$IncludeExtensions = @(
        ".java", ".json", ".md", ".txt", ".xml", ".properties",
        ".yml", ".yaml", ".cfg", ".ini", ".iml", ".ps1", ".bat", ".cmd"
    ),
    [string[]]$IncludeFileNames = @(".gitignore", "AGENTS.md")
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
$utf8Strict = New-Object System.Text.UTF8Encoding($false, $true)
$replacementChar = [char]0xFFFD

$scanCount = 0
$issues = New-Object System.Collections.Generic.List[object]

function Get-RelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )

    $baseTrimmed = $BasePath.TrimEnd("\", "/")
    if ($FullPath.StartsWith($baseTrimmed, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $FullPath.Substring($baseTrimmed.Length).TrimStart("\", "/")
    }

    return $FullPath
}

function Is-ExcludedPath {
    param(
        [string]$PathValue,
        [string[]]$DirectoryNames
    )

    foreach ($name in $DirectoryNames) {
        $pattern = "(^|[\\/])" + [regex]::Escape($name) + "([\\/]|$)"
        if ($PathValue -match $pattern) {
            return $true
        }
    }

    return $false
}

function Should-ScanFile {
    param(
        [string]$FilePath,
        [string[]]$Extensions,
        [string[]]$FileNames
    )

    $leafName = [System.IO.Path]::GetFileName($FilePath)
    if ($FileNames -contains $leafName) {
        return $true
    }

    $extension = [System.IO.Path]::GetExtension($FilePath).ToLowerInvariant()
    return $Extensions -contains $extension
}

function Add-Issue {
    param(
        [string]$PathValue,
        [string]$Type,
        [string]$Detail
    )

    $issues.Add([PSCustomObject]@{
        Path = $PathValue
        Type = $Type
        Detail = $Detail
    }) | Out-Null
}

function Check-FileEncoding {
    param(
        [string]$FilePath
    )

    $script:scanCount++
    $relativePath = Get-RelativePath -BasePath $rootPath -FullPath $FilePath

    [byte[]]$bytes = [System.IO.File]::ReadAllBytes($FilePath)
    $decoded = $null

    try {
        $decoded = $utf8Strict.GetString($bytes)
    } catch {
        Add-Issue -PathValue $relativePath -Type "InvalidUTF8" -Detail $_.Exception.Message
        return
    }

    $replacementCount = 0
    for ($index = 0; $index -lt $decoded.Length; $index++) {
        if ($decoded[$index] -eq $replacementChar) {
            $replacementCount++
        }
    }

    if ($replacementCount -gt 0) {
        Add-Issue -PathValue $relativePath -Type "ReplacementChar" -Detail ("U+FFFD count: {0}" -f $replacementCount)
    }
}

foreach ($target in $Targets) {
    $resolvedTarget = Join-Path $rootPath $target
    if (-not (Test-Path -LiteralPath $resolvedTarget)) {
        continue
    }

    $item = Get-Item -LiteralPath $resolvedTarget
    if ($item.PSIsContainer) {
        Get-ChildItem -LiteralPath $resolvedTarget -Recurse -File | ForEach-Object {
            $filePath = $_.FullName
            if (Is-ExcludedPath -PathValue $filePath -DirectoryNames $ExcludeDirs) {
                return
            }

            if (-not (Should-ScanFile -FilePath $filePath -Extensions $IncludeExtensions -FileNames $IncludeFileNames)) {
                return
            }

            Check-FileEncoding -FilePath $filePath
        }
    } else {
        $filePath = $item.FullName
        if (Is-ExcludedPath -PathValue $filePath -DirectoryNames $ExcludeDirs) {
            continue
        }

        if (-not (Should-ScanFile -FilePath $filePath -Extensions $IncludeExtensions -FileNames $IncludeFileNames)) {
            continue
        }

        Check-FileEncoding -FilePath $filePath
    }
}

if ($issues.Count -eq 0) {
    Write-Output ("[OK] Scanned {0} files. No UTF-8/ReplacementChar issues found." -f $scanCount)
    exit 0
}

Write-Output ("[FAIL] Scanned {0} files. Found {1} encoding issue(s)." -f $scanCount, $issues.Count)
$issues | Sort-Object Path, Type | ForEach-Object {
    Write-Output (" - [{0}] {1}: {2}" -f $_.Type, $_.Path, $_.Detail)
}

exit 1
