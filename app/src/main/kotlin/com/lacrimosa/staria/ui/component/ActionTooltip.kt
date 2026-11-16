package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.PopupRadius
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.accent
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val TooltipWidth = 132.dp
private val TooltipItemHeight = 30.dp
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
    /** 被长按卡片的窗口矩形；压暗遮罩会在此处挖洞，让卡片保持原亮度。 */
    keepLit: Rect? = null,
) {
    if (actions.isEmpty()) return

    // 有 host 就转给它，由根布局在 layerBackdrop 子树之外绘制 ——
    // 否则玻璃面板会「画自己」，实机栈溢出（见 TooltipHostState 的说明）。
    val host = LocalTooltipHost.current
    if (host != null) {
        // 只要这个组合存在，就代表「页面希望菜单显示」。
        // key 用 anchor：同一锚点重复组合不会重复 show。
        DisposableEffect(anchor, keepLit) {
            host.show(anchor, actions, preferredCorner, onDismiss, keepLit)
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
        keepLit = keepLit,
    )
}

/**
 * 真正绘制菜单的入口。
 *
 * 与 [ActionTooltip] 分开的原因：根布局要用它来画菜单，
 * 如果直接复用 [ActionTooltip]，根布局又会把请求转发回自己，形成自触发循环。
 *
 * 展开 / 收起动画都在这里统一管理：进入用带过冲的弹簧（展开后小幅回弹），
 * 收起同样带弹性；收起动画播完才回调 [onDismiss]，页面才会真正移除本组合。
 */
@Composable
fun TooltipSurface(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    preferredCorner: AnchorCorner = AnchorCorner.BottomLeft,
    /** 被长按卡片的窗口矩形；压暗遮罩会在此处挖洞，让卡片保持原亮度。 */
    keepLit: Rect? = null,
) {
    if (actions.isEmpty()) return

    // 0 = 完全收起，1 = 完全展开。过冲时 > 1。
    val appear = remember { Animatable(0f) }
    // 正在播放收起动画。动画期间内容仍需渲染，结束后才 onDismiss。
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 进入：快速展开 + 小幅度过冲回弹（与随笔查看页「更多」菜单统一参数）
    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    // 请求收起：先播退出动画，结束再执行回调 + onDismiss。
    fun requestClose(afterClose: () -> Unit = {}) {
        if (closing) return
        closing = true
        scope.launch {
            appear.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            afterClose()
            onDismiss()
        }
    }

    TooltipBody(
        actions = actions,
        anchor = anchor,
        appear = appear.value,
        requestClose = ::requestClose,
        preferredCorner = preferredCorner,
        keepLit = keepLit,
    )
}

/**
 * tooltip 的实际绘制：压暗遮罩 + 淡灰不透明菜单。
 *
 * ## 为什么不再用玻璃面板
 *
 * 早先菜单是一块液态玻璃（`drawBackdrop`）。它每帧都要重采样背景并重算
 * 折射/模糊，在弹簧动画期间边缘折射带随尺寸重算，背景上就出现明显的
 * 阴影闪动；调参、锁缩放、改真实尺寸都未能彻底消除。
 *
 * 现在面板是与页面背景同色的淡灰实心块，不依赖背景采样，任何尺寸与
 * 透明度动画都不会闪；层次由遮罩提供。
 */
@Composable
private fun TooltipBody(
    actions: List<TooltipAction>,
    anchor: Offset,
    appear: Float,
    requestClose: (afterClose: () -> Unit) -> Unit,
    preferredCorner: AnchorCorner,
    keepLit: Rect?,
) {
    val density = LocalDensity.current

    // 过冲时 appear > 1，scale 会短暂超过 1 再回落，形成「展开后小幅回弹」。
    // 面板是纯色、不采样背景，过冲不会引起任何背景闪烁。
    val scale = 0.92f + 0.08f * appear
    val alpha = appear.coerceIn(0f, 1f)

    // 颜色令牌在组合期取出，供 Canvas 闭包捕获
    val scrimColor = Scrim
    val surfaceColor = PageBackground

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat()
        val screenH = constraints.maxHeight.toFloat()

        val w = with(density) { TooltipWidth.toPx() }
        val h = with(density) { (TooltipItemHeight * actions.size + TooltipPadV * 2).toPx() }
        val m = with(density) { TooltipMargin.toPx() }
        val pos = resolvePosition(anchor, w, h, m, screenW, screenH, preferredCorner)

        // 点空白关闭 + 压暗背景。遮罩随 appear 淡入淡出；
        // 被长按卡片（keepLit）处挖洞，保持原亮度。
        //
        // 注意 clickable 要等菜单基本展开后才启用：触发 tooltip 的正是长按后的
        // 抬手，遮罩若立刻可点，会把这次抬手当成「点空白」当场关掉。
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (appear >= 0.98f) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { requestClose {} },
                        )
                    } else {
                        Modifier
                    },
                ),
        ) {
            val scrimAlpha = (alpha * scrimColor.alpha).coerceIn(0f, 1f)
            // 单独开一层，才能用 BlendMode.Clear 把卡片区域「擦」回原亮度
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(Offset.Zero, size), Paint())
                drawRect(color = scrimColor, alpha = scrimAlpha)
                val lit = keepLit
                if (lit != null) {
                    // 圆角矩形挖洞：与卡片圆角一致，看起来是「这张卡被拎起来」，
                    // 而不是在背景上开了一条白带
                    val radiusPx = CardRadius.toPx()
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = Offset(lit.left, lit.top),
                        size = Size(lit.width, lit.height),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        blendMode = BlendMode.Clear,
                    )
                }
                canvas.restore()
            }
        }

        // 菜单面板：与全局背景同色的淡灰实心块，保证可读性；
        // 外圈一道主题色描边提升辨识度（简约白主题下回落为墨色）。
        val outline = LocalThemeColor.current.accent()
        Box(
            modifier = Modifier
                .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                .width(TooltipWidth)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }
                .background(surfaceColor, RoundedCornerShape(PopupRadius))
                .border(1.dp, outline, RoundedCornerShape(PopupRadius))
                .padding(vertical = TooltipPadV),
        ) {
            TooltipItems(actions = actions, requestClose = requestClose)
        }
    }
}

@Composable
private fun TooltipItems(
    actions: List<TooltipAction>,
    requestClose: (afterClose: () -> Unit) -> Unit,
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
                        // 先播收起动画，结束再执行动作 + 关闭
                        requestClose { action.onClick() }
                    }
                    .padding(horizontal = 13.dp, vertical = 6.dp),
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
