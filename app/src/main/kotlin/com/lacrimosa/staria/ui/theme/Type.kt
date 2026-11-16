package com.lacrimosa.staria.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.R

/**
 * 品牌字体：郑庆科-南北词宫谱宋体（ZhengQingKeNanBeiCiGongPuSongTi）。
 *
 * 按设计要求仅用于首页 slogan 与品牌性文字；其余一律使用系统默认字体。
 *
 * 字体文件已做过**子集化**：原始 52.6 MB（21555 字形）裁到只保留本项目
 * 实际用到的 180 个字符（232 字形），体积降到约 126 KB。
 *
 * ⚠️ 因此**新增品牌文案时必须同步扩充子集**，否则新字会回退成系统字体、
 * 与其他字符字形不一致。扩充步骤见 `tools/subset_font.py`。
 *
 * 版权：Copyright (c) 2025 by zhengqingke / 济南一字百炼艺术设计有限公司。
 * 字体内置声明「本字体商业使用免费」，随包分发时请保留字体内部的 name 表。
 */
val KebenSong = FontFamily(
    Font(R.font.zqknbcgpst_subset, weight = FontWeight.Normal),
)

/** 全局字体：不指定字族，即系统默认。 */
val StariaTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 13.sp),
)
