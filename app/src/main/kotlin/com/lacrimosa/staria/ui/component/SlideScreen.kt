package com.lacrimosa.staria.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 二级页面进入 / 退出的滑动方向。
 */
enum class SlideDirection {
    /** 从左侧滑入，退出时向左侧滑出（星尘、设置）。 */
    FromLeft,
    /** 从右侧滑入，退出时向右侧滑出（随笔查看、编辑页）。 */
    FromRight,
}

/** 二级页面滑动动画时长。 */
private const val SlideDurationMs = 260

/**
 * 带滑动进出动画的全屏二级页面容器。
 *
 * 用 [AnimatedVisibility] 保证双向动画：
 * - `visible` 变 true 时按 [direction] 滑入
 * - `visible` 变 false 时按 [direction] 反向滑出，动画播完内容才移除
 *
 * 滑入方向：FromLeft = 从 -width 到 0；FromRight = 从 +width 到 0。
 * 滑出方向与之相反。
 */
@Composable
fun SlideScreen(
    visible: Boolean,
    direction: SlideDirection,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val enter = when (direction) {
        SlideDirection.FromLeft -> slideInHorizontally(
            animationSpec = tween(SlideDurationMs),
            initialOffsetX = { -it },
        )
        SlideDirection.FromRight -> slideInHorizontally(
            animationSpec = tween(SlideDurationMs),
            initialOffsetX = { it },
        )
    }
    val exit = when (direction) {
        SlideDirection.FromLeft -> slideOutHorizontally(
            animationSpec = tween(SlideDurationMs),
            targetOffsetX = { -it },
        )
        SlideDirection.FromRight -> slideOutHorizontally(
            animationSpec = tween(SlideDurationMs),
            targetOffsetX = { it },
        )
    }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
    ) {
        content()
    }
}
