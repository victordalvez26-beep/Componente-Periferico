@echo off
REM Load environment variables from .env file and start WildFly
REM Usage: start-wildfly.bat

echo Loading environment variables from .env file...

REM Read .env file and set environment variables
for /f "tokens=1,2 delims==" %%a in (.env) do (
    if not "%%a"=="" (
        if not "%%a:~0,1%"=="#" (
            set "%%a=%%b"
            echo Set %%a=%%b
        )
    )
)

echo.
echo Starting WildFly with environment variables...
echo MONGODB_URI=%MONGODB_URI%
echo MONGODB_DB=%MONGODB_DB%
echo.

REM Start WildFly
call "%WILDFLY_HOME%\bin\standalone.bat"
