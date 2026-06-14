@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title 学途项目关闭器

echo ========================================
echo     学途 (XueTuPT) 项目关闭脚本
echo ========================================
echo.

:: ========================================
:: 1. 关闭 AI Agent 服务
:: ========================================
echo [1/4] 关闭 AI Agent 服务...
for /f "tokens=2 delims=," %%a in ('tasklist /fi "windowtitle eq xuetupt-agent" /fo csv /nh 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
:: 也尝试通过端口查找
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8013" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo   ✅ AI Agent 服务已关闭

:: ========================================
:: 2. 关闭 Java 后端服务
:: ========================================
echo [2/4] 关闭 Java 后端服务...
:: 通过端口 8081 查找并关闭（实际端口在 application.yaml 中配置）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8081" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
:: 也尝试 8080 端口（兼容旧配置）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
:: 关闭 Maven 进程（通过窗口标题或进程名）
taskkill /f /fi "WINDOWTITLE eq xuetupt-server" >nul 2>&1
echo   ✅ Java 后端服务已关闭


:: ========================================
:: 3. 关闭前端开发服务器
:: ========================================
echo [3/4] 关闭前端开发服务器...
:: 通过端口 5173 查找并关闭（Vite 默认端口）
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
:: 直接关闭窗口标题匹配的 cmd.exe（处理端口变化或未匹配到的情况）
for /f "tokens=2 delims=," %%a in ('tasklist /fi "WINDOWTITLE eq xuetupt-web" /fo csv /nh 2^>nul') do (
    taskkill /f /pid %%a >nul 2>&1
)
echo   ✅ 前端开发服务器已关闭

:: ========================================
:: 4. 询问是否关闭 Ollama
:: ========================================
echo [4/4] Ollama 服务状态...
set /p CLOSE_OLLAMA="是否关闭 Ollama 服务？(y/n，默认 n): "
if /i "!CLOSE_OLLAMA!"=="y" (
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":11434" 2^>nul') do (
        taskkill /f /pid %%a >nul 2>&1
    )
    taskkill /f /im "ollama.exe" >nul 2>&1
    echo   ✅ Ollama 服务已关闭
) else (
    echo   ⏭️ 保留 Ollama 服务运行
)

echo.
echo ========================================
echo     所有服务已关闭！
echo ========================================
echo.
pause
