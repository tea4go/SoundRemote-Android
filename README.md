# SoundRemote android client

> **本仓库为 fork 版本**
> Fork 自 [SoundRemote/client-android](https://github.com/SoundRemote/client-android)（作者 Aleksandr Shipovskii，GPL v3）。
> 本 fork 由 [tea4go](https://github.com/tea4go) 于 2026 年起进行中文本地化与功能增强，遵循相同的 **GNU GPL v3** 协议。
>
> 主要改动概览：
> - 完整中文本地化（`values-zh-rCN`），应用内可实时切换 中文 / English / 跟随系统
> - 主界面 UI 重构：IP 输入框布局优化、快捷键分组配色（6 色调板，可按热键单独选色）、分隔线、字体加大加粗
> - 设置项拆分为"值 + 帮助提示"两级显示，帮助信息更接近设计意图
> - 快捷键名称长度限制（≈12 字母或 6 汉字）
> - Windows PowerShell 构建脚本适配（`jvms` 装 JDK 17、Gradle 直接构建、去除 Ionic/fastlane 依赖）
>
> 原项目主页：https://github.com/SoundRemote/client-android

An Android app that, when paired up
with [SoundRemote server](https://github.com/SoundRemote/server-windows), allows to:

- Capture and stream audio from a PC to an Android device
- Execute keyboard commands on the PC remotely from the Android app either directly through its UI
  or by binding to certain events such as device shaking or incoming phone call
- Control media on the PC through the Android media notification

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
alt="Get it on F-Droid"
height="80">](https://f-droid.org/packages/io.github.soundremote/)

Or download the latest APK from
the [Releases Section](https://github.com/SoundRemote/client-android/releases/latest).

## Screenshots

<img src="metadata/en-US/images/phoneScreenshots/1.png" alt="Home screen" title="Home screen" width="250"/>
⠀
<img src="metadata/en-US/images/phoneScreenshots/2.png" alt="Events screen" title="Events screen" width="250"/>
⠀
<img src="metadata/en-US/images/phoneScreenshots/3.png" alt="Notification" title="Notification" width="250"/>
