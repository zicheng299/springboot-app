@echo off
chcp 65001 >nul
title Dropnote - 启动中...

echo.
echo   ========================================
echo     Dropnote - 文件上传中心
echo   ========================================
echo.

:: ---- Check Java ----
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 未找到 Java，请安装 JDK 17+
    pause
    exit /b 1
)

for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set javaver=%%v
)
echo   Java: %javaver%
echo.

:: ---- Build & Run ----
echo   [1/2] 正在编译项目...
call gradlew.bat compileKotlin --quiet 2>&1
if %errorlevel% neq 0 (
    echo   [错误] 编译失败，请查看上方输出
    pause
    exit /b 1
)

echo   [2/2] 正在启动服务 (端口 28080)...
echo.
start http://localhost:28080
call gradlew.bat bootRun
pause
