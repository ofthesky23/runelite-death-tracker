@echo off
setlocal
cd /d "%~dp0"

rem The system Java installation on this computer is Java 8. Gradle needs a JDK.
if exist "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\javac.exe" (
  set "JAVA_HOME=C:\Program Files\Android\openjdk\jdk-21.0.8"
)

if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo A JDK 11 or newer is required. Install one and set JAVA_HOME, then run this file again.
  pause
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"
call "%~dp0gradlew.bat" run
if errorlevel 1 pause
