@echo off
cd /d d:\daima\Lianshi\qiangpiao\qiangpiao-frontend
set LOG=d:\daima\Lianshi\qiangpiao\md\npm.log
echo === step1: 移走旧 lock，强制 npm 重新解析 === > %LOG%
if exist package-lock.json move /y package-lock.json package-lock.json.bak >> %LOG% 2>&1
call npm install --no-audit --no-fund >> %LOG% 2>&1
echo INSTALL_EXIT=%errorlevel% >> %LOG%
dir /b node_modules\vite >> %LOG% 2>&1
dir /b node_modules\esbuild >> %LOG% 2>&1
dir /b node_modules\rollup >> %LOG% 2>&1
dir /b node_modules\.bin\vite.cmd >> %LOG% 2>&1
exit /b 0
