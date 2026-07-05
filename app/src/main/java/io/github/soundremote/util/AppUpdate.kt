package io.github.soundremote.util

/** 更新源枚举，与 Delphi 项目相同（GitHub / Gitee 双源） */
enum class UpdateSource(val tag: String) {
    GITEE("gitee"),
    GITHUB("github");

    companion object {
        fun fromTag(tag: String?): UpdateSource = entries.firstOrNull { it.tag == tag } ?: GITEE
    }
}

/**
 * 检查更新的结果。
 *
 * @property hasUpdate 是否有比当前更新的版本
 * @property versionName 远端 versionName（如 0.6.0）
 * @property versionCode 远端 versionCode（整数）
 * @property downloadUrl APK 下载地址
 * @property releaseNotes 更新说明（release body）
 */
data class UpdateInfo(
    val hasUpdate: Boolean,
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
) {
    companion object {
        val NONE = UpdateInfo(false, "", 0, "", "")
    }
}
