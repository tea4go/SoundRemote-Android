# Scripts

Android release 构建、环境安装与版本管理脚本。

## 环境安装（Windows）

```powershell
# 1. 基础工具（winget、Windows Terminal、Node.js 可选）
.\scripts\windows\install_1_base_tools_bywin.ps1 -AddTools winget,terminal

# 2. Ruby（fastlane 依赖）
.\scripts\windows\install_2_ruby_bywin.ps1

# 3. fastlane
.\scripts\windows\install_3_fastlane_bywin.ps1

# 4. Android SDK（cmdline-tools、platform-tools、build-tools）
.\scripts\windows\install_4_android_sdk_bywin.ps1
```

## 构建 Android Release APK/AAB（Windows）

```powershell
# 1. 复制并编辑签名配置
copy scripts\local\android-signing.ps1.example scripts\local\android-signing.ps1
notepad scripts\local\android-signing.ps1
```

至少设置 `KEYSTORE_FILE_PASSWORD`。脚本默认使用以下路径和别名：

- keystore：`%USERPROFILE%\keystore\soundremote-keystore.jks`
- alias：`soundremote`

如需覆盖，在 `android-signing.ps1` 中添加：

```powershell
$env:KEYSTORE_FILE_PATH = 'C:\path\to\your.jks'
$env:KEYSTORE_FILE_ALIAS = 'your_alias'
```

还没有 keystore 文件？先创建：

```powershell
keytool -genkey -v `
  -keystore "$env:USERPROFILE\keystore\soundremote-keystore.jks" `
  -keyalg RSA -keysize 2048 -validity 10000 -alias soundremote
```

```powershell
# 2. 执行构建（versionCode 自动 +1）
.\scripts\windows\build_android_bywin.ps1 -Build

# 只检查环境，不构建
.\scripts\windows\build_android_bywin.ps1 -Check

# 发布到 GitHub Release（需要 gh CLI 且已登录）
.\scripts\windows\build_android_bywin.ps1 -Publish

# 发布到 Gitee Release
.\scripts\windows\build_android_bywin.ps1 -Publish -Platform gitee
```

## 版本号管理

版本号唯一权威在 `app/build.gradle.kts`。

```powershell
# 查看当前版本
.\scripts\windows\sync_version_bywin.ps1

# 手动设置版本
.\scripts\windows\sync_version_bywin.ps1 -VersionName 0.6.0 -VersionCode 13
```

## GitHub Actions 签名配置

在仓库 Settings → Secrets and variables → Actions 中创建：

- `ANDROID_KEYSTORE_BASE64`：keystore 文件的 Base64 编码
- `ANDROID_KEYSTORE_PASSWORD`：keystore 密码
- `ANDROID_KEYSTORE_KEY`：key alias（默认 `soundremote`）

生成 Base64 值（PowerShell）：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("$env:USERPROFILE\keystore\soundremote-keystore.jks")) | Set-Clipboard
```

## 卸载（Windows）

```powershell
.\scripts\windows\remove_1_android_sdk_bywin.ps1
.\scripts\windows\remove_2_fastlane_bywin.ps1
.\scripts\windows\remove_3_ruby_bywin.ps1
```
