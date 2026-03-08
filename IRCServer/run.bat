@echo off
setlocal

set JAVA_HOME=D:\Software\Dev\Zulu\zulu-21
set JAVA=%JAVA_HOME%\bin\java.exe
set CP=out\classes;lib\*

echo [IRCServer] Starting...
"%JAVA%" -cp "%CP%" irc.server.IRCServer %*
