package com.lacrimosa.staria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 主题固定为浅色：设计要求全局白色背景，不跟随系统深色模式，
 * 也不启用动态取色（会破坏纯白基调）。
 *
 * 颜色全部定义在 [Color.kt]，这里只负责把它们交给 Material 的语义槽位。
 */
private val StariaColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = PaperWhite,
    background = PaperWhite,
    onBackground = Ink,
    surface = PaperWhite,
    onSurface = Ink,
    surfaceVariant = CardPaper,
    onSurfaceVariant = InkMuted,
    outline = Border,
    error = Danger,
)

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
