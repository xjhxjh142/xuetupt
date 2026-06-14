@echo off
chcp 65001 >nul
title 学途项目启动器

echo ========================================
echo     学途 (XueTuPT) 项目启动脚本
echo ========================================
echo.

:: 获取项目根目录
set PROJECT_DIR=%~dp0
cd /d "%PROJECT_DIR%"

:: ========================================
:: 1. 检查并启动 Ollama 服务
:: ========================================
echo [1/4] 检查 Ollama 服务...
curl -s --max-time 3 http://localhost:11434/api/tags >nul 2>&1
if %errorlevel% neq 0 (
    echo   Ollama 未运行，正在启动...
    start "Ollama" "C:\Program Files\Ollama\ollama.exe" serve
    echo   等待 Ollama 启动...
    :wait_ollama
    timeout /t 3 /nobreak >nul
    curl -s --max-time 3 http://localhost:11434/api/tags >nul 2>&1
    if %errorlevel% neq 0 goto wait_ollama
    echo   ✅ Ollama 启动成功
) else (
    echo   ✅ Ollama 已在运行
)

:: ========================================
:: 2. 启动 AI Agent 服务
:: ========================================
echo [2/4] 启动 AI Agent 服务 (端口 8013)...
start "xuetupt-agent" /D "%PROJECT_DIR%xuetupt-agent" "E:\ProgramTool\miniconda3\envs\agent_env\python.exe" main.py
echo   等待 Agent 服务启动...
:wait_agent
timeout /t 3 /nobreak >nul
curl -s --max-time 3 http://localhost:8013/health >nul 2>&1
if %errorlevel% neq 0 goto wait_agent
echo   ✅ AI Agent 服务启动成功

:: ========================================
:: 3. 启动 Java 后端服务
:: ========================================
echo [3/4] 启动 Java 后端服务 (端口 8081)...

start "xuetupt-server" /D "%PROJECT_DIR%xuetupt-server" mvn spring-boot:run
echo   ✅ 后端服务启动命令已执行（Maven 构建需要一些时间）...

:: ========================================
:: 4. 启动前端开发服务器
:: ========================================
echo [4/4] 启动前端开发服务器 (端口 5173)...
start "xuetupt-web" /D "%PROJECT_DIR%xuetupt-web" npm run dev

echo.
echo ========================================
echo     所有服务启动命令已执行！
echo ========================================
echo.
echo   AI Agent 服务:  http://localhost:8013
echo   后端 API 服务:   http://localhost:8081

echo   前端页面:        http://localhost:5173
echo.
echo   提示：首次启动后端需要 Maven 下载依赖，请耐心等待
echo   关闭项目请运行 close.bat
echo.
pause
