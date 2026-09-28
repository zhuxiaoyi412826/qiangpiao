@echo off
chcp 65001 >nul
"D:\software\mysql\mysql-8.4.10-winx64\mysql-8.4.10-winx64\bin\mysql.exe" -uroot -p412826 --default-character-set=utf8mb4 qiangpiao < "%~dp0city_data.sql"
echo EXITCODE=%errorlevel%
