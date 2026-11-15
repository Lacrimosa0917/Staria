package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.lacrimosa.staria.ui.theme.GlassSpec
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.stariaGlassEffects
import kotlinx.coroutines.launch

/**
 * 带「按下放大」反馈的液态玻璃面板。
 *
 * ## 交互反馈的来历
 *
 * 这是 AndroidLiquidGlass 仓库 Interactive Glass Bottom Bar 一节给出的做法：
 * 按下时把面板放大一点点，松手回弹，模拟「手指压上玻璃、玻璃被按扁回弹」的手感。
 *
 * ## 关键实现细节：为什么必须用 layerBlock
 *
 * 仓库文档专门强调过这一点：如果按普通做法把 `scaleX/scaleY` 写在
 * `Modifier.graphicsLayer{...}` 里，**backdrop 会跟着一起放大**，
 * 于是玻璃里显示的内容相对位置就错了（看起来像背景在抖）。
 *
 * 正确做法是写进 `drawBackdrop(layerBlock = {...})`：
 * 它只变换「玻璃这一层」，采样到的背景仍然是原始尺寸与位置。
 *
 * ## 放大倍数的算法
 *
 * `(size.width + inflate * 2) / size.width` —— 让控件在视觉上向外扩张
 * 固定的 dp 数（而不是按比例放大），这样 44dp 的头像和 300dp 的胶囊
 * 按下去「变大的绝对值」是一致的，手感统一。
 *
 * ## 没有蒙层
 *
 * 材质只有模糊 + 折射 + 1dp 描边。可读性靠模糊把背景糊成色晕，
 * 不靠盖一层白。
 */
@Composable
fun InteractiveGlassPanel(
    backdrop: Backdrop?,
    shape: Shape,
    modifier: Modifier = Modifier,
    /** 按下时向外扩张的尺寸。仓库示例是 16dp，这里按小控件收到 4dp。 */
    pressInflate: Dp = 4.dp,
    /** 折射强度倍率。胶囊传小值，见 stariaGlassEffects。 */
    refractionScale: Float = 1f,
    enabled: Boolean = true,
    onClick: () -> Unit,
    /**
     * 玻璃层的额外垂直位移（px）。用于「本体平移」类动画（如新建按钮的向下惯性）。
     * 必须放进 layerBlock（只平移玻璃层、采样背景不动），
     * 若用外层 graphicsLayer.translationY 会把玻璃和背景错位、露出色块。
     */
    layerTranslationY: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    // 与底栏面板统一：临界阻尼（无过冲），刚度 1000，按下快速放大、松手平滑回弹
    val spec = PressScaleSpec

    val inflatePx = with(LocalDensity.current) { pressInflate.toPx() }

    // 降级底色在组合期取出（动态令牌）
    val fallbackColor = PaperWhite

    val body = if (backdrop != null) {
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = { stariaGlassEffects(refractionScale) },
                // 缩放 + 平移写在 layerBlock 里，背景采样才不会跟着动。
                // 内容用下方 graphicsLayer 同步，二者参数一致。
                layerBlock = {
                    val p = progress.value
                    if (p > 0f) {
                        val maxScale = if (size.width > 0f) {
                            (size.width + inflatePx * 2) / size.width
                        } else {
                            1f
                        }
                        val s = 1f + (maxScale - 1f) * p
                        scaleX = s
                        scaleY = s
                    }
                    translationY = layerTranslationY()
                },
            )
    } else {
        modifier.background(fallbackColor.copy(alpha = GlassSpec.FALLBACK_ALPHA), shape)
    }

    Box(
        modifier = body
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .pointerInput(enabled, spec) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    // 按下：放大
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { progress.animateTo(1f, spec) }

                    // 抬起或取消：回弹
                    waitForUpOrCancellation()
                    scope.launch { progress.animateTo(0f, spec) }
                }
            },
    ) {
        // 内容跟随玻璃层同步缩放 + 平移：倍率与位移和 layerBlock 完全一致。
        // 用 graphicsLayer 包住 content，图标/文字随面板一起扩大回弹、一起平移。
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val p = progress.value
                    if (p > 0f) {
                        val maxScale = if (size.width > 0f) {
                            (size.width + inflatePx * 2) / size.width
                        } else {
                            1f
                        }
                        val s = 1f + (maxScale - 1f) * p
                        scaleX = s
                        scaleY = s
                    }
                    translationY = layerTranslationY()
                },
        ) {
            content()
        }
    }
}

/** 便捷重载：自动从 CompositionLocal 取 backdrop。 */
@Composable
fun InteractiveGlassPanel(
    shape: Shape,
    modifier: Modifier = Modifier,
    pressInflate: Dp = 4.dp,
    refractionScale: Float = 1f,
    enabled: Boolean = true,
    onClick: () -> Unit,
    layerTranslationY: () -> Float = { 0f },
    content: @Composable BoxScope.() -> Unit,
) = InteractiveGlassPanel(
    backdrop = LocalGlassBackdrop.current,
    shape = shape,
    modifier = modifier,
    pressInflate = pressInflate,
    refractionScale = refractionScale,
    enabled = enabled,
    onClick = onClick,
    layerTranslationY = layerTranslationY,
    content = content,
)
