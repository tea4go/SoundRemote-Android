# 将此文件复制为 scripts\local\android-signing.ps1，并填入本地密钥信息。
# 本文件由 scripts\windows.bak\build_android_bywin.ps1 以点源方式引入（PowerShell 语法）。
# 请勿将真实的 android-signing.ps1 或任何密钥库密码提交到 git。

$env:KEYSTORE_FILE_PASSWORD = "testing123"

# 可选覆盖项（默认值来自 android\app\build.gradle 及以下路径）：
# $env:BUILD_NUMBER = "3001"
# $env:VERSION_NUMBER = "3.0.0"
# $env:KEYSTORE_FILE_PATH = "$env:USERPROFILE\keystore\macro-deck-client-keystore.jks"
# $env:KEYSTORE_FILE_ALIAS = "macro-deck-client"

# 还没有密钥库？先创建默认密钥库：
#   keytool -genkey -v -keystore "$env:USERPROFILE\keystore\macro-deck-client-keystore.jks" `
#     -keyalg RSA -keysize 2048 -validity 10000 -alias macro-deck-client
keytool -genkeypair -v `
   -keystore "$env:USERPROFILE\keystore\macro-deck-client-keystore.jks" `
   -storetype JKS `
   -keyalg RSA -keysize 2048 -validity 10000 `
   -alias macro-deck-client