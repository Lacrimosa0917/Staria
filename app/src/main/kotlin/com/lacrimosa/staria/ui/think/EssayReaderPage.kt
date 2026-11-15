package com.lacrimosa.staria.ui.think

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.Essay
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.GlassCircleButton
import com.lacrimosa.staria.ui.component.GlassPanel
import com.lacrimosa.staria.ui.icon.IconBack
import com.lacrimosa.staria.ui.icon.IconMore
import com.lacrimosa.staria.ui.icon.IconPencilSmall
import com.lacrimosa.staria.ui.icon.IconTrash
import java.time.format.DateTimeFormatter
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.PopupRadius
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 圆形按钮直径。 */
private val ButtonSize = SecondaryButtonSize

/** 菜单面板宽度。 */
private val MenuWidth = 132.dp

/** 菜单单项高度。 */
private val MenuItemHeight = 34.dp

/** 菜单上下内边距。 */
private val MenuPadV = 4.dp

/** 收起态圆角半径（正圆）。 */
private val CircleRadius = ButtonSize / 2

/** 展开态圆角半径。 */
private val MenuRadius = PopupRadius

/**
 * 随笔查看页（二级全屏页面）。
 *
 * README 要求：
 * - 左上角圆形返回按键，右上角圆形更多选项按键
 * - 点击更多展开 tooltip 菜单，可选编辑或删除
 * - 标题行显示「随笔：＋标题实际内容」，其中「随笔：」用刻本宋体
 *
 * 「更多」的实现方式：按钮**自身形变**成菜单面板 ——
 * 直径从 38dp 拉伸为菜单尺寸、圆角由圆变方角，
 * 因为定位在右上，扩展方向自然朝左下。不是弹出独立浮层。
 */
@Composable
fun EssayReaderPage(
    essay: Essay,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    // 展开后的面板尺寸：两项 + 上下内边距
    val menuHeight = MenuItemHeight * 2 + MenuPadV * 2

    val boxWidth by animateDpAsState(
        targetValue = if (menuExpanded) MenuWidth else ButtonSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "more-width",
    )
    val boxHeight by animateDpAsState(
        targetValue = if (menuExpanded) menuHeight else ButtonSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "more-height",
    )
    val corner by animateDpAsState(
        targetValue = if (menuExpanded) MenuRadius else CircleRadius,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "more-corner",
    )

    // 收起时显示三点，展开时显示菜单项
    val iconAlpha by animateFloatAsState(
        targetValue = if (menuExpanded) 0f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "more-icon",
    )
    val menuAlpha by animateFloatAsState(
        targetValue = if (menuExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "more-menu",
    )

    Box(modifier = modifier.fillMaxSize().background(PageBackground)) {
        // ---------- 正文（可滚动） ----------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                // 顶栏按钮是悬浮的，正文顶部留出它的高度
                .padding(top = ButtonSize + 28.dp),
        ) {
            // 标题行：随笔：＋标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding + 4.dp),
            ) {
                Text(
                    text = "随笔：",
                    fontFamily = KebenSong,
                    fontSize = 16.sp,
                    color = InkMuted,
                )
                Text(
                    text = essay.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink,
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = essay.date.format(DateFormatter),
                modifier = Modifier.padding(horizontal = BarSidePadding + 4.dp),
                color = InkMuted,
                fontSize = 12.sp,
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = essay.body,
                modifier = Modifier.padding(horizontal = BarSidePadding + 4.dp),
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 28.sp,
            )

            Spacer(Modifier.height(48.dp))
        }

        // ---------- 菜单展开时点空白处收起 ----------
        // 放在按钮之前声明，层级低于按钮，所以不会挡住面板自身
        if (menuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { menuExpanded = false },
                    ),
            )
        }

        // ---------- 左上角：返回 ----------
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = 14.dp),
        ) {
            CircleIconButton(
                icon = IconBack,
                description = "返回",
                onClick = onBack,
            )
        }

        // ---------- 右上角：更多（点击后自身形变为菜单） ----------
        // 形状动画期间圆角在「正圆」与「12dp 圆角」之间变化，两者都是
        // CornerBasedShape，玻璃折射能跟上；宽度跨度过大时折射带会被拉宽，
        // 因此倍率收到 0.6，与底栏胶囊一致。
        //
        // 注意 padding 必须在 GlassPanel 之前：
        // GlassPanel 用 drawBackdrop 绘制，若把 padding 写在它之后，
        // 面板会被整体内缩，而点击感应区仍留在原位，出现「看得见点不到」。
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = BarSidePadding, top = 14.dp),
        ) {
            GlassPanel(
                backdrop = LocalGlassBackdrop.current,
                shape = RoundedCornerShape(corner),
                refractionScale = 0.6f,
                edgeHighlight = if (!menuExpanded) 1.dp else 0.dp,
                modifier = Modifier.size(boxWidth, boxHeight),
            ) {
            // 三点图标：收起态显示，展开时淡出
            Box(
                modifier = Modifier.fillMaxSize().alpha(iconAlpha),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconMore,
                    contentDescription = "更多",
                    modifier = Modifier.size(18.dp),
                    tint = Ink,
                )
            }

            // 展开态：点击整块也能展开的感应区（仅在收起时生效）
            if (!menuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { menuExpanded = true },
                        ),
                )
            }

            // 菜单项：展开时淡入
            if (menuAlpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(menuAlpha)
                        .padding(vertical = MenuPadV),
                ) {
                    MenuRow(
                        icon = IconPencilSmall,
                        label = "编辑",
                        destructive = false,
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    MenuRow(
                        icon = IconTrash,
                        label = "删除",
                        destructive = true,
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                    )
                }
            }
            }
        }
    }
}

/** 菜单里的一行。 */
@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    destructive: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(MenuItemHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = if (destructive) Danger else Ink,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            color = if (destructive) Danger else Ink,
            fontSize = 13.sp,
        )
    }
}

/** 圆形图标按钮。返回键用。 */
@Composable
private fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    GlassCircleButton(
        icon = icon,
        description = description,
        onClick = onClick,
        iconSize = 18.dp,
    )
}
