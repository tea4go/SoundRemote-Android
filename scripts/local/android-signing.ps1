# 将此文件复制为 scripts\local\android-signing.ps1，并填入本地密钥信息。
# 本文件由 scripts\windows.bak\build_android_bywin.ps1 以点源方式引入（PowerShell 语法）。
# 请勿将真实的 android-signing.ps1 或任何密钥库密码提交到 git。

$env:KEYSTORE_FILE_PASSWORD = "testing123"

# Gitee 发布令牌（仅 -Publish -Platform gitee 时需要）：
# 获取令牌：https://gitee.com/personal_access_tokens
$env:GITEE_TOKEN = "8bffe81a0f5cd62248c1deadbcdb48fc"

# 可选覆盖项（默认值来自 android\app\build.gradle 及以下路径）：
# $env:BUILD_NUMBER = "3001"
# $env:VERSION_NUMBER = "3.0.0"
# $env:KEYSTORE_FILE_PATH = "$env:USERPROFILE\keystore\macro-deck-client-keystore.jks"
# $env:KEYSTORE_FILE_ALIAS = "soundremote"

# Create keystore if alias does not already exist.
$_ksPath  = "$env:USERPROFILE\keystore\soundremote-keystore.jks"
$_ksAlias = 'soundremote'
$_ksPass  = $env:KEYSTORE_FILE_PASSWORD

$_aliasExists = $false
if (Test-Path -LiteralPath $_ksPath) {
    $_prevEA = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & keytool -list -keystore $_ksPath -alias $_ksAlias -storepass $_ksPass *>&1 | Out-Null
    $_aliasExists = ($LASTEXITCODE -eq 0)
    $ErrorActionPreference = $_prevEA
}

if ($_aliasExists) {
    Write-Host "  Keystore alias '$_ksAlias' already exists:" -ForegroundColor Cyan
    $_prevEA = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $_info = @(& keytool -list -v -keystore $_ksPath -alias $_ksAlias -storepass $_ksPass 2>&1)
    $ErrorActionPreference = $_prevEA

    $_masked = $_info | ForEach-Object {
        $line = [string]$_
        # Mask fingerprint hex tails so only first 6 chars remain visible
        $line = $line -replace '((?:SHA1|SHA-1|SHA256|SHA-256|MD5)[^:]*:\s*)([0-9A-Fa-f:]{6})[0-9A-Fa-f:]+', '$1$2:...'
        # Mask serial number tail similarly
        $line = $line -replace '(Serial number:\s*)([0-9A-Fa-f]{4})[0-9A-Fa-f]+', '$1$2...'
        $line
    }
    $_masked | Where-Object { $_ -match '^\s*(Alias|Entry type|Creation date|Valid from|Owner|Issuer|SHA|MD5|Serial)' } |
        ForEach-Object { Write-Host "  $_" }
} else {
    keytool -genkeypair -v `
       -keystore $_ksPath `
       -storetype JKS `
       -keyalg RSA -keysize 2048 -validity 10000 `
       -alias $_ksAlias
}