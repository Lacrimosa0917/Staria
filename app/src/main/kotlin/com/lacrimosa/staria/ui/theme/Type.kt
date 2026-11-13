package com.lacrimosa.staria.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.R

/**
 * 刻本宋体（GuDianKeBenSong.ttf）。
 *
 * 按设计要求仅用于首页 slogan 与品牌性文字；其余一律使用系统默认字体。
 */
val KebenSong = FontFamily(
    Font(R.font.gudian_keben_song, weight = FontWeight.Normal),
)

/** 全局字体：不指定字族，即系统默认。 */
val StariaTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp),
)
