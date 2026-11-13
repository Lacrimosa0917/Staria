package com.lacrimosa.staria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 全局纯白背景（README：全局背景采用白色）。 */
val PaperWhite = Color(0xFFFFFFFF)

/** 墨色：正文与图标统一使用近黑，避免纯黑在 OLED 上过硬。 */
val Ink = Color(0xFF1A1A1A)

/** 次级文字 / 分隔线。 */
val InkMuted = Color(0xFF9A9A9A)
val HairLine = Color(0xFFEDEDED)

/**
 * 卡面：与图标母版底色一致（#FCFBF9），比纯白背景略暖一丝，
 * 让纸条卡片在白色背景上仍有边界感。
 */
val CardPaper = Color(0xFFFCFBF9)

private val StariaColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = PaperWhite,
    background = PaperWhite,
    onBackground = Ink,
    surface = PaperWhite,
    onSurface = Ink,
    surfaceVariant = CardPaper,
    onSurfaceVariant = InkMuted,
    outline = HairLine,
)

/**
 * 主题固定为浅色：设计要求全局白色背景，不跟随系统深色模式，
 * 也不启用动态取色（会破坏纯白基调）。
 */
@Composable
fun StariaTheme(
    typography: Typography = StariaTypography,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = StariaColorScheme,
        typography = typography,
        content = content,
    )
}
