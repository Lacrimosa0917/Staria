package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.PopupRadius
import com.lacrimosa.staria.ui.theme.Border

/** tooltip 宽度。内容只有图标与两三个汉字，保持紧凑。 */
private val TooltipWidth = 148.dp

/** 单项高度（含内边距），用于估算弹窗总高。 */
private val TooltipItemHeight = 34.dp

/** 弹窗内边距。 */
private val TooltipPadV = 4.dp

/** 弹窗与长按点之间留的空隙。 */
private val TooltipMargin = 6.dp

/** tooltip 里的一项：图标 + 文字。 */
data class TooltipAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 弹窗相对锚点的展开方向。
 *
 * 每一项表示「锚点落在弹窗的哪个角」：
 * - [AnchorCorner.BottomLeft]：锚点在弹窗左下 → 弹窗向右上展开
 * - [AnchorCorner.BottomRight]：锚点在弹窗右下 → 弹窗向左上展开
 * 等。
 */
enum class AnchorCorner {
    BottomLeft,
    BottomRight,
    TopLeft,
    TopRight,
}

/**
 * 长按唤起的操作 tooltip。
 *
 * 时间页、纸条页、思索页共用同一个组件，保证各处手感与外观一致。
 *
 * 位置策略（[anchor] 是锚点在窗口坐标系里的位置）：
 * 1. 默认让锚点落在弹窗的**左下角**（弹窗向右上展开）
 * 2. 该位置放不下时依次尝试：右下角 → 左上角 → 右上角
 * 3. 四个角都放不下，就夹进屏幕内
 *
 * [preferredCorner] 可指定首选角。例如右上角的「更多」按钮希望菜单
 * 向左下展开（锚点落在弹窗右上角），这样菜单不会盖住按钮本身。
 *
 * 用 `Popup` 而不是 `Dialog`：Dialog 固定居中且自带遮罩，无法贴住锚点。
 */
@Composable
fun ActionTooltip(
    actions: List<TooltipAction>,
    anchor: Offset,
    onDismiss: () -> Unit,
    preferredCorner: AnchorCorner = AnchorCorner.BottomLeft,
) {
    if (actions.isEmpty()) return

    val density = LocalDensity.current

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset.Zero,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()

            val w = with(density) { TooltipWidth.toPx() }
            val h = with(density) {
                (TooltipItemHeight * actions.size + TooltipPadV * 2).toPx()
            }
            val m = with(density) { TooltipMargin.toPx() }

            // 各角对应的弹窗左上角坐标
            fun posFor(corner: AnchorCorner): Offset = when (corner) {
                // 锚点位于弹窗左下角 → 弹窗在锚点右上方
                AnchorCorner.BottomLeft -> Offset(anchor.x + m, anchor.y - h - m)
                // 锚点位于弹窗右下角 → 弹窗在锚点左上方
                AnchorCorner.BottomRight -> Offset(anchor.x - w - m, anchor.y - h - m)
                // 锚点位于弹窗左上角 → 弹窗在锚点右下方
                AnchorCorner.TopLeft -> Offset(anchor.x + m, anchor.y + m)
                // 锚点位于弹窗右上角 → 弹窗在锚点左下方
                AnchorCorner.TopRight -> Offset(anchor.x - w - m, anchor.y + m)
            }

            // 首选角排在最前，其后是其余三个，作为放不下时的备选
            val order = buildList {
                add(preferredCorner)
                AnchorCorner.entries.filterNot { it == preferredCorner }.forEach { add(it) }
            }
            val candidates = order.map(::posFor)

            val fits = candidates.firstOrNull { p ->
                p.x >= 0f && p.y >= 0f && p.x + w <= screenW && p.y + h <= screenH
            }
            val pos = fits ?: run {
                val p = candidates.first()
                Offset(
                    x = p.x.coerceIn(0f, (screenW - w).coerceAtLeast(0f)),
                    y = p.y.coerceIn(0f, (screenH - h).coerceAtLeast(0f)),
                )
            }

            // 点空白关闭：铺满整屏的透明层，位于弹窗之下
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(
                        with(density) { screenW.toDp() },
                        with(density) { screenH.toDp() },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )

            Column(
                modifier = Modifier
                    .offset { IntOffset(pos.x.roundToInt(), pos.y.roundToInt()) }
                    .width(TooltipWidth)
                    .background(PaperWhite, RoundedCornerShape(PopupRadius))
                    .border(1.dp, Border, RoundedCornerShape(PopupRadius))
                    // 吞掉点击，避免穿透到关闭层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(vertical = TooltipPadV),
            ) {
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
    }
}
