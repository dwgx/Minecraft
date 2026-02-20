param(
    [string]$Root = (Resolve-Path ".").Path,
    [string]$VersionId = "Dwgx189",
    [string]$MainClass = "net.minecraft.client.main.Main",
    [string]$JavaHome = "",
    [switch]$InstallToMinecraft = $false,
    [string]$MinecraftDir = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-Command([string]$name) {
    if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $name"
    }
}

function Ensure-Dir([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path -Force | Out-Null
    }
}

$rootPath = (Resolve-Path $Root).Path

if ($JavaHome -and $JavaHome.Trim().Length -gt 0) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = (Join-Path $JavaHome "bin") + ";" + $env:Path
}

Assert-Command "javac"
Assert-Command "jar"

$srcDir = Join-Path $rootPath "src"
$libDir = Join-Path $rootPath "lib"
$resourcesDir = Join-Path $srcDir "resources"

if (-not (Test-Path -LiteralPath $srcDir)) {
    throw "Missing source directory: $srcDir"
}
if (-not (Test-Path -LiteralPath $libDir)) {
    throw "Missing lib directory: $libDir"
}

$outDir = Join-Path $rootPath "out"
$classesDir = Join-Path $outDir "classes"
$tmpDir = Join-Path $outDir "pcl2_tmp"
$fatDir = Join-Path $tmpDir "fat_classes"
$sourcesFile = Join-Path $tmpDir "sources.txt"
$manifestFile = Join-Path $tmpDir "MANIFEST.MF"

if (Test-Path -LiteralPath $tmpDir) {
    Remove-Item -LiteralPath $tmpDir -Recurse -Force
}
if (Test-Path -LiteralPath $classesDir) {
    Remove-Item -LiteralPath $classesDir -Recurse -Force
}

Ensure-Dir $classesDir
Ensure-Dir $tmpDir

Write-Output "[STEP] Collect Java sources"
Get-ChildItem -Path $srcDir -Recurse -Filter *.java | ForEach-Object { $_.FullName } | Set-Content -Path $sourcesFile -Encoding ASCII

Write-Output "[STEP] Compile (Java 8 target)"
Push-Location $rootPath
try {
    & javac --% -source 1.8 -target 1.8 -encoding UTF-8 -classpath lib/* -d out/classes @out/pcl2_tmp/sources.txt
} finally {
    Pop-Location
}
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

if (Test-Path -LiteralPath $resourcesDir) {
    Write-Output "[STEP] Copy resources"
    Copy-Item (Join-Path $resourcesDir "*") $classesDir -Recurse -Force
}

Write-Output "[STEP] Build fat class tree"
Copy-Item $classesDir $fatDir -Recurse -Force
$libJars = Get-ChildItem -Path $libDir -Filter *.jar | Sort-Object Name

if ($libJars.Count -eq 0) {
    throw "No jars found in $libDir"
}

Push-Location $fatDir
try {
    foreach ($jarFile in $libJars) {
        & jar xf $jarFile.FullName
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to extract: $($jarFile.FullName)"
        }
    }
} finally {
    Pop-Location
}

# Remove signature files that can break class loading after repack.
Get-ChildItem -Path (Join-Path $fatDir "META-INF") -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Extension -in @(".SF", ".RSA", ".DSA") } |
    Remove-Item -Force -ErrorAction SilentlyContinue

Set-Content -Path $manifestFile -Encoding ASCII -Value @(
    "Manifest-Version: 1.0"
    "Main-Class: $MainClass"
    ""
)

$distVersionDir = Join-Path $rootPath ("dist\pcl2\" + $VersionId)
Ensure-Dir $distVersionDir

$jarPath = Join-Path $distVersionDir ($VersionId + ".jar")
$jsonPath = Join-Path $distVersionDir ($VersionId + ".json")

if (Test-Path -LiteralPath $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}

Write-Output "[STEP] Pack jar: $jarPath"
Push-Location $rootPath
try {
    & jar cfm $jarPath $manifestFile -C $fatDir .
} finally {
    Pop-Location
}
if ($LASTEXITCODE -ne 0) {
    throw "jar pack failed with exit code $LASTEXITCODE"
}

$nowUtc = [DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ssZ")
$json = @"
{
  "id": "$VersionId",
  "inheritsFrom": "1.8.9",
  "jar": "$VersionId",
  "type": "release",
  "time": "$nowUtc",
  "releaseTime": "$nowUtc",
  "mainClass": "$MainClass"
}
"@
Set-Content -Path $jsonPath -Encoding UTF8 -Value $json

Write-Output "[STEP] Verify jar contains patched classes"
$jarListing = & jar tf $jarPath
$required = @(
    "net/minecraft/client/main/Main.class",
    "org/lwjgl/Sys.class",
    "org/lwjgl/opengl/Display.class"
)
foreach ($entry in $required) {
    if (-not ($jarListing -contains $entry)) {
        throw "Missing class in packaged jar: $entry"
    }
}

if ($InstallToMinecraft) {
    if (-not $MinecraftDir -or $MinecraftDir.Trim().Length -eq 0) {
        $MinecraftDir = Join-Path $env:APPDATA ".minecraft"
    }
    $targetVersionDir = Join-Path $MinecraftDir ("versions\" + $VersionId)
    Ensure-Dir $targetVersionDir
    Copy-Item $jarPath (Join-Path $targetVersionDir ($VersionId + ".jar")) -Force
    Copy-Item $jsonPath (Join-Path $targetVersionDir ($VersionId + ".json")) -Force
    Write-Output ("[OK] Installed to " + $targetVersionDir)
}

Write-Output "[OK] PCL2 package created."
Write-Output ("     jar : " + $jarPath)
Write-Output ("     json: " + $jsonPath)
Write-Output "     JVM args for PCL2: -Djava.library.path=D:\Project\Minecraft\natives"
