package com.lacrimosa.stario.ui.time

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.data.DiaryEntry
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted

private val Hairline = Color(0xFFE6E2DA)

/** 多选模式下标题整体左移的距离（为左侧选中框让位）。 */
private val SelectShift = 26.dp

/**
 * 一行标题。点击展开为卡片（README：点击该行展开为卡片展示）。
 *
 * 长按 1 秒弹出 tooltip（编辑 / 删除 / 多选），与纸条页保持一致。
 * 卡片本身不再放操作按钮。
 *
 * 多选模式下该行左移，左侧出现选中框。
 */
@Composable
fun DiaryTitleRow(
    entry: DiaryEntry,
    expanded: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset) -> Unit,
) {
    val shift = if (selectionMode) SelectShift else 0.dp

    // 本行在窗口里的左上角，用于把手势坐标换算成窗口坐标
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = shift)
                    .onGloballyPositioned { coords ->
                        windowOrigin = coords.positionInWindow()
                    }
                    // clickable 只负责点击；长按单独处理以获取坐标
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { local -> onLongPress(windowOrigin + local) },
                        )
                    }
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

            // 选中框：贴在该行最左侧，多选时才出现
            if (selectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 2.dp)
                        .size(17.dp)
                        .then(
                            if (selected) {
                                Modifier.background(Ink, RoundedCornerShape(4.dp))
                            } else {
                                Modifier.border(1.dp, Color(0xFFC9C3B8), RoundedCornerShape(4.dp))
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Text(
                            text = "✓",
                            color = CardPaper,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // ---------- 展开卡片 ----------
        // 多选时不展开，避免与选择操作抢空间
        if (expanded && !selectionMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = shift)
                    .padding(bottom = 10.dp)
                    .background(CardPaper, RoundedCornerShape(14.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // 正文占位：暂无内容字段
                Text(
                    text = "（日记正文）",
                    color = InkMuted,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }
}
