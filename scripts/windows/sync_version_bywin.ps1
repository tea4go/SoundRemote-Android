<#
.SYNOPSIS
  同步版本号（可选先设置版本），不构建。以 app/build.gradle.kts 为源。
.DESCRIPTION
  版本号唯一权威是 app/build.gradle.kts。
  - 不带参数：读 build.gradle.kts 现值并输出。
  - 带 -VersionName / -VersionCode：先写入 build.gradle.kts，再输出（用于手动设定版本）。
  与构建解耦，适合”只想设定版本、暂不打包”的场景。
.PARAMETER VersionName
  要设置的版本名（如 0.6.0）。省略则沿用 build.gradle.kts 现值。
.PARAMETER VersionCode
  要设置的版本号（正整数）。省略则沿用 build.gradle.kts 现值。
.PARAMETER Help
  显示本帮助后退出，不执行任何操作。
.EXAMPLE
  .\sync_version_bywin.ps1
  读 build.gradle.kts 现值并输出。
.EXAMPLE
  .\sync_version_bywin.ps1 -VersionName 0.6.0 -VersionCode 13
  把版本设为 0.6.0(13) 写回 build.gradle.kts。
.EXAMPLE
  .\sync_version_bywin.ps1 -Help
#>
param(
  [string]$VersionName,
  [int]$VersionCode,
  [switch]$Help
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '_common.ps1')

<#
.SYNOPSIS
  打印脚本用法。
#>
function Write-Usage {
  Write-Host ""
  Write-Banner -Title '版本号管理（build.gradle.kts）' -Color Cyan
  Write-Host ""
  Write-Host "用法：" -ForegroundColor Cyan
  Write-Host "  .\sync_version_bywin.ps1                                    读 build.gradle.kts 现值并输出"
  Write-Host "  .\sync_version_bywin.ps1 -VersionName <名> -VersionCode <号>  设定版本后写回"
  Write-Host "  .\sync_version_bywin.ps1 -Help                              显示本帮助"
  Write-Host ""
  Write-Host "参数：" -ForegroundColor Cyan
  Write-Host "  -VersionName   版本名（如 0.6.0），省略则沿用 build.gradle.kts 现值"
  Write-Host "  -VersionCode   版本号（正整数），省略则沿用 build.gradle.kts 现值"
  Write-Host "  -Help          显示本帮助后退出"
  Write-Host ""
  Write-Host "示例：" -ForegroundColor Cyan
  Write-Host "  .\sync_version_bywin.ps1"
  Write-Host "  .\sync_version_bywin.ps1 -VersionName 0.6.0 -VersionCode 13"
  Write-Host ""
}

if ($Help) {
  Write-Usage
  exit 0
}

Write-Banner '版本号管理（build.gradle.kts）'

# 可选：先把传入的版本写回 build.gradle.kts（不传则保持现值）
if ($PSBoundParameters.ContainsKey('VersionName')) {
  if (-not (Set-AndroidGradleValue 'versionName' $VersionName)) { exit 1 }
}
if ($PSBoundParameters.ContainsKey('VersionCode')) {
  if ($VersionCode -lt 1) { Write-Fail "VersionCode 必须为正整数（当前：$VersionCode）"; exit 1 }
  if (-not (Set-AndroidGradleValue 'versionCode' $VersionCode)) { exit 1 }
}

Sync-AppVersion
Write-Host ''
Write-Ok '版本同步完成'
