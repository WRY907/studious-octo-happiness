@echo off
chcp 65001 >nul
title 华为手机全息数据驾驶舱 - 一键启动
cd /d "%~dp0"

echo.
echo ================================================================
echo        🚀 华为手机全息数据驾驶舱 HOLO DATA COCKPIT
echo ================================================================
echo.

REM ================= 1. 环境检查 =================
echo [1/5] 检查运行环境...

where java >nul 2>nul
if errorlevel 1 (
  echo   ❌ 未检测到 Java，请先安装 JDK 8 及以上版本
  echo      下载地址：https://adoptium.net/
  pause
  exit /b 1
)
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%v
echo   ✅ Java: %JAVA_VER%

where node >nul 2>nul
if errorlevel 1 (
  echo   ❌ 未检测到 Node.js，请先安装 Node.js 16 及以上版本
  echo      下载地址：https://nodejs.org/
  pause
  exit /b 1
)
echo   ✅ Node: 

node --version

REM ================= 2. 前端依赖 =================
echo.
echo [2/5] 检查前端依赖...
if exist "holo-cockpit-frontend\node_modules" (
  echo   ✅ 前端依赖已安装，跳过
) else (
  echo   ⏳ 首次运行，正在安装前端依赖（使用国内镜像，可能需要几分钟）...
  pushd holo-cockpit-frontend
  call npm install --registry=https://registry.npmmirror.com
  if errorlevel 1 (
    echo   ❌ 前端依赖安装失败，请检查网络后重试
    popd
    pause
    exit /b 1
  )
  popd
  echo   ✅ 前端依赖安装完成
)

REM ================= 3. 数据库 =================
echo.
echo [3/5] 检查数据库...
where mysql >nul 2>nul
if errorlevel 1 (
  echo   ⚠️ 未检测到 mysql 命令，跳过数据库检查
  echo      若数据库尚未初始化，请手动执行：mysql -uroot -p ^< sql\init.sql
) else (
  mysql -uroot -e "use huawei_cockpit;" >nul 2>nul
  if errorlevel 1 (
    echo   ⚠️ 数据库 huawei_cockpit 不存在或密码不是默认值
    set /p initDb=      是否现在初始化数据库？(Y/N)：
    if /i "%initDb%"=="Y" (
      set /p dbUser=      请输入 MySQL 用户名 (默认 root)：
      if "!dbUser!"=="" set "dbUser=root"
      set /p dbPass=      请输入 MySQL 密码：
      mysql -u!dbUser! -p!dbPass! --default-character-set=utf8 < sql\init.sql
      if errorlevel 1 (
        echo   ❌ 数据库初始化失败，请检查 MySQL 服务与账号密码
      ) else (
        echo   ✅ 数据库 huawei_cockpit 初始化完成
      )
    ) else (
      echo      跳过初始化。注意：后端启动需要数据库已就绪
    )
  ) else (
    echo   ✅ 数据库 huawei_cockpit 已就绪
  )
)

REM ================= 4. 启动后端 =================
echo.
echo [4/5] 启动后端服务（Spring Boot，端口 8080）...
echo      首次启动会自动下载 Maven 依赖，请耐心等待
pushd holo-cockpit-backend
start "后端服务-SpringBoot-8080" cmd /k mvnw.cmd spring-boot:run
popd

REM ================= 5. 启动前端 =================
echo.
echo [5/5] 启动前端服务（Vite，端口 3000）...
pushd holo-cockpit-frontend
start "前端服务-Vite-3000" cmd /k npm run dev
popd

echo.
echo ================================================================
echo   ✅ 启动完成！
echo.
echo   📊 数据大屏：   http://localhost:3000/
echo   🔐 管理后台登录：http://localhost:3000/#/login
echo.
echo   默认账号：
echo     管理员  admin / admin123
echo     商家    merchant / merchant123
echo.
echo   提示：两个服务窗口请不要关闭，关闭窗口即停止服务
echo         后端首次启动需等待依赖下载与编译完成
echo ================================================================
echo.
timeout /t 8 /nobreak >nul
start http://localhost:3000/
echo 已在浏览器中打开 http://localhost:3000/（若未打开请手动访问）
pause
