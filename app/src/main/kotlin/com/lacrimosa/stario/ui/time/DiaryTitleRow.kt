package com.lacrimosa.stario.ui.time

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.data.DiaryEntry
import com.lacrimosa.stario.ui.icon.IconPencilSmall
import com.lacrimosa.stario.ui.icon.IconTrash
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted

private val Hairline = Color(0xFFE6E2DA)

/**
 * 一行标题。点击后原地展开为卡片（README：点击该行展开为卡片展示）。
 *
 * 卡片右上角是编辑与删除按钮。
 */
@Composable
fun DiaryTitleRow(
    entry: DiaryEntry,
    expanded: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            ),
    ) {
        // ---------- 标题行 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(vertical = 9.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.title,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // ---------- 展开卡片 ----------
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(CardPaper, RoundedCornerShape(14.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    // 正文占位：暂无内容字段，先给出排版位
                    Text(
                        text = "（日记正文）",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        color = InkMuted,
                        fontSize = 13.sp,
                        lineHeight = 22.sp,
                    )

                    // 右上角：编辑 / 删除
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CardIconButton(
                            icon = IconPencilSmall,
                            description = "编辑",
                            onClick = onEdit,
                        )
                        CardIconButton(
                            icon = IconTrash,
                            description = "删除",
                            onClick = onDelete,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(Color(0xFFF3F0EA), CircleShape)
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = Ink,
        )
    }
}
