param(
  [string]$SdkRoot
)

$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot '_common.ps1')

Enable-AutoConfirm

<#
.SYNOPSIS
  在 Windows 上安装/修复 Android SDK，并配置当前会话及用户级环境变量。
.DESCRIPTION
  主要流程：
  1) 定位或自举安装 sdkmanager（cmdline-tools）
  2) 校验 Java 17 环境
  3) 安装 platform-tools / platform / build-tools 等组件
  4) 写入 ANDROID_HOME，并把 platform-tools 加入用户 PATH
.PARAMETER SdkRoot
  优先使用的 SDK 根目录。若为空，将依次回退到 ANDROID_HOME 或默认路径。
#>

<#
.SYNOPSIS
  下载并安装 Android commandline-tools（用于提供 sdkmanager）。
.PARAMETER SdkRootPath
  Android SDK 根目录（会在其下创建 cmdline-tools\latest）。
.OUTPUTS
  [bool] 是否安装成功。
.NOTES
  仅负责把 cmdline-tools 放到 <SdkRootPath>\cmdline-tools\latest，不安装具体 SDK 组件。
#>
function Install-SdkManagerBootstrap {
  param([string]$SdkRootPath)

  $zipName = 'commandlinetools-win-11076708_latest.zip'
  $urls = @(
    "https://mirrors.huaweicloud.com/android/repository/$zipName",
    "https://mirrors.cloud.tencent.com/AndroidSDK/$zipName",
    "https://dl.google.com/android/repository/$zipName"
  )

  # cmdline-tools 的标准目录结构：<SDK>\cmdline-tools\latest\bin\sdkmanager.bat
  New-DirectoryIfMissing (Join-Path $SdkRootPath 'cmdline-tools')

  # 使用临时目录下载/解压，避免污染 SDK 目录；用 guid 避免并发/重入时冲突
  $tmpZip = Join-Path $env:TEMP ("cmdline-tools_{0}.zip" -f ([guid]::NewGuid().ToString('N')))
  $tmpExtract = Join-Path $env:TEMP ("cmdline-tools_extract_{0}" -f ([guid]::NewGuid().ToString('N')))
  New-DirectoryIfMissing $tmpExtract

  try {
    if (-not (Save-WebFile -Urls $urls -OutFile $tmpZip)) {
      Write-Fail "所有镜像源下载失败"
      return $false
    }

    Write-Host "  解压到临时目录 ..." -ForegroundColor Cyan
    try {
      Expand-Archive -LiteralPath $tmpZip -DestinationPath $tmpExtract -Force
    } catch {
      Write-Fail "Expand-Archive 解压失败"
      return $false
    }

    $extracted = Join-Path $tmpExtract 'cmdline-tools'
    if (-not (Test-Path -LiteralPath $extracted)) {
      Write-Fail "解压后未找到 cmdline-tools 目录"
      return $false
    }

    # 统一写入到 latest：如果目录已存在则先清理，确保脚本可重复执行
    $latest = Join-Path $SdkRootPath 'cmdline-tools\latest'
    if (Test-Path -LiteralPath $latest) {
      Remove-Item -LiteralPath $latest -Recurse -Force -ErrorAction SilentlyContinue
    }
    Move-Item -LiteralPath $extracted -Destination $latest -Force

    $sdkmanager = Join-Path $latest 'bin\sdkmanager.bat'
    if (Test-Path -LiteralPath $sdkmanager) {
      Write-Ok "SDKManager 已安装：$sdkmanager"
      return $true
    }
    Write-Fail "安装后仍未找到 SDKManager.bat"
    return $false
  } finally {
    # 保证清理临时文件，即使下载/解压/移动过程中失败也不留下垃圾
    Remove-Item -LiteralPath $tmpZip -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $tmpExtract -Recurse -Force -ErrorAction SilentlyContinue
  }
}

<#
.SYNOPSIS
  打印 sdkmanager 的版本信息（用于快速判断 cmdline-tools 是否可运行）。
.PARAMETER SdkManagerPath
  sdkmanager.bat 的路径。
#>
function Show-SdkManagerVersion([string]$SdkManagerPath) {
  $ver = Invoke-NativeText -FilePath $SdkManagerPath -Arguments @('--version') |
    Where-Object { $_ -match '^[0-9]' } |
    Select-Object -First 1
  if ($ver) {
    Write-Ok "    版本：$ver"
  } else {
    Write-Warn "    无法读取 SDKManager 版本（可能 Java 未就绪，下一步会校验）"
  }
}

<#
.SYNOPSIS
  调用 sdkmanager 安装指定 Android SDK 组件，并自动接受 license。
.DESCRIPTION
  sdkmanager 在 Windows 下与管道/终端交互有兼容性问题，这里通过：
  - 生成大量 y 的临时文件以自动回答 license 提示
  - 使用 cmd.exe /c 执行管道命令，避免 PowerShell 管道行为差异
.PARAMETER SdkManagerPath
  sdkmanager.bat 的路径。
.PARAMETER AndroidHome
  Android SDK 根目录（会传入 --sdk_root=...）。
.PARAMETER Packages
  需要安装的包列表（如 platform-tools、platforms;android-xx）。
.OUTPUTS
  [bool] 是否安装成功。
#>
function Invoke-SdkManager {
  param(
    [string]$SdkManagerPath,
    [string]$AndroidHome,
    [string[]]$Packages
  )
  $sdkRootArg = "--sdk_root=$AndroidHome"
  $pkgArgs = ($Packages | ForEach-Object { '"{0}"' -f $_ }) -join ' '

  $yesFile = Join-Path $env:TEMP ("sdkmanager_yes_{0}.txt" -f ([guid]::NewGuid().ToString('N')))
  (1..2500 | ForEach-Object { 'y' }) | Set-Content -LiteralPath $yesFile -Encoding ASCII
  try {
    $cmd_str = "type `"$yesFile`" 2>nul| `"$SdkManagerPath`" `"$sdkRootArg`" $pkgArgs"
    Write-Host "  运行命令：`"$SdkManagerPath`" `"$sdkRootArg`" $pkgArgs" -ForegroundColor Cyan
    Invoke-NativeStream -Block {& cmd.exe /c $cmd_str }
  } finally {
    Remove-Item -LiteralPath $yesFile -Force -ErrorAction SilentlyContinue
  }
  if ($LASTEXITCODE -ne 0) {
    Write-Fail "Android SDK 组件安装失败"
    return $false
  }
  return $true
}

<#
.SYNOPSIS
  读取 cmdline-tools\latest\source.properties 的 Pkg.Revision 主版本号。
.OUTPUTS
  [int] 主版本号（如 12、21）；读不到返回 0。
#>
function Get-CmdlineToolsMajor {
  param([string]$AndroidHome)
  $props = Join-Path $AndroidHome 'cmdline-tools\latest\source.properties'
  if (-not (Test-Path -LiteralPath $props)) { return 0 }
  foreach ($line in Get-Content -LiteralPath $props) {
    if ($line -match '^Pkg\.Revision\s*=\s*(\d+)') { return [int]$Matches[1] }
  }
  return 0
}

<#
.SYNOPSIS
  若当前 cmdline-tools 版本过老（< 阈值），通过 sdkmanager 自升级到 latest。
.DESCRIPTION
  Google 从 SDK XML v4 起，旧版 cmdline-tools（<= 12）无法识别 Android 36+ 的 platform 包。
  升级流程：
  1) 用旧 sdkmanager 装 cmdline-tools;latest，它会解压到 cmdline-tools\latest-2\
  2) 删除旧的 cmdline-tools\latest\，把 latest-2\ 改名为 latest\
  3) 让调用方重新定位 sdkmanager
.PARAMETER SdkManagerPath
  当前 sdkmanager.bat 路径（旧版本）。
.PARAMETER AndroidHome
  SDK 根目录。
.PARAMETER MinVersion
  最低可接受主版本号，低于此值触发升级。
.OUTPUTS
  [bool] 升级完成或本已足够返回 true；失败返回 false。
#>
function Update-CmdlineToolsIfOld {
  param(
    [string]$SdkManagerPath,
    [string]$AndroidHome,
    [int]$MinVersion = 16
  )
  $ver = Get-CmdlineToolsMajor -AndroidHome $AndroidHome
  if ($ver -ge $MinVersion) {
    Write-Ok "cmdline-tools 版本 $ver 已足够（>= $MinVersion），跳过升级"
    return $true
  }

  Write-Warn "cmdline-tools 主版本 $ver 过旧（< $MinVersion），尝试升级到 latest ..."
  if (-not (Invoke-SdkManager -SdkManagerPath $SdkManagerPath -AndroidHome $AndroidHome -Packages @('cmdline-tools;latest'))) {
    Write-Fail 'cmdline-tools 升级失败'
    return $false
  }

  # sdkmanager 会把新版装到 cmdline-tools\latest-2\（因 latest 已占用），需要把它顶替过去
  $latest  = Join-Path $AndroidHome 'cmdline-tools\latest'
  $latest2 = Join-Path $AndroidHome 'cmdline-tools\latest-2'
  if (Test-Path -LiteralPath $latest2) {
    Remove-Item -LiteralPath $latest -Recurse -Force -ErrorAction SilentlyContinue
    Move-Item -LiteralPath $latest2 -Destination $latest -Force
    Write-Ok "已把新版 cmdline-tools 从 latest-2 顶替到 latest"
  }
  return $true
}

<#
.SYNOPSIS
  在磁盘上检查某个 platform api 是否已安装（兼容 android-<api> 和 android-<api>.0 命名）。
.OUTPUTS
  [string] 已装的目录名（如 android-37.0）；未装返回 $null。
#>
function Test-InstalledPlatform {
  param([string]$AndroidHome, [int]$Api)
  foreach ($name in @("android-$Api", "android-$Api.0")) {
    if (Test-Path -LiteralPath (Join-Path $AndroidHome "platforms\$name")) {
      return $name
    }
  }
  return $null
}

<#
.SYNOPSIS
  智能安装 Android platform：先按新命名（android-<api>.0）尝试，失败再降级到旧命名。
.NOTES
  Google 从 API 36 开始，稳定 platform 命名过渡到 android-<major>.<minor>；
  API 35 及以前仍是 android-<api>。已装则跳过。
.OUTPUTS
  [bool] 安装成功或已装返回 true。
#>
function Install-AndroidPlatform {
  param([string]$SdkManagerPath, [string]$AndroidHome, [int]$Api)
  $exist = Test-InstalledPlatform -AndroidHome $AndroidHome -Api $Api
  if ($exist) {
    Write-Ok "platform 已装：$exist"
    return $true
  }
  # 新命名优先（API 37+），失败降级到无小数版
  foreach ($name in @("android-$Api.0", "android-$Api")) {
    Write-Host "  尝试安装 platforms;$name ..." -ForegroundColor Cyan
    if (Invoke-SdkManager -SdkManagerPath $SdkManagerPath -AndroidHome $AndroidHome -Packages @("platforms;$name")) {
      if (Test-Path -LiteralPath (Join-Path $AndroidHome "platforms\$name")) {
        Write-Ok "platform 安装成功：$name"
        return $true
      }
    }
  }
  Write-Fail "platforms;android-$Api 系列全部候选安装失败"
  return $false
}

<#
.SYNOPSIS
  智能安装 Android build-tools：已装则跳过，否则装指定版本；主版本存在但 minor 不同也可接受。
.OUTPUTS
  [bool]
#>
function Install-AndroidBuildTools {
  param([string]$SdkManagerPath, [string]$AndroidHome, [int]$Api)
  $btDir = Join-Path $AndroidHome 'build-tools'
  # 已存在同 major 版本即视为满足（如 API 37 匹配 37.x.x）
  if (Test-Path -LiteralPath $btDir) {
    $matched = Get-ChildItem -LiteralPath $btDir -Directory -ErrorAction SilentlyContinue |
      Where-Object { $_.Name -match "^$Api\." } | Select-Object -First 1
    if ($matched) {
      Write-Ok "build-tools 已装：$($matched.Name)"
      return $true
    }
  }
  $pkg = "build-tools;$Api.0.0"
  Write-Host "  尝试安装 $pkg ..." -ForegroundColor Cyan
  return Invoke-SdkManager -SdkManagerPath $SdkManagerPath -AndroidHome $AndroidHome -Packages @($pkg)
}

Write-Host ""
Write-Banner -Title 'Android SDK 自动安装脚本（Windows PowerShell）       ' -Color Cyan -Width 55
Write-Host ""

$sdkRootDefault = $SdkRoot
# 目录优先级：参数 > 环境变量 ANDROID_HOME > 脚本默认路径
if ([string]::IsNullOrWhiteSpace($sdkRootDefault)) { $sdkRootDefault = $env:ANDROID_HOME }
if ([string]::IsNullOrWhiteSpace($sdkRootDefault)) { $sdkRootDefault = 'C:\DevDisk\DevTools\AndroidSDK' }
$sdkRootDefault = $sdkRootDefault.Trim('"')

Write-Host "[1/6] 定位 SDKManager" -ForegroundColor Cyan
$sdkmanager = Find-SdkManager -PreferredRoot $sdkRootDefault
if ($sdkmanager) {
  Write-Ok "SDKManager 已找到：$sdkmanager"
  Show-SdkManagerVersion $sdkmanager
} else {
  # 常见期望路径：<SDK>\cmdline-tools\latest\bin\sdkmanager.bat
  $expected = Join-Path $sdkRootDefault 'cmdline-tools\latest\bin\sdkmanager.bat'
  Write-Warn "SDKManager 未找到：$expected"
  New-DirectoryIfMissing $sdkRootDefault
  if (-not (Install-SdkManagerBootstrap -SdkRootPath $sdkRootDefault)) {
    Write-Fail "命令行工具包下载/安装失败"
    Write-Fail "请手动下载：https://developer.android.com/studio#command-tools"
    exit 1
  }
  $sdkmanager = Find-SdkManager -PreferredRoot $sdkRootDefault
  if (-not $sdkmanager) {
    Write-Fail "安装后仍未找到 sdkmanager.bat"
    exit 1
  }
}

# 通过 sdkmanager 反推真实 SDK 根目录，避免用户传入/环境变量指向错误位置
$androidHome = Get-AndroidHomeFromSdkManager -SdkManagerPath $sdkmanager
Write-Ok "ANDROID_HOME 推导为：$androidHome"
$env:ANDROID_HOME = $androidHome

Write-Host "[2/6] 检查 Java 环境" -ForegroundColor Cyan
if (-not (Assert-Java17)) { exit 1 }
$api = Get-AndroidSdkApi
Write-Ok "项目 compileSdk = $api（读自 app\build.gradle.kts）"

Write-Host "[3/6] 升级 cmdline-tools（如版本过旧）" -ForegroundColor Cyan
if (-not (Update-CmdlineToolsIfOld -SdkManagerPath $sdkmanager -AndroidHome $androidHome)) { exit 1 }
# 升级后重新定位 sdkmanager（可能路径没变，但确保拿到新版本）
$sdkmanager = Find-SdkManager -PreferredRoot $androidHome
if (-not $sdkmanager) {
  Write-Fail "升级 cmdline-tools 后未找到 sdkmanager.bat"
  exit 1
}
Show-SdkManagerVersion $sdkmanager

Write-Host "[4/6] 安装 platform-tools" -ForegroundColor Cyan
if (Test-Path -LiteralPath (Join-Path $androidHome 'platform-tools\adb.exe')) {
  Write-Ok "platform-tools 已装"
} else {
  if (-not (Invoke-SdkManager -SdkManagerPath $sdkmanager -AndroidHome $androidHome -Packages @('platform-tools'))) { exit 1 }
}

Write-Host "[5/6] 安装 platforms;android-$api（智能命名）" -ForegroundColor Cyan
if (-not (Install-AndroidPlatform -SdkManagerPath $sdkmanager -AndroidHome $androidHome -Api ([int]$api))) { exit 1 }

Write-Host "[6/6] 安装 build-tools;$api.x.x" -ForegroundColor Cyan
if (-not (Install-AndroidBuildTools -SdkManagerPath $sdkmanager -AndroidHome $androidHome -Api ([int]$api))) { exit 1 }

Write-Host ""
Write-Ok "Android SDK 组件安装完成！"
Write-Host ""

Write-Host "[配置] 环境变量" -ForegroundColor Cyan
$platformTools = Join-Path $androidHome 'platform-tools'

# 写入用户级环境变量（需要新开终端窗口才会影响新的 shell）
if ($androidHome) {
  Set-UserEnvIfChanged -Name 'ANDROID_HOME' -Value $androidHome
} else {
  Write-Warn "未检测到 ANDROID_HOME 版本，跳过 ANDROID_HOME 设置"
}

if (-not (Add-UserPathSegment -Segment $platformTools)) {
  Write-Warn "set PATH = $env:Path;$platformTools (失败)"
}

# 同步到当前会话环境变量，便于脚本后续步骤/当前终端立即可用
$env:ANDROID_HOME = $androidHome
$env:Path = "$platformTools;$env:Path"
Write-Ok "当前 shell 环境变量变更已生效（export）"

Write-Host ""
Write-Banner -Title 'Android SDK 安装 & 配置完成！                         ' -Color Cyan -TitleColor Green -Width 55
Write-Host ""
Write-Host "  注意：写入的用户环境变量需要新开终端窗口才会生效" -ForegroundColor Yellow
Write-Host ""
