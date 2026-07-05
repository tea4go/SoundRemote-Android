# Scripts

Android release 构建、环境安装与版本管理脚本。

## 环境安装（Windows）

```powershell
# 1. 基础工具（winget/Windows Terminal 可选，jvms + JDK 17 必需）
.\scripts\windows\install_1_base_tools_bywin.ps1 -AddTools winget,terminal,jvms,jdk17

# 或只装必需的
.\scripts\windows\install_1_base_tools_bywin.ps1 -AddTools jvms,jdk17

# 2. Android SDK（cmdline-tools、platform-tools、build-tools）
.\scripts\windows\install_4_android_sdk_bywin.ps1
```

安装完 JDK 17 后请**重新打开终端**，让新的 java 生效。

**可用工具项：**
- `winget` — Windows 包管理器
- `terminal` — Windows Terminal
- `store` — Microsoft Store
- `jvms` — JDK Version Manager（用于统一管理 JDK 版本）
- `jdk17` — 通过 jvms 安装 openjdk-17.0.2（本项目 Gradle 工具链要求）

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
# 卸载 Android SDK
.\scripts\windows\remove_1_android_sdk_bywin.ps1

# 卸载 JDK（通过 jvms）
jvms remove openjdk-17.0.2
```
