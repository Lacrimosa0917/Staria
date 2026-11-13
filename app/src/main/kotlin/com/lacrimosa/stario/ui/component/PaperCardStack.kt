package com.lacrimosa.stario.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.data.MAX_STACKED_NOTES
import com.lacrimosa.stario.data.PaperNote
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted
import kotlin.math.hypot
import kotlin.math.tanh

private val CardHairline = Color(0xFFE8E4DC)
private val SwapEasing = CubicBezierEasing(0.22f, 0.77f, 0.23f, 1f)

/** 与参考实现一致的橡皮筋：160 * tanh(delta / 160)。 */
private fun rubberBand(delta: Float): Float = 160f * tanh(delta / 160f)

/**
 * 纸条卡片堆叠。
 *
 * 交互参考 LeiBox「拖甩照片卡片 / Card Swap」
 * （https://leibox.pantoria.cn/docs/animations/card-swap）：
 * - 前排卡片可拖拽，位移经过橡皮筋衰减；松手未达阈值则回弹
 * - 点按、甩动（位移超阈值或速度够快）都会让前排卡片退到队尾，其余顺势前移
 * - 卡片按透视关系逐层缩小、向右上方错开
 *
 * 与参考实现的差异：
 * - 参考用 `skewY(6deg)` 倾斜纸面；Compose 无等价 shear 变换，
 *   这里改为拖拽时的轻微旋转，避免字形被拉变形
 * - 参考是照片轮播，这里展示摘录文字，卡面改为纸质样式
 *
 * @param notes 纸条列表，超出 [MAX_STACKED_NOTES] 的部分不展示
 */
@Composable
fun PaperCardStack(
    notes: List<PaperNote>,
    modifier: Modifier = Modifier,
) {
    val visible = remember(notes) { notes.take(MAX_STACKED_NOTES) }
    if (visible.isEmpty()) return

    // order 存放 visible 的下标，[0] 为最前面一张
    var order by remember(visible) { mutableStateOf(visible.indices.toList()) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        // 卡片尺寸随可用空间自适应：
        // 宽度按可用宽取比例，同时用可用高度反推上限，防止横屏/小窗时
        // 卡片被压扁导致文字与出处重叠
        val aspect = 1.36f
        val widthLimit = minOf(maxWidth * 0.56f, 224.dp)
        val heightLimit = maxHeight * 0.92f
        val cardHeight = minOf(widthLimit * aspect, heightLimit)
        val cardWidth = cardHeight / aspect
        val stepX = cardWidth * 0.115f
        val stepY = cardHeight * 0.070f

        val count = order.size
        // 让整个堆叠视觉居中：把总偏移的一半反抵消
        val last = (count - 1).toFloat()
        val shiftX = stepX * (-last / 2f)
        val shiftY = stepY * (last / 2f)

        // 与参考实现同量级的阈值
        val tapSlopPx = with(density) { 6.dp.toPx() }
        val flingDistancePx = with(density) { 70.dp.toPx() }
        val fastDistancePx = with(density) { 24.dp.toPx() }
        val flingSpeed = 0.6f // px / ms

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 从后往前绘制，保证前排覆盖后排
            for (slot in (count - 1) downTo 0) {
                val noteIndex = order[slot]
                val note = visible[noteIndex]
                val isFront = slot == 0

                val targetX = with(density) { (shiftX + stepX * slot.toFloat()).toPx() }
                val targetY = with(density) { (shiftY - stepY * slot.toFloat()).toPx() }
                val targetScale = 850f / (850f + slot * 70f)

                val animX by animateFloatAsState(
                    targetValue = targetX,
                    animationSpec = tween(durationMillis = 540, easing = SwapEasing),
                    label = "card-x-$noteIndex",
                )
                val animY by animateFloatAsState(
                    targetValue = targetY,
                    animationSpec = tween(durationMillis = 540, easing = SwapEasing),
                    label = "card-y-$noteIndex",
                )
                val animScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "card-scale-$noteIndex",
                )

                // 拖动中的前排卡片直接跟手，不做补间
                val offsetX = if (isFront) dragOffset.x else 0f
                val offsetY = if (isFront) dragOffset.y else 0f

                Surface(
                    modifier = Modifier
                        .size(cardWidth, cardHeight)
                        .graphicsLayer {
                            translationX = animX + offsetX
                            translationY = animY + offsetY
                            scaleX = animScale
                            scaleY = animScale
                            rotationZ = if (isFront && dragging) offsetX * 0.02f else 0f
                        }
                        .then(
                            if (isFront) {
                                Modifier.pointerInput(order, visible, tapSlopPx, flingDistancePx) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val startTime = System.currentTimeMillis()
                                        var total = Offset.Zero
                                        dragging = true
                                        dragOffset = Offset.Zero

                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            val delta = change.positionChange()
                                            if (delta != Offset.Zero) {
                                                total += delta
                                                dragOffset = Offset(
                                                    x = rubberBand(total.x),
                                                    y = rubberBand(total.y),
                                                )
                                                change.consume()
                                            }
                                            if (!change.pressed) break
                                        }

                                        val distance = hypot(total.x.toDouble(), total.y.toDouble()).toFloat()
                                        val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                                        val speed = distance / elapsed

                                        val shouldSwap = distance < tapSlopPx ||
                                            distance > flingDistancePx ||
                                            (speed > flingSpeed && distance > fastDistancePx)

                                        dragging = false
                                        dragOffset = Offset.Zero
                                        if (shouldSwap && order.size > 1) {
                                            haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                            order = order.drop(1) + order.first()
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            },
                        ),
                    shape = RoundedCornerShape(14.dp),
                    color = CardPaper,
                    border = BorderStroke(1.dp, CardHairline),
                    shadowElevation = if (isFront) 10.dp else 3.dp,
                ) {
                    // 文字只在前排完全显示：后排只露出纸边，可避免错开的边缘
                    // 出现被裁断的字，同时减轻绘制量
                    val textAlpha by animateFloatAsState(
                        targetValue = if (isFront) 1f else 0f,
                        animationSpec = tween(durationMillis = 220),
                        label = "card-text-$noteIndex",
                    )
                    if (textAlpha > 0.01f) {
                        Box(modifier = Modifier.graphicsLayer { alpha = textAlpha }) {
                            NoteContent(note = note)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteContent(note: PaperNote) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 正文占据卡片中部，超出时省略
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = note.text,
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(10.dp))
        // 出处贴在正文下方
        Text(
            text = "— ${note.source}",
            color = InkMuted,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
