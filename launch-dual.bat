@echo off
setlocal EnableExtensions

set "BASE=D:\Project\Minecraft"
set "JAVA_EXE=D:\Software\Dev\Zulu\zulu-8\bin\java.exe"
set "JAVAC_EXE=D:\Software\Dev\Zulu\zulu-8\bin\javac.exe"

if not exist "%JAVA_EXE%" set "JAVA_EXE=java"
if not exist "%JAVAC_EXE%" set "JAVAC_EXE=javac"

set "NATIVES=%BASE%\natives"
set "OUT=%BASE%\out\classes"
set "CP=%OUT%;%BASE%\lib\*"
set "ASSETS=%BASE%\run\assets"
set "WORKDIR1=%BASE%\run"
set "WORKDIR2=%BASE%\run2"
set "TMP=%BASE%\.codex_tmp"
set "SOURCES=%TMP%\launch_sources.txt"

if not exist "%WORKDIR1%" mkdir "%WORKDIR1%"
if not exist "%WORKDIR2%" mkdir "%WORKDIR2%"
if not exist "%OUT%" mkdir "%OUT%"
if not exist "%TMP%" mkdir "%TMP%"

echo.
echo [Launcher] Compiling latest sources to out\classes ...
dir /b /s "%BASE%\src\*.java" > "%SOURCES%"
"%JAVAC_EXE%" -source 1.8 -target 1.8 -encoding UTF-8 -classpath "%BASE%\lib\*" -d "%OUT%" @"%SOURCES%"
if errorlevel 1 goto build_fail

if "%DRY_RUN%"=="1" (
    echo [Launcher] DRY_RUN=1, skipping game startup.
    goto done
)

echo.
echo [Launcher] Starting Instance 1...
start "MC-1" /D "%WORKDIR1%" "%JAVA_EXE%" -Djava.library.path="%NATIVES%" -Dclient.autologin=false -Dirc.nickname=Player1 -Dirc.username=Player1 -Xms1G -Xmx2G -cp "%CP%" net.minecraft.client.main.Main --gameDir "%WORKDIR1%" --assetsDir "%ASSETS%" --assetIndex 1.8 --username Player1 --version 1.8.9 --accessToken 0

timeout /t 5 /nobreak > nul

echo.
echo [Launcher] Starting Instance 2...
start "MC-2" /D "%WORKDIR2%" "%JAVA_EXE%" -Djava.library.path="%NATIVES%" -Dclient.autologin=false -Dirc.nickname=Player2 -Dirc.username=Player2 -Xms1G -Xmx2G -cp "%CP%" net.minecraft.client.main.Main --gameDir "%WORKDIR2%" --assetsDir "%ASSETS%" --assetIndex 1.8 --username Player2 --version 1.8.9 --accessToken 0

:done
echo.
echo [Launcher] Done.
echo [Launcher] Logs: run\logs\latest.log and run2\logs\latest.log
pause
exit /b 0

:build_fail
echo.
echo [Launcher] Build failed. Fix compile errors and retry.
pause
exit /b 1
