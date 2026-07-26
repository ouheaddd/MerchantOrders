@echo off
setlocal
set "APP_HOME=%~dp0"
java -Dmerchant.orders.projectDir="%APP_HOME%" -cp "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
