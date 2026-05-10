@echo off
@REM Maven Wrapper - downloads Maven on first use, no system install needed.
@REM Cache: %USERPROFILE%\.m2\wrapper\dists\
@REM Mirror: aliyun (suitable for China network).
setlocal

set "MAVEN_VERSION=3.9.9"
set "WRAPPER_BASE=%USERPROFILE%\.m2\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%WRAPPER_BASE%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_URL=https://maven.aliyun.com/repository/central/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo [mvnw] Maven %MAVEN_VERSION% not found, downloading from aliyun...
    if not exist "%WRAPPER_BASE%" mkdir "%WRAPPER_BASE%"
    powershell -NoProfile -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]'Tls12,Tls13'; [System.Net.WebRequest]::DefaultWebProxy=$null; $env:HTTP_PROXY=''; $env:HTTPS_PROXY=''; $ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%WRAPPER_BASE%\maven.zip' -UseBasicParsing"
    if errorlevel 1 (
        echo [mvnw] Download failed.
        exit /b 1
    )
    echo [mvnw] Extracting...
    powershell -NoProfile -Command "Expand-Archive -LiteralPath '%WRAPPER_BASE%\maven.zip' -DestinationPath '%WRAPPER_BASE%' -Force"
    if errorlevel 1 (
        echo [mvnw] Extract failed.
        exit /b 1
    )
    del /q "%WRAPPER_BASE%\maven.zip"
    echo [mvnw] Maven ready: %MAVEN_HOME%
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
