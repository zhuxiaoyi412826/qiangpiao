@echo off
set CATALINA_HOME=D:\software\tomcat\Tomcat\apache-tomcat-9.0.64-windows-x64\apache-tomcat-9.0.64
set CATALINA_BASE=C:\Users\Administrator\AppData\Local\JetBrains\IntelliJIdea2026.1\tomcat\4ac6af0b-c78c-4791-b2c3-5d1f23339b04
call "%CATALINA_HOME%\bin\shutdown.bat" > d:\daima\Lianshi\qiangpiao\md\tomcat.log 2>&1
echo SHUTDOWN_EXIT=%errorlevel% >> d:\daima\Lianshi\qiangpiao\md\tomcat.log
exit /b 0
