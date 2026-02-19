param(
    [string]$Root = (Resolve-Path ".").Path
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$rootPath = (Resolve-Path $Root).Path
$issues = New-Object System.Collections.Generic.List[object]

function Add-Issue {
    param(
        [string]$PathValue,
        [int]$LineNumber,
        [string]$Rule,
        [string]$LineText
    )

    $issues.Add([PSCustomObject]@{
        Path = $PathValue
        Line = $LineNumber
        Rule = $Rule
        Code = $LineText.Trim()
    }) | Out-Null
}

function To-RelativePath {
    param(
        [string]$BasePath,
        [string]$FullPath
    )

    $trimmedBase = $BasePath.TrimEnd("\", "/")
    if ($FullPath.StartsWith($trimmedBase, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $FullPath.Substring($trimmedBase.Length).TrimStart("\", "/")
    }

    return $FullPath
}

function Scan-Files {
    param(
        [string]$BaseDir,
        [string]$Pattern,
        [string]$RuleName
    )

    if (-not (Test-Path -LiteralPath $BaseDir)) {
        return
    }

    Get-ChildItem -LiteralPath $BaseDir -Recurse -File -Filter *.java | ForEach-Object {
        $path = $_.FullName
        Select-String -Path $path -Pattern $Pattern | ForEach-Object {
            Add-Issue -PathValue (To-RelativePath -BasePath $rootPath -FullPath $path) -LineNumber $_.LineNumber -Rule $RuleName -LineText $_.Line
        }
    }
}

$srcClient = Join-Path $rootPath "src/client"
$srcAll = Join-Path $rootPath "src"
$srcGui = Join-Path $rootPath "src/net/minecraft/client/gui"
$nanoOwner = (Join-Path $rootPath "src/net/minecraft/client/Minecraft.java")

# Client-side custom UI/modules must not touch swap loop or low-level clear/viewport.
Scan-Files -BaseDir $srcClient -Pattern '\bDisplay\.(?:update|sync|processMessages)\s*\(' -RuleName 'client-no-display-loop-calls'
Scan-Files -BaseDir $srcClient -Pattern '\bGL11\.glViewport\s*\(' -RuleName 'client-no-raw-viewport'
Scan-Files -BaseDir $srcClient -Pattern '\bGlStateManager\.viewport\s*\(' -RuleName 'client-no-viewport'
Scan-Files -BaseDir $srcClient -Pattern '\bGlStateManager\.clear\s*\(' -RuleName 'client-no-clear'

# GUI rendering should not rebind Minecraft's primary framebuffer mid-frame.
# Doing so can expose incomplete backbuffer content during window resize/restore.
Scan-Files -BaseDir $srcGui -Pattern '\bgetFramebuffer\(\)\.(?:unbindFramebuffer|bindFramebuffer)\s*\(' -RuleName 'gui-no-main-framebuffer-rebind'

# Nano runtime ownership must remain in Minecraft.
if (Test-Path -LiteralPath $srcAll) {
    Get-ChildItem -LiteralPath $srcAll -Recurse -File -Filter *.java | ForEach-Object {
        $path = $_.FullName
        $relative = To-RelativePath -BasePath $rootPath -FullPath $path

        Select-String -Path $path -Pattern '\bNanoRuntime\.(?:createContext|destroyContext)\s*\(' | ForEach-Object {
            if (-not $path.Equals($nanoOwner, [System.StringComparison]::OrdinalIgnoreCase)) {
                Add-Issue -PathValue $relative -LineNumber $_.LineNumber -Rule 'nano-runtime-owner-only-minecraft' -LineText $_.Line
            }
        }

        Select-String -Path $path -Pattern '\bnew\s+NanoVGContext\s*\(' | ForEach-Object {
            if (-not $path.Equals($nanoOwner, [System.StringComparison]::OrdinalIgnoreCase)) {
                Add-Issue -PathValue $relative -LineNumber $_.LineNumber -Rule 'nano-context-owner-only-minecraft' -LineText $_.Line
            }
        }
    }
}

if ($issues.Count -eq 0) {
    Write-Output "[OK] Render guard checks passed."
    exit 0
}

Write-Output ("[FAIL] Render guard checks found {0} issue(s)." -f $issues.Count)
$issues | Sort-Object Path, Line | ForEach-Object {
    Write-Output (" - [{0}] {1}:{2} :: {3}" -f $_.Rule, $_.Path, $_.Line, $_.Code)
}

exit 1
