package com.lacrimosa.staria.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 内容层需要避让的上下留白。
 *
 * 上下栏是悬浮层，但计算留白时必须把**系统栏**也算进去：
 * 头像与底栏自身带 statusBarsPadding / systemBarsPadding，
 * 只按控件高度加间距会短一截，卡片仍会压到它们身上。
 *
 * 由根布局在运行时按真实窗口 inset 计算后提供，
 * 页面只消费，不再自己写死常量。
 */
data class ContentInsets(
    /** 滚到最上方时，首项相对屏幕顶部的起始偏移。 */
    val top: Dp = 0.dp,
    /** 滚到最下方时，末项相对屏幕底部的结束偏移。 */
    val bottom: Dp = 0.dp,
)

val LocalContentInsets = compositionLocalOf { ContentInsets() }

/** 头像直径。根布局用它换算顶部留白，页面不要单独引用。 */
val AvatarSize: Dp = 44.dp

/** 头像距状态栏的下方间距。 */
val AvatarTopPadding: Dp = 14.dp

/** 悬浮底栏距屏幕（含导航栏）的下方间距。 */
val BarBottomPadding: Dp = 18.dp

/** 首尾内容与悬浮控件之间再留一点呼吸感。 */
val ScrollEdgeGap: Dp = 8.dp
