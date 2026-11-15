package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize
import kotlinx.coroutines.launch

/**
 * 二级页顶栏的圆形图标按键（液态玻璃，带按压反馈）。
 *
 * 原先随笔查看 / 编辑 / 星尘 / 设置四个页面各写了一份 38dp 圆钮。
 *
 * @param filled 实心主操作（如保存键）：铺墨色底，保留原有主次关系。
 *   实心键是「有内容」的，不参与玻璃折射，否则墨色块会被折射带切出色阶。
 */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = SecondaryButtonSize,
    iconSize: Dp = 17.dp,
    filled: Boolean = false,
    enabled: Boolean = true,
    fillColor: Color = Ink,
) {
    if (filled) {
        // 实心键：默认墨色，保存键传入主题色；白图标，不走玻璃。
        // 按压反馈与玻璃键一致（扩大回弹），用 pressScale 统一。
        // 顺序关键：pressScale（graphicsLayer）必须在 background **之前**，
        // 这样实心圆背景和图标一起缩放，否则只有图标在动、本体不动。
        Box(
            modifier = modifier
                .size(size)
                .pressScale(enabled = enabled)
                .background(fillColor, CircleShape)
                .clickableNoIndication(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(iconSize),
                tint = PaperWhite,
            )
        }
    } else {
        InteractiveGlassPanel(
            backdrop = LocalGlassBackdrop.current,
            shape = CircleShape,
            enabled = enabled,
            onClick = onClick,
            pressInflate = 3.dp,
            modifier = modifier.size(size),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(iconSize),
                    tint = Ink,
                )
            }
        }
    }
}

/**
 * 胶囊按钮（撤回重做 / 星尘批量恢复共用）。
 *
 * 圆角等于高度的一半，所以传较小的折射倍率 —— 否则 lens 会在两端
 * 拉出弧形折射带（见 stariaGlassEffects 的说明）。
 */
@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    height: Dp = SecondaryButtonSize,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val inflatePx = with(LocalDensity.current) { 3.dp.toPx() }

    GlassPanel(
        backdrop = LocalGlassBackdrop.current,
        shape = RoundedCornerShape(height / 2),
        // 圆角 = 高度/2，比圆钮的半径大，按底栏同样的比例收一下
        refractionScale = 0.6f,
        edgeHighlight = 1.dp,
        // 胶囊外壳带「本体扩大回弹」：layerBlock 缩放玻璃层，
        // 内容 Row 用 graphicsLayer 同步缩放，二者倍率一致。
        layerBlock = {
            val p = press.value
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
        },
        modifier = modifier
            .height(height)
            // 只检测按下/抬起用于缩放，不消费事件（requireUnconsumed=false、
            // waitForUpOrCancellation 不消费），内部图标格的点击不受影响。
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { press.animateTo(1f, PressScaleSpec) }
                    waitForUpOrCancellation()
                    scope.launch { press.animateTo(0f, PressScaleSpec) }
                }
            },
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .height(height)
                .wrapContentWidth()
                .graphicsLayer {
                    val p = press.value
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
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            content = content,
        )
    }
}

/** 胶囊里的一个图标格。 */
@Composable
fun GlassPillIcon(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    height: Dp = SecondaryButtonSize,
    cellWidth: Dp = 36.dp,
) {
    Box(
        modifier = Modifier
            .width(cellWidth)
            .height(height)
            .clickableNoIndication(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(17.dp),
            // 不可用时压淡，让「有没有东西可撤回」一眼可见
            tint = if (enabled) Ink else Ink.copy(alpha = 0.28f),
        )
    }
}
