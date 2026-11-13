package com.lacrimosa.staria.ui.think

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.Essay
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.AnchorCorner
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.icon.IconChevronLeft
import com.lacrimosa.staria.ui.icon.IconMore
import com.lacrimosa.staria.ui.icon.IconPencilSmall
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.PaperWhite
import java.time.format.DateTimeFormatter

private val Hairline = Color(0xFFE6E2DA)
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/**
 * 随笔查看页（二级全屏页面）。
 *
 * README 要求：
 * - 左上角圆形返回按键，右上角圆形更多选项按键
 * - 点击更多展开 tooltip，可选编辑或删除
 * - 标题行显示「随笔：＋标题实际内容」，其中「随笔：」用刻本宋体
 *
 * 该页全屏覆盖在思考列表之上，底栏与头像不出现。
 */
@Composable
fun EssayReaderPage(
    essay: Essay,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize().background(PaperWhite)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // ---------- 顶栏：返回 / 更多 ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = BarSidePadding, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(
                    icon = IconChevronLeft,
                    description = "返回",
                    onClick = onBack,
                )

                Spacer(Modifier.weight(1f))

                CircleIconButton(
                    icon = IconMore,
                    description = "更多",
                    onClick = { showMenu = true },
                    capturePosition = { anchor = it },
                )
            }

            Spacer(Modifier.height(6.dp))

            // ---------- 标题行：随笔：＋标题 ----------
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

            // ---------- 正文 ----------
            Text(
                text = essay.body,
                modifier = Modifier.padding(horizontal = BarSidePadding + 4.dp),
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 28.sp,
            )

            Spacer(Modifier.height(48.dp))
        }

        if (showMenu) {
            ActionTooltip(
                anchor = anchor,
                // 更多按钮在右上角，菜单向左下展开，不遮住按钮本身
                preferredCorner = AnchorCorner.TopRight,
                actions = listOf(
                    TooltipAction(
                        label = "编辑",
                        icon = IconPencilSmall,
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                    ),
                    TooltipAction(
                        label = "删除",
                        icon = IconTrash,
                        destructive = true,
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                    ),
                ),
                onDismiss = { showMenu = false },
            )
        }
    }
}

/**
 * 圆形图标按钮。顶栏返回与更多共用。
 *
 * [capturePosition] 用于记下按钮在窗口中的位置，让 tooltip 贴着按钮展开。
 */
@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    capturePosition: ((Offset) -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .then(
                if (capturePosition != null) {
                    Modifier.onGloballyPositioned { coords ->
                        // 取按钮左下角作为锚点：配合 TopRight 偏好，
                        // 菜单贴着按钮左下方向展开，不遮住按钮本身
                        val topLeft = coords.positionInWindow()
                        capturePosition(
                            Offset(topLeft.x, topLeft.y + coords.size.height),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .background(PaperWhite, CircleShape)
            .border(1.dp, Hairline, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(18.dp),
            tint = Ink,
        )
    }
}
