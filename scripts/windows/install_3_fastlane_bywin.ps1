<#
.SYNOPSIS
  在 Windows 上通过 Bundler 安装并检查 fastlane（基于仓库 Gemfile）。
.DESCRIPTION
  主要流程：
  - fastlane 官方 Android setup 推荐用 Gemfile + Bundler 管理 fastlane
  - 本仓库根目录 Gemfile 已声明 fastlane，这里执行 bundle install
  - 安装完成后，构建脚本通过 bundle exec fastlane build 调用
.PARAMETER CheckOnly
  只检查 Gemfile/Bundler/fastlane 命令解析，不执行 bundle install。
.EXAMPLE
  .\install_3_fastlane_bywin.ps1
.EXAMPLE
  .\install_3_fastlane_bywin.ps1 -CheckOnly
#>
param(
  [switch]$CheckOnly
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_common.ps1')

<#
.SYNOPSIS
  从注册表重新读取 机器级 + 用户级 PATH，合并注入当前 PowerShell 会话。
.NOTES
  install_2 刚装完的 Ruby/Bundler 把 ruby\bin 写入用户 PATH（注册表），但当前会话的
  $env:Path 是进程启动时的快照、不会自动刷新。调用本函数后，bundle 等命令即可在
  当前会话被 Get-Command 发现，无需重开窗口。
#>
function Sync-PathFromRegistry {
  $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
  $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
  $merged = @($machinePath, $userPath |
    Where-Object { -not [string]::IsNullOrWhiteSpace($_) }) -join ';'
  if (-not [string]::IsNullOrWhiteSpace($merged)) {
    $env:Path = $merged
  }
}

<#
.SYNOPSIS
  从候选 RubyGems 国内镜像中逐个探测，返回第一个真正可用（compact index 不重定向）的镜像。
.OUTPUTS
  [string] 可用镜像 URL；全部不可用时返回 $null。
.NOTES
  关键坑（实测）：USTC/清华/ruby-china 的 compact index /info/<gem> 端点会 302
  重定向回 rubygems.org —— 在国内等于死路，Bundler 跟着跳转后连官方源超时。
  curl -I 默认跟随重定向会拿到 200 假象，必须“禁止跟随重定向”探测才能识破。
  腾讯云、华为云镜像的 /info 端点是真 200、不重定向，实测可完整跑通 bundle install。
#>
function Select-GemMirror {
  $candidates = @(
    'https://mirrors.cloud.tencent.com/rubygems/',
    'https://repo.huaweicloud.com/repository/rubygems/'
  )
  foreach ($m in $candidates) {
    try {
      # 探测 /info/fastlane（compact index 的依赖解析端点）。必须禁止自动重定向：
      # 部分镜像该端点会 302 跳回 rubygems.org，跟随后会拿到 200 的假象。
      # 只有“直接 200、无 Location 跳转”的镜像才能真正离线解析依赖。
      $probe = ($m.TrimEnd('/')) + '/info/fastlane'
      $req = [System.Net.HttpWebRequest]::Create($probe)
      $req.Method = 'HEAD'
      $req.Timeout = 8000
      $req.AllowAutoRedirect = $false
      $resp = $req.GetResponse()
      $code = [int]$resp.StatusCode
      $resp.Close()
      if ($code -eq 200) {
        Write-Ok "选用 RubyGems 镜像：$m"
        return $m
      }
      Write-Warn "镜像 $m 的 /info 返回 $code（疑似重定向），尝试下一个"
    } catch {
      Write-Warn "镜像不可用，尝试下一个：$m"
    }
  }
  return $null
}

<#
.SYNOPSIS
  判断 Gemfile.lock 锁定的 Bundler 版本是否与当前 bundle 版本不一致。
.PARAMETER BundleRoot
  含 Gemfile / Gemfile.lock 的目录。
.OUTPUTS
  [bool] 不一致返回 true；无 lockfile、无 BUNDLED WITH 段或版本相同返回 false。
.NOTES
  lockfile 末尾 BUNDLED WITH 下一行即生成时的 Bundler 版本；与当前不一致时
  Bundler 会自动下载并切换到锁定版本重跑，故先对齐以避免无谓的旧版下载。
#>
function Test-LockfileBundlerMismatch {
  param([Parameter(Mandatory)] [string]$BundleRoot)

  $lockfile = Join-Path $BundleRoot 'Gemfile.lock'
  if (-not (Test-Path -LiteralPath $lockfile)) { return $false }

  $lines = Get-Content -LiteralPath $lockfile -ErrorAction SilentlyContinue
  $idx = [Array]::FindIndex($lines, [Predicate[string]] { param($l) $l.Trim() -eq 'BUNDLED WITH' })
  if ($idx -lt 0 -or $idx + 1 -ge $lines.Count) { return $false }
  $lockedVersion = $lines[$idx + 1].Trim()
  if ([string]::IsNullOrWhiteSpace($lockedVersion)) { return $false }

  $currentVersion = (Invoke-NativeText -FilePath 'bundle' -Arguments @('--version') |
    Select-Object -First 1) -replace '[^0-9.]', ''
  if ([string]::IsNullOrWhiteSpace($currentVersion)) { return $false }

  return ($lockedVersion -ne $currentVersion)
}

<#
.SYNOPSIS
  确保 Gemfile 中声明的 fastlane 可通过 Bundler 使用。
.OUTPUTS
  [bool] fastlane 可解析返回 true。
.NOTES
  Get-FastlaneBundleRoot 会优先查找 android\Gemfile，再查找仓库根 Gemfile；
  只有包含 gem 'fastlane' 的 Gemfile 会被采用。
#>
function Ensure-FastlaneBundle {
  $bundleRoot = Get-FastlaneBundleRoot
  if ([string]::IsNullOrWhiteSpace($bundleRoot)) {
    Write-Fail '未找到声明 fastlane 的 Gemfile'
    return $false
  }

  # bundle 由 install_2 安装；若刚在同一会话装完，PATH 可能尚未刷新，先从注册表重读再判断。
  if (-not (Get-CommandPath 'bundle')) {
    Sync-PathFromRegistry
  }
  if (-not (Require-Command 'bundle')) {
    Write-Host '请先运行 scripts\windows\install_2_ruby_bywin.ps1 安装 Ruby 与 Bundler。'
    Write-Host '若刚安装完，请关闭并重新打开 PowerShell 后再运行本脚本。'
    return $false
  }

  $gemfile = Join-Path $bundleRoot 'Gemfile'
  Write-Ok "使用 Gemfile：$gemfile"

  if (-not $CheckOnly) {
    # 选一个当前可连通的 RubyGems 国内镜像。
    $mirror = Select-GemMirror

    # 先清除可能残留的 mirror 配置（早期版本曾用 bundle config mirror 写入 .bundle/config）。
    # 残留的 BUNDLE_MIRROR__* 会让 Bundler 进入 mirror 兼容模式、改走旧式 dependency API
    # (rubygems.org/v1/dependencies -> 回落 api.rubygems.org)，反而绕开下面改写的 source。
    # unset 后才能让“改写 source 指向镜像”真正生效（走 compact index）。
    Invoke-NativeIn -Path $bundleRoot -Block {
      & bundle config unset --local mirror.https://rubygems.org 2>$null
    } | Out-Null
    $bundleConfig = Join-Path $bundleRoot '.bundle\config'
    if (Test-Path -LiteralPath $bundleConfig) {
      # bundle config unset 对带 fallback_timeout 等后缀的键清理不彻底，直接按行剔除所有 MIRROR 项。
      $kept = Get-Content -LiteralPath $bundleConfig | Where-Object { $_ -notmatch 'BUNDLE_MIRROR__' }
      Set-Content -LiteralPath $bundleConfig -Value $kept
    }

    # 关键：必须改写 Gemfile 的 source，而不是用 bundle config mirror。
    # 原因：mirror 配置只重定向 source 的 specs 拉取，但 Bundler 的依赖解析走的是
    # 独立的 compact index / dependency API（api.rubygems.org），mirror 拦不住它，
    # 国内直连 api.rubygems.org 会超时（实测如此）。把 source 直接指向镜像，
    # 整个解析（versions/info 端点）都走镜像，根本不碰 api.rubygems.org。
    # Gemfile 是入库文件，source 不能永久改，故 try/finally 用后即恢复。
    $gemfileBackup = $null
    if ($mirror) {
      $original = Get-Content -LiteralPath $gemfile -Raw
      if ($original -match 'source\s+["'']https://rubygems\.org["'']') {
        $gemfileBackup = $original
        $mirrorSource = $mirror.TrimEnd('/')
        $patched = $original -replace 'source\s+["'']https://rubygems\.org["'']', "source `"$mirrorSource`""
        Set-Content -LiteralPath $gemfile -Value $patched -NoNewline
        Write-Ok "已临时将 Gemfile source 指向镜像：$mirrorSource"
      }
    } else {
      Write-Warn '所有国内镜像均不可达，将直接使用 rubygems.org（可能较慢或超时）。'
      Write-Warn '若长时间卡住，请检查本机网络/防火墙/安全软件是否拦截了镜像站。'
    }

    try {
      # 对齐 lockfile 的 BUNDLED WITH 与当前 Bundler 版本，避免 Bundler 4.x
      # 见到 lockfile 锁的旧版后下载并切换到旧版重跑（慢且无谓）。
      if (Test-LockfileBundlerMismatch -BundleRoot $bundleRoot) {
        Write-Warn 'Gemfile.lock 的 Bundler 版本与当前不一致，正在对齐 ...'
        Invoke-NativeIn -Path $bundleRoot -Block { & bundle update --bundler } | Out-Null
      }

      $code = Invoke-NativeIn -Path $bundleRoot -Block { & bundle install }
    }
    finally {
      # 无论成败都恢复 Gemfile 原始 source，避免把镜像地址提交进 git。
      if ($null -ne $gemfileBackup) {
        Set-Content -LiteralPath $gemfile -Value $gemfileBackup -NoNewline
        Write-Ok 'Gemfile source 已恢复为 rubygems.org'
      }
    }

    if ($code -ne 0) {
      Write-Fail 'bundle install 执行失败'
      return $false
    }
  }

  $fastlane = Get-FastlaneCommand
  if ($fastlane) {
    Write-Ok "fastlane 命令：$($fastlane.Display)"
    return $true
  }

  Write-Fail 'bundle install 之后仍未找到 fastlane'
  return $false
}

Write-Banner 'Fastlane'

if (-not (Ensure-FastlaneBundle)) { exit 1 }
Write-Ok 'fastlane 已就绪'
