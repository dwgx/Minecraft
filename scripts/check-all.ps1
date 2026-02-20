param(
    [string]$Root = (Resolve-Path ".").Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path

function Run-Step([string]$name, [scriptblock]$step) {
    Write-Output ("[STEP] {0}" -f $name)
    & $step
    if ($LASTEXITCODE -ne 0) {
        Write-Output ("[FAIL] {0}" -f $name)
        exit $LASTEXITCODE
    }
    Write-Output ("[OK] {0}" -f $name)
}

Run-Step "compile-javac-1.8" {
    $tmpDir = Join-Path $rootPath ".codex_tmp"
    if (-not (Test-Path -LiteralPath $tmpDir)) {
        New-Item -ItemType Directory -Path $tmpDir | Out-Null
    }

    $sources = Join-Path $tmpDir "sources.txt"
    Get-ChildItem -Path (Join-Path $rootPath "src") -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content -Path $sources -Encoding ASCII
    $outDir = Join-Path $rootPath "out/classes"
    if (-not (Test-Path -LiteralPath $outDir)) {
        New-Item -ItemType Directory -Path $outDir -Force | Out-Null
    }

    Push-Location $rootPath
    try {
        & javac --% -source 1.8 -target 1.8 -encoding UTF-8 -classpath lib/* -d out/classes @.codex_tmp/sources.txt
    } finally {
        Pop-Location
    }
}

Run-Step "check-encoding" { & powershell -ExecutionPolicy Bypass -File (Join-Path $rootPath "scripts/check-encoding.ps1") -Root $rootPath }
Run-Step "check-render-rules" { & powershell -ExecutionPolicy Bypass -File (Join-Path $rootPath "scripts/check-render-rules.ps1") -Root $rootPath }
Run-Step "check-checkstyle" { & powershell -ExecutionPolicy Bypass -File (Join-Path $rootPath "scripts/check-checkstyle.ps1") -Root $rootPath }
Run-Step "check-spotbugs" { & powershell -ExecutionPolicy Bypass -File (Join-Path $rootPath "scripts/check-spotbugs.ps1") -Root $rootPath }
Run-Step "check-refactor-guards" { & powershell -ExecutionPolicy Bypass -File (Join-Path $rootPath "scripts/check-refactor-guards.ps1") -Root $rootPath }

Write-Output "[OK] All checks passed."
exit 0

