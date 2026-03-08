param(
    [string]$Root = (Resolve-Path ".").Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
$testRoot = Join-Path $rootPath "src/test"

if (-not (Test-Path -LiteralPath $testRoot)) {
    Write-Output "[OK] No src/test directory; smoke tests skipped."
    exit 0
}

$javaSources = @(Get-ChildItem -Path $testRoot -Recurse -Filter *.java -File)
if ($javaSources.Count -eq 0) {
    Write-Output "[OK] No smoke test sources; smoke tests skipped."
    exit 0
}

$tmpDir = Join-Path $rootPath ".codex_tmp"
if (-not (Test-Path -LiteralPath $tmpDir)) {
    New-Item -ItemType Directory -Path $tmpDir | Out-Null
}

$sourceList = Join-Path $tmpDir "test-sources.txt"
$javaSources | ForEach-Object { $_.FullName } | Set-Content -Path $sourceList -Encoding ASCII

$testOut = Join-Path $tmpDir "test-classes"
Remove-Item -Recurse -Force $testOut -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $testOut | Out-Null

Push-Location $rootPath
try {
    & javac --% -source 1.8 -target 1.8 -encoding UTF-8 -classpath out/classes;lib/* -d .codex_tmp/test-classes @.codex_tmp/test-sources.txt
} finally {
    Pop-Location
}

if ($LASTEXITCODE -ne 0) {
    Write-Output "[FAIL] Smoke test compilation failed."
    exit $LASTEXITCODE
}

$testClasses = New-Object System.Collections.Generic.List[string]
foreach ($source in $javaSources) {
    if ($source.Name -notlike "*Test.java") {
        continue
    }

    $relative = $source.FullName.Substring($testRoot.Length + 1)
    $className = ($relative -replace '\.java$', '') -replace '\\', '.'
    $testClasses.Add($className) | Out-Null
}

if ($testClasses.Count -eq 0) {
    Write-Output "[OK] No *Test.java smoke tests discovered."
    exit 0
}

foreach ($className in $testClasses) {
    Write-Output ("[RUN] {0}" -f $className)
    Push-Location $rootPath
    try {
        & java -cp ".codex_tmp/test-classes;out/classes;lib/*" $className
    } finally {
        Pop-Location
    }

    if ($LASTEXITCODE -ne 0) {
        Write-Output ("[FAIL] Smoke test failed: {0}" -f $className)
        exit $LASTEXITCODE
    }
}

Write-Output "[OK] Smoke tests passed."
exit 0
