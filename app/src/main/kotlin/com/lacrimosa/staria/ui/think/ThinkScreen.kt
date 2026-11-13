package com.lacrimosa.staria.ui.think

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.Essay
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.LocalContentInsets
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.icon.IconChecklist
import com.lacrimosa.staria.ui.icon.IconPencilSmall
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import java.time.format.DateTimeFormatter

private val Hairline = Color(0xFFE6E2DA)

/** 卡片日期格式：精确至日。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 多选时内容右移的距离（为左侧选中框让位）。 */
private val SelectShift = 26.dp

/**
 * 思索（随笔查看页）。
 *
 * README 要求：
 * - 等宽卡片上下排列，第一行为日期
 * - 第二行以更高字号和字重展示标题
 * - 第三行展示正文预览，行末放不下的用省略号
 * - 点击卡片进入二级全屏查看页
 * - 长按卡片展开 tooltip（编辑 / 删除 / 多选），与时间、纸条页一致
 * - 空列表时展示 think slogan
 */
@Composable
fun ThinkScreen(
    essays: List<Essay> = SampleData.essays,
    selection: SelectionState,
    onUserInteract: () -> Unit = {},
    onEdit: (Essay) -> Unit = {},
    onDelete: (Essay) -> Unit = {},
    /** 点击卡片进入查看页，由根布局渲染二级全屏页面。 */
    onOpen: (Essay) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 新的在下方：按日期升序
    val ordered = remember(essays) { essays.sortedBy { it.date } }

    var tooltipEssay by remember { mutableStateOf<Essay?>(null) }
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }

    if (ordered.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "think",
                color = InkMuted.copy(alpha = 0.55f),
                fontSize = 13.sp,
            )
        }
        return
    }

    val insets = LocalContentInsets.current
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // 只观察按下事件用于收起底栏，不消费，
                // 否则会与卡片的长按/点击手势竞争
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) onUserInteract()
                        }
                    }
                },
        ) {
            // 滚到最上方时第一张停在悬浮头像之下（含状态栏高度）
            Spacer(Modifier.height(insets.top))

            ordered.forEach { essay ->
                EssayCard(
                    essay = essay,
                    selectionMode = selection.isActive,
                    selected = selection.contains(essay.id),
                    onClick = {
                        onUserInteract()
                        if (selection.isActive) {
                            selection.toggle(essay.id)
                        } else {
                            onOpen(essay)
                        }
                    },
                    onLongPress = { pos ->
                        onUserInteract()
                        if (!selection.isActive) {
                            tooltipAnchor = pos
                            tooltipEssay = essay
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            // 滚到底时最后一张停在悬浮底栏之上（含导航栏高度）
            Spacer(Modifier.height(insets.bottom))
        }

        tooltipEssay?.let { essay ->
            ActionTooltip(
                anchor = tooltipAnchor,
                actions = listOf(
                    TooltipAction(
                        label = "编辑",
                        icon = IconPencilSmall,
                        onClick = {
                            tooltipEssay = null
                            onEdit(essay)
                        },
                    ),
                    TooltipAction(
                        label = "删除",
                        icon = IconTrash,
                        destructive = true,
                        onClick = {
                            tooltipEssay = null
                            onDelete(essay)
                        },
                    ),
                    TooltipAction(
                        label = "多选",
                        icon = IconChecklist,
                        onClick = {
                            tooltipEssay = null
                            selection.enter(essay.id)
                        },
                    ),
                ),
                onDismiss = { tooltipEssay = null },
            )
        }
    }
}

/**
 * 一张随笔卡片。
 *
 * 版式（README）：
 * - 第一行：日期
 * - 第二行：标题，字号与字重都高于正文
 * - 第三行：正文预览，超出用省略号
 */
@Composable
private fun EssayCard(
    essay: Essay,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset) -> Unit,
) {
    val shift = if (selectionMode) SelectShift else 0.dp
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BarSidePadding)
            .onGloballyPositioned { coords ->
                windowOrigin = coords.positionInWindow()
            }
            // 卡片底色与纸条页一致，略深于纯白背景
            .background(CardPaper, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            // 点击与长按放在同一个 detectTapGestures 里
            .pointerInput(selectionMode) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { local ->
                        if (!selectionMode) onLongPress(windowOrigin + local)
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ---------- 第一行：日期 ----------
            Text(
                text = essay.date.format(DateFormatter),
                modifier = Modifier.padding(start = shift),
                color = InkMuted,
                fontSize = 11.sp,
            )

            Spacer(Modifier.height(8.dp))

            // ---------- 第二行：标题（更高字号与字重）----------
            Text(
                text = essay.title,
                modifier = Modifier.padding(start = shift),
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 24.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(6.dp))

            // ---------- 第三行：正文预览 ----------
            Text(
                text = essay.body,
                modifier = Modifier.padding(start = shift),
                color = InkMuted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 选中框：多选时出现在最左侧
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
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
}
