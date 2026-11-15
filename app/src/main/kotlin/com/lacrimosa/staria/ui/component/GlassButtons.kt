package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize

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
) {
    if (filled) {
        // 实心键：墨色圆 + 白图标，不走玻璃
        Box(
            modifier = modifier
                .size(size)
                .background(Ink, CircleShape)
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
    GlassPanel(
        backdrop = LocalGlassBackdrop.current,
        shape = RoundedCornerShape(height / 2),
        // 圆角 = 高度/2，比圆钮的半径大，按底栏同样的比例收一下
        refractionScale = 0.6f,
        edgeHighlight = 1.dp,
        // 关键：只固定高度，宽度由内容撑开（wrapContent）。
        // 早先这里没给宽度约束，Panel 内部的 fillMaxSize 会让它占满整行，
        // 结果撤销重做胶囊把编辑页整个顶栏都吃掉了。
        modifier = modifier.height(height),
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .height(height)
                .wrapContentWidth(),
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
