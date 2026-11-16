package com.lacrimosa.staria.ui.think

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
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
import com.lacrimosa.staria.ui.theme.BorderSelected
import com.lacrimosa.staria.ui.theme.CardGap
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.SelectionBoxRadius
import com.lacrimosa.staria.ui.theme.SelectionBoxSize
import com.lacrimosa.staria.ui.theme.SelectionShift
import java.time.format.DateTimeFormatter

/** 卡片日期格式：精确至日。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 多选时内容右移的距离（为左侧选中框让位）。 */
private val SelectShift = SelectionShift

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
    val ordered = remember(essays) { essays.sortedByDescending { it.date } }

    var tooltipEssay by remember { mutableStateOf<Essay?>(null) }
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }
    // 被长按卡片的窗口矩形：tooltip 遮罩在此挖洞，保持该卡片原亮度
    var tooltipKeepLit by remember { mutableStateOf<Rect?>(null) }

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
                    onLongPress = { pos, rect ->
                        onUserInteract()
                        if (!selection.isActive) {
                            tooltipAnchor = pos
                            tooltipKeepLit = rect
                            tooltipEssay = essay
                        }
                    },
                )
                Spacer(Modifier.height(CardGap))
            }

            // 滚到底时最后一张停在悬浮底栏之上（含导航栏高度）
            Spacer(Modifier.height(insets.bottom))
        }

        tooltipEssay?.let { essay ->
            ActionTooltip(
                anchor = tooltipAnchor,
                keepLit = tooltipKeepLit,
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
    /** 参数：长按点的窗口坐标、本卡片的窗口矩形（供 tooltip 挖洞保持亮度）。 */
    onLongPress: (Offset, Rect) -> Unit,
) {
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }
    var windowSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BarSidePadding)
            .onGloballyPositioned { coords ->
                windowOrigin = coords.positionInWindow()
                windowSize = coords.size
            }
            // 纯白卡面，不描边：与 #F5F5F5 底色的层次只靠色差
            .background(PaperWhite, RoundedCornerShape(CardRadius))
            // 点击与长按放在同一个 detectTapGestures 里
            .pointerInput(selectionMode) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { local ->
                        if (!selectionMode) {
                            onLongPress(
                                windowOrigin + local,
                                Rect(
                                    offset = windowOrigin,
                                    size = Size(
                                        windowSize.width.toFloat(),
                                        windowSize.height.toFloat(),
                                    ),
                                ),
                            )
                        }
                    },
                )
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 进入 / 退出多选时，只有第一行日期平滑右移 / 移回，
        // 标题与正文保持原位
        val dateShift by animateDpAsState(
            targetValue = if (selectionMode) SelectShift else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "think-date-shift",
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            // ---------- 第一行：日期 ----------
            // lineHeight 压到 14sp：11sp 文字吃默认行高会让这一行偏胖，
            // 与下方标题之间的间隙也跟着变大。
            Text(
                text = essay.date.format(DateFormatter),
                modifier = Modifier.padding(start = dateShift),
                color = InkMuted,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )

            Spacer(Modifier.height(7.dp))

            // ---------- 第二行：标题（更高字号与字重）----------
            Text(
                text = essay.title,
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
                color = InkMuted,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 选中框：多选时出现在最左侧（淡入淡出 + 轻微缩放）
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(160)) + scaleIn(tween(160), initialScale = 0.8f),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.8f),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Box(
                modifier = Modifier
                    .size(SelectionBoxSize)
                    .then(
                        if (selected) {
                            Modifier.background(Ink, RoundedCornerShape(SelectionBoxRadius))
                        } else {
                            Modifier.border(1.dp, BorderSelected, RoundedCornerShape(SelectionBoxRadius))
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Text(
                        text = "✓",
                        color = PaperWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
