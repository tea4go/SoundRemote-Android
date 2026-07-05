@echo off
setlocal

rem SoundRemote Android 构建/发布工具入口。
rem - 直接双击：显示菜单
rem - 命令行带参数：透传给 build_android_bywin.ps1（例如 run_win.bat -Build）

set "SCRIPT=.\scripts\windows\build_android_bywin.ps1"

if not "%~1"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" %*
    goto end
)

:menu
cls
echo ================================================
echo   SoundRemote Android - 构建 / 发布工具
echo ================================================
echo.
echo   [1] 检查构建环境（-Check）
echo   [2] 构建 Release APK / AAB（-Build）
echo   [3] 发布到 GitHub Release
echo   [4] 发布到 Gitee Release
echo   [5] 显示帮助（-Help）
echo   [0] 退出
echo.
set /p "choice=请选择: "

if "%choice%"=="1" goto check
if "%choice%"=="2" goto build
if "%choice%"=="3" goto publish_github
if "%choice%"=="4" goto publish_gitee
if "%choice%"=="5" goto help
if "%choice%"=="0" goto end
goto menu

:check
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Check
goto done

:build
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Build
goto done

:publish_github
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Publish -Platform github
goto done

:publish_gitee
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Publish -Platform gitee
goto done

:help
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%" -Help
goto done

:done
echo.
pause
goto menu

:end
endlocal
