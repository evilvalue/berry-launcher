@echo off
setlocal enableextensions enabledelayedexpansion
title BerryUp
set "DIR=%~dp0"
set "JAR=%DIR%BerryUp.jar"
set "UPD=%LOCALAPPDATA%\BerryUp\update\BerryUp.jar"

if not exist "%JAR%" (
  echo.
  echo BerryUp.jar не найден рядом с этим файлом.
  echo Распакуй BerryUp.zip целиком в одну папку и запусти BerryUp.cmd заново.
  echo Скачать: https://evilvalue.github.io/berry/
  echo.
  pause
  start "" "https://evilvalue.github.io/berry/"
  exit /b 1
)

rem Если обновление уже скачано, подменяем jar до запуска.
if exist "%UPD%" (
  copy /y "%UPD%" "%JAR%" >nul 2>&1
  del /q "%UPD%" >nul 2>&1
)

set "JAVAW="
if defined BERRYUP_JAVA if exist "%BERRYUP_JAVA%" set "JAVAW=%BERRYUP_JAVA%"
if not defined JAVAW if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javaw.exe" set "JAVAW=%JAVA_HOME%\bin\javaw.exe"
if not defined JAVAW call :scan "%APPDATA%\PrismLauncher\java"
if not defined JAVAW call :scan "%APPDATA%\PineconeMC\java"
if not defined JAVAW call :scan "%APPDATA%\ElyPrismLauncher\java"
if not defined JAVAW call :scan "%LOCALAPPDATA%\Programs\PrismLauncher\java"
if not defined JAVAW for /f "delims=" %%J in ('where javaw.exe 2^>nul') do if not defined JAVAW set "JAVAW=%%J"

if /i "%~1"=="--java" (
  if defined JAVAW (echo %JAVAW%) else (echo Java не найдена)
  if defined JAVAW (exit /b 0) else (exit /b 1)
)

if not defined JAVAW (
  echo.
  echo Java не найдена. BerryUp запускается на Java из Prism Launcher.
  echo.
  echo 1. Установи Prism Launcher: https://prismlauncher.org/download/windows/
  echo    или PineconeMC:        https://pineconemc.com/download/
  echo 2. Запусти в нём игру один раз - Prism скачает Java.
  echo 3. Запусти BerryUp.cmd снова.
  echo.
  pause
  start "" "https://prismlauncher.org/download/windows/"
  exit /b 1
)

:launch
"%JAVAW%" -jar "%JAR%" %*
if exist "%UPD%" goto launch
exit /b 0

:scan
if "%~1"=="" exit /b 0
if not exist "%~1" exit /b 0
for /f "delims=" %%D in ('dir /b /ad /o-n "%~1\java-runtime-*" 2^>nul') do if not defined JAVAW if exist "%~1\%%D\bin\javaw.exe" set "JAVAW=%~1\%%D\bin\javaw.exe"
exit /b 0
