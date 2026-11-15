package com.lacrimosa.staria.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import com.lacrimosa.staria.data.DarkMode

/** 深色模式三档，由根布局按设置项 + 系统亮度解析后下发。 */
val LocalDarkMode = compositionLocalOf { DarkMode.FollowSystem }

/**
 * 主题装配。
 *
 * 浅色固定为白纸墨线；深色由 [LocalStariaColors] 的深色表驱动。
 * Material 的语义槽位只做兜底，真正的颜色全部走 [Color.kt] 里的令牌。
 */
@Composable
fun StariaTheme(
    darkMode: DarkMode = DarkMode.FollowSystem,
    typography: Typography = StariaTypography,
    content: @Composable () -> Unit,
) {
    // 实际是否深色：打开强制开、关闭强制关、跟随系统读系统亮度
    val isDark = when (darkMode) {
        DarkMode.On -> true
        DarkMode.Off -> false
        DarkMode.FollowSystem -> isSystemInDarkTheme()
    }

    val colors = if (isDark) DarkStariaColors else LightStariaColors
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = colors.ink,
            onPrimary = colors.pageBackground,
            background = colors.pageBackground,
            onBackground = colors.ink,
            surface = colors.cardPaper,
            onSurface = colors.ink,
            surfaceVariant = colors.cardPaper,
            onSurfaceVariant = colors.inkMuted,
            outline = colors.border,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.ink,
            onPrimary = colors.paperWhite,
            background = colors.pageBackground,
            onBackground = colors.ink,
            surface = colors.cardPaper,
            onSurface = colors.ink,
            surfaceVariant = colors.cardPaper,
            onSurfaceVariant = colors.inkMuted,
            outline = colors.border,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkMode,
        LocalStariaColors provides colors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}
