# 本文件已按项目配置，由 scripts\windows\build_android_bywin.ps1 以点源方式引入。
# 请勿将此文件及任何密钥库密码提交到 git。

$env:KEYSTORE_FILE_PASSWORD = "testing123"

# 可选覆盖项（默认值如下）：
# $env:KEYSTORE_FILE_PATH = "$env:USERPROFILE\keystore\soundremote-keystore.jks"
# $env:KEYSTORE_FILE_ALIAS = "soundremote"

# 还没有密钥库？先创建：
#   keytool -genkey -v -keystore "$env:USERPROFILE\keystore\soundremote-keystore.jks" `
#     -keyalg RSA -keysize 2048 -validity 10000 -alias soundremote
