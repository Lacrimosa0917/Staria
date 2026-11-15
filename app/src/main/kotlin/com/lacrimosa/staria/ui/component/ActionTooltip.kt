package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kyant.backdrop.Backdrop
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PopupRadius
import com.lacrimosa.staria.ui.theme.supportsRenderEffect
import kotlin.math.roundToInt

private val TooltipWidth = 148.dp
private val TooltipItemHeight = 34.dp
private val TooltipPadV = 4.dp
private val TooltipMargin = 6.dp

/** tooltip 里的一项：图标 + 文字。 */
data class TooltipAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/** 弹窗相对锚点的展开方向。 */
enum class AnchorCorner {
    BottomLeft,
    BottomRight,
    TopLeft,
    TopRight,
}

/**
 * 长按唤起的操作 tooltip（液态玻璃，按仓库 Glass Bottom Sheet 的做法）。
 *
 * ## 为什么用同窗口层而不是 Popup
 *
 * 库的 `drawBackdrop` 需要一个 [Backdrop]，而 Backdrop 是通过
 * `Modifier.layerBackdrop(...)` 把**同一个窗口里**的内容录进 GraphicsLayer 的。
 * `Popup` 在 Android 上会开一个**独立的 Window**，
 * 跨窗口既拿不到那个 layer 的坐标，也采样不到宿主窗口的像素 —— 玻璃效果会失效。
 *
 * 所以这里分两条路：
 * - **支持 RenderEffect（Android 12+）**：走同窗口绝对定位层，玻璃是真的
 * - **不支持（minSdk 26 那批机器）**：退回 `Popup` + 纸面样式
 *
 * ## glass on glass
 *
 * 仓库 Glass Bottom Sheet 一节专门讲过：如果玻璃面板里还要放玻璃控件，
 * 直接给内层控件传同一个 backdrop 会采样到「面板尚未绘制」的画面。
 * 正确做法是给面板传 `exportedBackdrop`，让内层控件以**面板本身**为背景。
 * 这里 tooltip 直接以页面为背景，所以用不到这层，但保留了参数位。
 */
@Composable
fun ActionTooltip(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    preferredCorner: AnchorCorner = AnchorCorner.BottomLeft,
) {
    if (actions.isEmpty()) return

    // 有 host 就转给它，由根布局在 layerBackdrop 子树之外绘制 ——
    // 否则玻璃面板会「画自己」，实机栈溢出（见 TooltipHostState 的说明）。
    val host = LocalTooltipHost.current
    if (host != null) {
        // 只要这个组合存在，就代表「页面希望菜单显示」。
        // key 用 anchor：同一锚点重复组合不会重复 show。
        DisposableEffect(anchor) {
            host.show(anchor, actions, preferredCorner, onDismiss)
            onDispose { /* 关闭由 onDismiss / 页面状态负责 */ }
        }
        return
    }

    // 兜底：没有 host（脱离根布局的预览 / 测试）时就地渲染
    TooltipSurface(
        actions = actions,
        anchor = anchor,
        onDismiss = onDismiss,
        preferredCorner = preferredCorner,
    )
}

/**
 * 真正绘制菜单的入口。
 *
 * 与 [ActionTooltip] 分开的原因：根布局要用它来画菜单，
 * 如果直接复用 [ActionTooltip]，根布局又会把请求转发回自己，形成自触发循环。
 */
@Composable
fun TooltipSurface(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    preferredCorner: AnchorCorner = AnchorCorner.BottomLeft,
) {
    if (actions.isEmpty()) return

    val backdrop = LocalGlassBackdrop.current
    if (backdrop != null && supportsRenderEffect) {
        InWindowTooltip(
            actions = actions,
            anchor = anchor,
            onDismiss = onDismiss,
            backdrop = backdrop,
            preferredCorner = preferredCorner,
        )
    } else {
        PopupTooltip(
            actions = actions,
            anchor = anchor,
            onDismiss = onDismiss,
            preferredCorner = preferredCorner,
        )
    }
}

/**
 * 同窗口内的玻璃 tooltip。
 *
 * 出现动画：从 0.92 缩放到 1、透明度 0 → 1，弹簧收尾。
 * 不突然弹出，与底栏的展开动画保持同一套手感。
 */
@Composable
private fun InWindowTooltip(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    preferredCorner: AnchorCorner,
) {
    val density = LocalDensity.current

    // 进入动画：Compose 首次组合即为目标值，所以用一个从 0 起跳的 Animatable
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f))
    }
    val scale = 0.92f + 0.08f * appear.value

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        val w = with(density) { TooltipWidth.toPx() }
        val h = with(density) { (TooltipItemHeight * actions.size + TooltipPadV * 2).toPx() }
        val m = with(density) { TooltipMargin.toPx() }
        val pos = resolvePosition(anchor, w, h, m, screenW, screenH, preferredCorner)

        // 点空白关闭：铺满整屏的透明层，位于面板之下
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        GlassPanel(
            backdrop = backdrop,
            shape = RoundedCornerShape(PopupRadius),
            // 面板尺寸小、圆角 12dp，折射带按 0.6 收窄避免边缘断裂
            refractionScale = 0.6f,
            edgeHighlight = 1.dp,
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .width(TooltipWidth)
                .graphicsLayer {
                    alpha = appear.value
                    scaleX = scale
                    scaleY = scale
                }
                .padding(vertical = TooltipPadV),
        ) {
            TooltipItems(actions = actions, onDismiss = onDismiss)
        }
    }
}

/** 跨窗口的降级版 tooltip（低版本系统）。 */
@Composable
private fun PopupTooltip(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    preferredCorner: AnchorCorner,
) {
    val density = LocalDensity.current

    // 低版本没有 RenderEffect，但动画仍然要有：淡入 + 轻微缩放
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = 420f))
    }
    val scale = 0.92f + 0.08f * appear.value

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset.Zero,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()

            val w = with(density) { TooltipWidth.toPx() }
            val h = with(density) { (TooltipItemHeight * actions.size + TooltipPadV * 2).toPx() }
            val m = with(density) { TooltipMargin.toPx() }
            val pos = resolvePosition(anchor, w, h, m, screenW, screenH, preferredCorner)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )

            GlassPanel(
                backdrop = null,
                shape = RoundedCornerShape(PopupRadius),
                modifier = Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .width(TooltipWidth)
                    .graphicsLayer {
                        alpha = appear.value
                        scaleX = scale
                        scaleY = scale
                    }
                    .padding(vertical = TooltipPadV),
            ) {
                TooltipItems(actions = actions, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun TooltipItems(
    actions: List<TooltipAction>,
    onDismiss: () -> Unit,
) {
    Column {
        actions.forEach { action ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onDismiss()
                        action.onClick()
                    }
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (action.destructive) Danger else Ink,
                )
                Box(modifier = Modifier.size(9.dp))
                Text(
                    text = action.label,
                    color = if (action.destructive) Danger else Ink,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/**
 * 计算弹窗左上角坐标。
 *
 * 每一项表示「锚点落在弹窗的哪个角」：
 * 首选角优先，放不下依次尝试其余三角，四角都放不下就夹进屏幕。
 */
private fun resolvePosition(
    anchor: Offset,
    w: Float,
    h: Float,
    m: Float,
    screenW: Float,
    screenH: Float,
    preferredCorner: AnchorCorner,
): Offset {
    fun posFor(corner: AnchorCorner): Offset = when (corner) {
        AnchorCorner.BottomLeft -> Offset(anchor.x + m, anchor.y - h - m)
        AnchorCorner.BottomRight -> Offset(anchor.x - w - m, anchor.y - h - m)
        AnchorCorner.TopLeft -> Offset(anchor.x + m, anchor.y + m)
        AnchorCorner.TopRight -> Offset(anchor.x - w - m, anchor.y + m)
    }

    val order = buildList {
        add(preferredCorner)
        AnchorCorner.entries.filterNot { it == preferredCorner }.forEach { add(it) }
    }
    val candidates = order.map(::posFor)

    val fits = candidates.firstOrNull { p ->
        p.x >= 0f && p.y >= 0f && p.x + w <= screenW && p.y + h <= screenH
    }
    return fits ?: run {
        val p = candidates.first()
        Offset(
            x = p.x.coerceIn(0f, (screenW - w).coerceAtLeast(0f)),
            y = p.y.coerceIn(0f, (screenH - h).coerceAtLeast(0f)),
        )
    }
}
