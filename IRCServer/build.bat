@echo off
setlocal

set JAVA_HOME=D:\Software\Dev\Zulu\zulu-21
set JAVAC=%JAVA_HOME%\bin\javac.exe
set SRC=src
set OUT=out\classes
set LIB=lib\*

echo [build] Compiling IRC Server...
if not exist "%OUT%" mkdir "%OUT%"

"%JAVAC%" -encoding UTF-8 -cp "%LIB%" -d "%OUT%" ^
    %SRC%\irc\server\IRCMessage.java ^
    %SRC%\irc\server\IRCServerChannel.java ^
    %SRC%\irc\server\IRCUserStore.java ^
    %SRC%\irc\server\db\DbConfig.java ^
    %SRC%\irc\server\db\MariaDbPool.java ^
    %SRC%\irc\server\db\SchemaBootstrap.java ^
    %SRC%\irc\server\db\UserDao.java ^
    %SRC%\irc\server\db\FriendDao.java ^
    %SRC%\irc\server\db\ProfileDao.java ^
    %SRC%\irc\server\db\MailDao.java ^
    %SRC%\irc\server\db\ChannelDao.java ^
    %SRC%\irc\server\db\SocialDao.java ^
    %SRC%\irc\server\db\MessageHistoryDao.java ^
    %SRC%\irc\server\service\IRCServiceDispatcher.java ^
    %SRC%\irc\server\service\FriendHandler.java ^
    %SRC%\irc\server\service\ProfileHandler.java ^
    %SRC%\irc\server\service\MailHandler.java ^
    %SRC%\irc\server\service\SocialHandler.java ^
    %SRC%\irc\server\service\MessageHistoryHandler.java ^
    %SRC%\irc\server\IRCServerHandler.java ^
    %SRC%\irc\server\IRCServer.java

if %ERRORLEVEL% neq 0 (
    echo [build] Compilation FAILED.
    exit /b 1
)

echo [build] Compilation OK.
echo.
echo Run with: run.bat [port]
