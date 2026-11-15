package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 统一按压「扩大回弹」的弹簧参数。
 *
 * 与底栏面板的 `pressProgress` 完全一致：临界阻尼（dampingRatio=1）、
 * 刚度 1000 —— 按下快速放大、松手平滑回弹，无过冲。
 */
val PressScaleSpec: SpringSpec<Float> = spring(
    dampingRatio = 1f,
    stiffness = 1000f,
)

/**
 * 通用「按下扩大、松手回弹」的 modifier。
 *
 * 用于**非玻璃采样**的控件（实心保存键、胶囊里的图标格）：
 * 它们没有「采样背景」这回事，直接用 graphicsLayer 缩放整块即可。
 *
 * 玻璃控件（InteractiveGlassPanel）走 layerBlock 缩放，避免背景跟着动，
 * 但弹簧参数同样用 [PressScaleSpec]，手感一致。
 *
 * @param inflateDp 按下时向外扩张的尺寸。与 InteractiveGlassPanel 的 pressInflate 同义。
 */
fun Modifier.pressScale(
    enabled: Boolean = true,
    inflateDp: Dp = 3.dp,
): Modifier = composed {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val inflatePx = with(LocalDensity.current) { inflateDp.toPx() }

    this
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
        }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                scope.launch { progress.animateTo(1f, PressScaleSpec) }
                waitForUpOrCancellation()
                scope.launch { progress.animateTo(0f, PressScaleSpec) }
            }
        }
}
