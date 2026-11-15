package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock

/**
 * 阻尼拖拽动画：底栏滑块的「跟手 + 回弹」内核。
 *
 * 移植自 AndroidLiquidGlass Catalog 的 `DampedDragAnimation`
 * （Apache-2.0，经 MeloX-Android 的 `PublicDampedDragAnimation` 转写）：
 * https://github.com/Kyant0/AndroidLiquidGlass
 *
 * ## 它管四件事
 *
 * 1. **位置**：[value] 是「停在第几格」的连续值（0f..n-1f）。
 *    点击时用临界阻尼弹簧滑过去；拖动时完全跟手。
 * 2. **按压进度**：[pressProgress] 0→1，滑块放大、玻璃折射增强都靠它。
 * 3. **缩放**：[scaleX]/[scaleY] 分别用不同阻尼（0.6 / 0.7），
 *    所以按下时先横向摊开、再纵向压扁，像水珠被压了一下。
 * 4. **速度**：[velocity] 供外层做「甩动时横向拉长」的变形。
 *
 * ## 为什么松手要等一会儿才回弹
 *
 * [release] 里先 `withFrameNanos` 让位一帧，再等位置弹簧基本落定
 * （差值小于整段范围的 2.5%）才把按压缩放收回去。
 * 这个延后是故意的：如果松手立刻缩回，滑块会在滑行途中「先缩小再滑」，
 * 看起来像被弹开而不是被吸过去。等落定再收，视觉上是「滑到位后回弹一下」。
 */
class StariaDampedDragAnimation(
    private val animationScope: CoroutineScope,
    initialValue: Float,
    val valueRange: ClosedRange<Float>,
    visibilityThreshold: Float,
    private val pressedScale: Float,
    val onDragStopped: StariaDampedDragAnimation.() -> Unit = {},
    val onTap: StariaDampedDragAnimation.(position: Offset) -> Unit = {},
    val onDrag: StariaDampedDragAnimation.(size: IntSize, dragAmount: Offset) -> Unit,
) {

    /** 位置弹簧：临界阻尼，不带过冲 —— 拖拽松手和点击切换都要「稳」。 */
    private val valueAnimationSpec = spring(1f, 1000f, visibilityThreshold)

    /**
     * 呼出专用弹簧：带过冲，让滑块「Q 弹」地落格。
     * 只用于程序化选中变化，不用于拖拽。
     */
    private val bouncyValueAnimationSpec = spring(0.6f, 380f, visibilityThreshold)

    private val velocityAnimationSpec = spring(0.5f, 300f, visibilityThreshold * 10f)
    private val pressProgressAnimationSpec = spring(1f, 1000f, 0.001f)

    // 横纵分开给阻尼与刚度：按下时先摊开再压扁，比等比缩放更像液体
    private val scaleXAnimationSpec = spring(0.6f, 250f, 0.001f)
    private val scaleYAnimationSpec = spring(0.7f, 250f, 0.001f)

    private val valueAnimation = Animatable(initialValue, visibilityThreshold)
    private val velocityAnimation = Animatable(0f, 5f)
    private val pressProgressAnimation = Animatable(0f, 0.001f)
    private val scaleXAnimation = Animatable(1f, 0.001f)
    private val scaleYAnimation = Animatable(1f, 0.001f)

    private val mutatorMutex = MutatorMutex()
    private val velocityTracker = VelocityTracker()
    private var downPosition = Offset.Zero
    private var movedDuringGesture = false

    val value: Float get() = valueAnimation.value
    val targetValue: Float get() = valueAnimation.targetValue
    val pressProgress: Float get() = pressProgressAnimation.value
    val scaleX: Float get() = scaleXAnimation.value
    val scaleY: Float get() = scaleYAnimation.value
    val velocity: Float get() = velocityAnimation.value

    val modifier: Modifier = Modifier.pointerInput(Unit) {
        awaitEachDragGesture(
            onDragStart = { down ->
                downPosition = down
                movedDuringGesture = false
                press()
            },
            onDragEnd = {
                // 位移为 0 视作「点击」而不是「拖拽」
                if (movedDuringGesture) onDragStopped() else onTap(downPosition)
                release()
            },
            onDragCancel = {
                onDragStopped()
                release()
            },
        ) { change, dragAmount ->
            if (dragAmount != Offset.Zero) movedDuringGesture = true
            onDrag(size, dragAmount)
            change.consume()
        }
    }

    /** 按下：放大 + 折射增强同时开始。 */
    fun press() {
        velocityTracker.resetTracking()
        animationScope.launch {
            launch { pressProgressAnimation.animateTo(1f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(pressedScale, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(pressedScale, scaleYAnimationSpec) }
        }
    }

    /** 松手：等位置弹簧落定后再收起按压缩放。 */
    fun release() {
        animationScope.launch {
            withFrameNanos { }
            if (valueAnimation.value != valueAnimation.targetValue) {
                val threshold = (valueRange.endInclusive - valueRange.start) * 0.025f
                snapshotFlow { valueAnimation.value }
                    .filter { abs(it - valueAnimation.targetValue) < threshold }
                    .first()
            }
            launch { pressProgressAnimation.animateTo(0f, pressProgressAnimationSpec) }
            launch { scaleXAnimation.animateTo(1f, scaleXAnimationSpec) }
            launch { scaleYAnimation.animateTo(1f, scaleYAnimationSpec) }
        }
    }

    /** 跟手更新（拖动期间调用）。 */
    fun updateValue(value: Float) {
        val target = value.coerceIn(valueRange)
        animationScope.launch {
            launch { valueAnimation.animateTo(target, valueAnimationSpec) { updateVelocity() } }
        }
    }

    /** 点击切换：临界阻尼滑过去，同时给一次按压反馈。 */
    fun animateToValue(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                press()
                val target = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(target, valueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
                release()
            }
        }
    }

    /**
     * 带过冲地滑到目标格（「呼出」用）。
     *
     * 刻意不调 [press]/[release]：程序化切换没有手指，
     * 放大后滑块采样区会超出捕获层缓冲，边缘被裁掉。
     * Q 弹感只由位置弹簧提供。
     */
    fun animateToValueBouncy(value: Float) {
        animationScope.launch {
            mutatorMutex.mutate {
                val target = value.coerceIn(valueRange)
                launch { valueAnimation.animateTo(target, bouncyValueAnimationSpec) }
                if (velocity != 0f) {
                    launch { velocityAnimation.animateTo(0f, velocityAnimationSpec) }
                }
            }
        }
    }

    private fun updateVelocity() {
        velocityTracker.addPosition(
            Clock.System.now().toEpochMilliseconds(),
            Offset(value, 0f),
        )
        val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f)
        val targetVelocity = velocityTracker.calculateVelocity().x / span
        animationScope.launch { velocityAnimation.animateTo(targetVelocity, velocityAnimationSpec) }
    }
}

/**
 * 拖拽手势：与 `detectDragGestures` 等价，但把「按下位置」交给调用方。
 *
 * Compose 自带的 `detectDragGestures` 只给位移不给按下点，
 * 而滑块需要知道「按在滑块内的哪个位置」才能把局部坐标换算回格下标。
 *
 * 手势流程与官方 `detectDragGestures` 一致：按下 → 等触摸阈值（slop）
 * → 逐帧上报位移 → 抬起/取消。先过 slop 保证「轻点」不会被误判成拖拽。
 */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.awaitEachDragGesture(
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (PointerInputChange, Offset) -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down.position)

        // 等真正越过触摸阈值才算拖拽（与系统一致，避免轻点被当成微小拖动）
        var drag: PointerInputChange? = null
        val slopChange = awaitTouchSlopOrCancellation(down.id) { change, over ->
            change.consume()
            onDrag(change, over)
            drag = change
        }

        if (slopChange != null) {
            // 已经吃掉了第一段位移，继续跟手
            drag = slopChange
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) break
                val delta = change.positionChange()
                if (delta != Offset.Zero) {
                    onDrag(change, delta)
                    change.consume()
                    drag = change
                }
            }
            onDragEnd()
        } else {
            // 没越过阈值：抬起就是点击（由调用方在 onDragEnd 里按 moved 判定）
            onDragEnd()
        }
    }
}
