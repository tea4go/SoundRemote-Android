package io.github.soundremote.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * 应用语言选项：AUTO 跟随系统；ZH/EN 强制切到中/英。
 *
 * 持久化时用 tag 字符串（"", "zh-CN", "en"）；空字符串表示自动。
 */
enum class AppLanguage(val tag: String) {
    AUTO(""),
    ZH("zh-CN"),
    EN("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: AUTO
    }
}

/**
 * 把选择的语言应用到进程，Compose 会通过 configuration change 自动重组。
 */
fun applyAppLanguage(language: AppLanguage) {
    val locales = if (language == AppLanguage.AUTO) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(language.tag)
    }
    AppCompatDelegate.setApplicationLocales(locales)
}
