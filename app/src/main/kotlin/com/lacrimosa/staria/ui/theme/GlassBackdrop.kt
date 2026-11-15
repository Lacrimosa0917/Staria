package com.lacrimosa.staria.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.kyant.backdrop.Backdrop

/**
 * 全局背景层（液态玻璃的采样源）。
 *
 * 根布局用 `Modifier.layerBackdrop(...)` 把「页面内容 + 纯白底色」录进一个
 * GraphicsLayer，玻璃控件通过 `drawBackdrop` 重新绘制这块内容并施加模糊 / 折射。
 *
 * 用 CompositionLocal 下发，而不是逐层传参：
 * 底栏、头像、新建栏、二级页顶栏按钮、tooltip 分散在十来个文件里，
 * 逐层传会把它们的签名全部污染一遍。
 *
 * 为 null 表示「当前不在可玻璃化的环境」（例如单独的 Dialog 窗口）——
 * 此时控件应退回纸面样式，见 [com.lacrimosa.staria.ui.component.GlassPanel] 的调用方。
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }
