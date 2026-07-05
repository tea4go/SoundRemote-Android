package io.github.soundremote.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 快捷键行的配色调板：整行底色 content + 顶部标题栏底色 title。
 * 参考 Delphi 项目里的 6 色循环方案（绿/黄/粉/紫/灰/蓝）。
 *
 * 调用方按热键 colorIndex 取色：
 * - 0..5 直接取对应色
 * - -1（[[Hotkey.COLOR_INDEX_AUTO]]）按行位置轮询：`idx % HotkeyPalettes.size`
 */
data class HotkeyPalette(val content: Color, val title: Color)

val HotkeyPalettes: List<HotkeyPalette> = listOf(
    HotkeyPalette(Color(0xFFC5FCAA), Color(0xFFA6FA91)),  // 绿
    HotkeyPalette(Color(0xFFFCF3A7), Color(0xFFFAE961)),  // 黄
    HotkeyPalette(Color(0xFFF5C9C8), Color(0xFFF2B5B4)),  // 粉
    HotkeyPalette(Color(0xFFBACAFB), Color(0xFFA1B7F8)),  // 紫
    HotkeyPalette(Color(0xFFEEEEEE), Color(0xFFDADADA)),  // 灰
    HotkeyPalette(Color(0xFFBEF2FD), Color(0xFFA5EEFD)),  // 蓝
)

/**
 * 按 colorIndex 解析出实际调色板；-1 或越界时回退到 fallbackIndex % palettes.size。
 */
fun resolveHotkeyPalette(colorIndex: Int, fallbackIndex: Int): HotkeyPalette {
    val idx = colorIndex.takeIf { it in HotkeyPalettes.indices }
        ?: (fallbackIndex.mod(HotkeyPalettes.size))
    return HotkeyPalettes[idx]
}
