@echo off
rem BerryUp: запуск лаунчера на любой Java, которая есть на компьютере.
rem Windows не всегда связывает .jar с Java, поэтому этот файл ищет её сам:
rem сначала ту, что скачал Prism Launcher (или PineconeMC / MultiMC), потом
rem официальный лаунчер Minecraft, JAVA_HOME, PATH и установленные JDK.
chcp 65001 >nul 2>&1
setlocal EnableExtensions EnableDelayedExpansion
title BerryUp

set "HERE=%~dp0"
set "JAR=%HERE%BerryUp.jar"
set "JAVA="

if not exist "%JAR%" goto nojar

set "PF=%ProgramFiles%"
set "PF86=%ProgramFiles(x86)%"

rem 1. Путь, указанный вручную: set BERRYUP_JAVA=C:\...\javaw.exe
if defined BERRYUP_JAVA call :probe "%BERRYUP_JAVA%"

rem 2. Java, выбранная в настройках Prism Launcher, PineconeMC, PolyMC, MultiMC
call :fromcfg "%APPDATA%\PrismLauncher\prismlauncher.cfg"
call :fromcfg "%APPDATA%\PineconeMC\pineconemc.cfg"
call :fromcfg "%APPDATA%\PolyMC\polymc.cfg"
call :fromcfg "%APPDATA%\MultiMC\multimc.cfg"

rem 3. Java, которую эти лаунчеры скачали сами
call :scan "%APPDATA%\PrismLauncher\java"
call :scan "%APPDATA%\PineconeMC\java"
call :scan "%APPDATA%\PolyMC\java"
call :scan "%APPDATA%\MultiMC\java"
call :scan "%LOCALAPPDATA%\Programs\PrismLauncher\java"

rem 4. Java официального лаунчера Minecraft
call :scan "%PF86%\Minecraft Launcher\runtime"
call :scan "%LOCALAPPDATA%\Packages\Microsoft.4297127D64EC6_8wekyb3d8bbwe\LocalCache\Local\runtime"

rem 5. JAVA_HOME, PATH, реестр и установленные JDK
if defined JAVA_HOME call :probe "%JAVA_HOME%\bin\javaw.exe"
for /f "delims=" %%P in ('where javaw 2^>nul') do call :probe "%%~P"
for %%K in ("HKLM\SOFTWARE\JavaSoft\JDK" "HKLM\SOFTWARE\JavaSoft\Java Development Kit" "HKLM\SOFTWARE\JavaSoft\Java Runtime Environment" "HKLM\SOFTWARE\WOW6432Node\JavaSoft\JDK" "HKCU\SOFTWARE\JavaSoft\JDK") do for /f "tokens=2,*" %%A in ('reg query %%K /s /v JavaHome 2^>nul ^| findstr /i "JavaHome"') do call :probe "%%B\bin\javaw.exe"
call :scan "%PF%\Java"
call :scan "%PF%\Eclipse Adoptium"
call :scan "%PF%\Microsoft"
call :scan "%PF%\Zulu"
call :scan "%PF%\Amazon Corretto"
call :scan "%PF%\BellSoft"
call :scan "%PF86%\Java"
call :scan "%LOCALAPPDATA%\Programs\Eclipse Adoptium"

if /i "%~1"=="--java" goto showjava
if not defined JAVA goto nojava

set "JAVACON=!JAVA:javaw.exe=java.exe!"
if "%~1"=="" goto gui
"!JAVACON!" -jar "%JAR%" %*
exit /b %errorlevel%

:gui
start "" "!JAVA!" -jar "%JAR%"
exit /b 0

:showjava
echo !JAVA!
exit /b 0

:nojava
echo.
echo Java не нашлась: ни в Prism, ни в Minecraft, ни в системе.
echo BerryUp — Java-программа, без Java она не запустится.
echo.
echo Что делать:
echo   1. Открой Prism Launcher и запусти в нём любую версию один раз —
echo      Prism сам скачает Java 21, после этого всё заработает.
echo   2. Или поставь Java 21: https://adoptium.net/temurin/releases/
echo.
echo Если Java стоит в необычном месте, укажи её так:
echo   set BERRYUP_JAVA=C:\путь\к\javaw.exe
echo.
pause
exit /b 1

:nojar
echo.
echo Рядом с этим файлом нет BerryUp.jar.
echo Положи START-BerryUp.bat и BerryUp.jar в одну папку и запусти снова.
echo.
pause
exit /b 1

rem --- служебные подпрограммы ---

:scan
rem %1 — папка, внутри которой ищем javaw.exe
if defined JAVA goto :eof
if not exist "%~1" goto :eof
for /f "delims=" %%P in ('dir /b /s "%~1\javaw.exe" 2^>nul') do call :probe "%%~P"
goto :eof

:fromcfg
rem %1 — настройки лаунчера, берём из них строку JavaPath=
if defined JAVA goto :eof
if not exist "%~1" goto :eof
for /f "usebackq tokens=1,* delims==" %%A in ("%~1") do if /i "%%~A"=="JavaPath" call :probe "%%~B"
goto :eof

:probe
rem %1 — java.exe, javaw.exe или папка с Java; берём только рабочую Java
if defined JAVA goto :eof
set "EXE=%~1"
if not defined EXE goto :eof
set EXE=%EXE:"=%
if not defined EXE goto :eof
set "EXE=!EXE:/=\!"
if exist "!EXE!\" set "EXE=!EXE!\bin\javaw.exe"
if /i "!EXE:~-8!"=="java.exe" set "EXE=!EXE:~0,-8!javaw.exe"
if not exist "!EXE!" goto :eof
set "BIN=!EXE!"
if /i "!BIN:~-9!"=="javaw.exe" set "BIN=!BIN:~0,-9!"
if not exist "!BIN!java.exe" goto :eof
"!BIN!java.exe" -version >nul 2>&1
if errorlevel 1 goto :eof
set "JAVA=!EXE!"
goto :eof
