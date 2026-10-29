@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows (Self-bootstrapping for GitHub export)
@rem ##########################################################################

if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

if not exist "%APP_HOME%\debug.keystore" (
    if exist "%APP_HOME%\debug.keystore.base64" (
        certutil -decode "%APP_HOME%\debug.keystore.base64" "%APP_HOME%\debug.keystore" >NUL 2>&1
    )
)

set WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
if not exist "%WRAPPER_JAR%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
      "$ver = '9.3.1'; $dir = Join-Path $env:USERPROFILE ('.gradle\wrapper\dists\gradle-' + $ver + '-bin'); $bin = Join-Path $dir ('gradle-' + $ver + '\bin\gradle.bat'); if (-not (Test-Path $bin)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null; $zip = Join-Path $dir 'gradle.zip'; Invoke-WebRequest -Uri ('https://services.gradle.org/distributions/gradle-' + $ver + '-bin.zip') -OutFile $zip; Expand-Archive -Path $zip -DestinationPath $dir -Force; Remove-Item $zip -Force }; & $bin $args" %*
    exit /b %ERRORLEVEL%
)

set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
exit /b 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe
if exist "%JAVA_EXE%" goto execute

echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
exit /b 1

:execute
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%WRAPPER_JAR%" %*

if "%OS%"=="Windows_NT" endlocal
exit /b %ERRORLEVEL%
