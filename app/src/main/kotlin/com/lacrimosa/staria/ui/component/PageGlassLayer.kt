package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PageBackground

/**
 * 二级页的玻璃采样层。
 *
 * ## 为什么需要它
 *
 * 根布局的 `glassBackdrop` 只录制了**四个 tab 的内容**
 * （`StariaApp.kt` 里 `layerBackdrop` 包住的是 `Crossfade` 那一层）。
 * 二级页（随笔查看 / 编辑 / 星尘 / 设置）渲染在那一层**之外**，
 * 所以它们的玻璃圆钮通过 `LocalGlassBackdrop` 拿到的仍是 tab 的 backdrop，
 * 于是「在设置页按返回键，圆钮里透出的是首页的卡片」。
 *
 * ## 两个槽为什么必须分开
 *
 * [content] 会被录进 backdrop，[chrome] 不会。必须这样分，否则会踩
 * 交接文档 §7.1 那个坑：
 *
 * 玻璃控件的绘制方式是「重新绘制 backdrop 录到的内容」。如果玻璃控件自己
 * 也在被录的范围里，它重画的那份内容里又有它自己 → 每画一次多一层
 * `RenderNode::prepareTreeImpl`，实机上直接崩（曾实测 511 层递归后 SIGSEGV）。
 *
 * 所以布局必须是：
 * ```
 * Box {
 *     Box(layerBackdrop) { content }   ← 被录制：页面正文、卡片、列表
 *     chrome                            ← 不被录制：玻璃圆钮、玻璃胶囊
 * }
 * ```
 * 玻璃控件虽然画在录制层之外，但通过 [CompositionLocalProvider] 拿到的是
 * 本次录制的 backdrop，因此透出的是**本页内容**。
 *
 * ## 用法
 *
 * ```
 * PageGlassLayer(
 *     content = { Column { ...正文... } },
 *     chrome = {
 *         Box(Modifier.align(Alignment.TopStart)) { GlassCircleButton(...) }
 *     },
 * )
 * ```
 *
 * @param content 需要被玻璃透出的页面内容（不含任何玻璃控件）
 * @param chrome 悬浮在本页之上的玻璃控件；它们能采样到 [content] 的录制结果
 */
@Composable
fun PageGlassLayer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
    chrome: @Composable BoxScope.() -> Unit,
) {
    // 底色在 Composable 作用域先取出（动态令牌），再闭包捕获进绘制块。
    // 用 key 包裹：切深色模式时底色变化，强制重建 backdrop。
    val baseColor = PageBackground
    val backdrop = key(baseColor) {
        rememberLayerBackdrop {
            // 必须先铺底色：不铺的话，没有内容覆盖的区域在玻璃里会露出透明像素
            drawRect(baseColor)
            drawContent()
        }
    }

    CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PageBackground),
        ) {
            // 只录制内容层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop),
                content = content,
            )

            // 玻璃层留在录制范围之外，避免递归
            chrome()
        }
    }
}
