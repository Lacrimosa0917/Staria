package com.lacrimosa.staria.ui.time

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.DiaryEntry
import com.lacrimosa.staria.data.DiaryGroup
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.LocalContentInsets
import com.lacrimosa.staria.ui.component.DestructiveRed
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.component.navFirstItemLeftX
import com.lacrimosa.staria.ui.icon.IconChecklist
import com.lacrimosa.staria.ui.icon.IconPencilSmall
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.PaperWhite
import java.time.format.DateTimeFormatter

private val AxisColor = Color(0xFFD9D4CA)

/**
 * 时间轴列宽占屏幕比例。
 *
 * README：时间轴加日期，显示范围不超过屏幕左侧 1/4。
 */
private const val AXIS_COLUMN_FRACTION = 0.25f

/**
 * 日期文本相对轴线圆点的横向起点偏移。
 *
 * 留出圆点半径 + 一点呼吸间距，避免日期紧贴圆点。
 */
private val DateTextStartOffset = 12.dp

/** 标题列表的左侧留白，把日期与标题分开。 */
private val TitleStartPadding = 14.dp

/** 日期格式：月/日，不带年份，保证能塞进 1/4 宽度。 */
private val DateTextFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M/d")

/** 圆点相对每组顶部的偏移。 */
private val DotTop = 20.dp

/**
 * 内容顶部留白在运行时从 [LocalContentInsets] 取，
 * 轴线起笔位置要与之对齐，所以不能再写成编译期常量。
 */

/**
 * 时间（日记查看页）。
 *
 * README 要求：
 * - 左侧竖向时间轴，竖线对齐左下角页面按键的中心
 * - 每个具体日期处用圆点把竖线截断，圆点右侧显示日期
 * - 时间轴加日期整体不超过屏幕左侧 1/4
 * - 靠下为最新日期（按日期升序自上而下）
 * - 长按日期超过 1 秒唤起日历卡片
 * - 右侧按日期分行展示日记标题；点击展开为卡片
 * - 卡片右上角编辑 / 删除；删除首次需确认，可勾选不再提示
 * - 操作本页时若底栏展开则自动收起
 * - 时间轴只记录已有日记的日期；整页为空时只保留中央 slogan
 * - 内容不足一页时上方截断（底部对齐）
 */
@Composable
fun TimeScreen(
    entries: List<DiaryEntry> = SampleData.diaryEntries,
    selection: SelectionState,
    onUserInteract: () -> Unit = {},
    onEdit: (DiaryEntry) -> Unit = {},
    onDelete: (DiaryEntry) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val groups = remember(entries) {
        entries
            .filterNot { it.deleted }
            .groupBy { it.date }
            .toSortedMap()
            .map { (date, list) -> DiaryGroup(date, list) }
    }

    var expandedId by remember { mutableStateOf<Long?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var skipDeleteConfirm by remember { mutableStateOf(false) }
    var dontAskAgain by remember { mutableStateOf(false) }

    // 长按唤起的 tooltip（与纸条页共用 ActionTooltip）
    var tooltipEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    // 长按点在窗口里的位置，tooltip 据此决定贴哪个角
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }

    // id -> 条目，供批量删除时取回对象
    val diaryLookup: Map<Long, DiaryEntry> = remember(entries) {
        entries.associateBy { it.id }
    }

    // 没有日记：不画时间轴，只留居中 slogan
    if (groups.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "time",
                color = InkMuted.copy(alpha = 0.55f),
                fontSize = 13.sp,
            )
        }
        return
    }

    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 竖线对准底栏第一个按键的左边缘（需求：时间轴整体左移）
        val axisX = navFirstItemLeftX()

        // 左侧列（轴线 + 日期）宽度：以轴线为起点，留出日期所需空间，
        // 整体不超过屏宽 1/4（README 限制）
        val dateColumnWidth = minOf(maxWidth * AXIS_COLUMN_FRACTION, axisX + 52.dp)

        // 记录每组实测高度，用它累加出每个圆点的纵向位置来画轴线。
        // 不用 IntrinsicSize + fillMaxHeight：那会把行撑高，导致节点稀疏。
        val groupHeights = remember(groups) { mutableStateListOf<Int>() }

        // 顶部留白从根布局提供的真实 inset 取，轴线起笔要与之对齐
        val insets = LocalContentInsets.current
        val topPadPx = with(LocalDensity.current) { insets.top.toPx() }

        Column(modifier = Modifier.fillMaxSize()) {
            // 不足一屏时上方留白，效果即「从上方截断」（底部对齐）
            Spacer(modifier = Modifier.weight(1f))

            Box(modifier = Modifier.fillMaxWidth()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (groupHeights.isEmpty()) return@Canvas

                    val x = axisX.toPx()
                    val stroke = 1.dp.toPx()
                    val dotCenterOffset = DotTop.toPx() + 3.5.dp.toPx()
                    val capPx = 14.dp.toPx()

                    var cursor = topPadPx
                    groupHeights.forEachIndexed { index, groupHeight ->
                        val dotY = cursor + dotCenterOffset
                        val isFirst = index == 0
                        val isLast = index == groupHeights.lastIndex

                        val startY = if (isFirst) (dotY - capPx).coerceAtLeast(0f) else dotY
                        drawLine(
                            color = AxisColor,
                            start = Offset(x, startY),
                            end = Offset(x, dotY),
                            strokeWidth = stroke,
                        )

                        cursor += groupHeight

                        val endY = when {
                            isLast -> (dotY + capPx).coerceAtMost(size.height)
                            else -> cursor
                        }
                        drawLine(
                            color = AxisColor,
                            start = Offset(x, dotY),
                            end = Offset(x, endY),
                            strokeWidth = stroke,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        // 只观察按下事件用于收起底栏，不消费，
                        // 否则会与标题的长按手势竞争
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press) onUserInteract()
                                }
                            }
                        },
                ) {
                    Spacer(Modifier.height(insets.top))

                    groups.forEachIndexed { index, group ->
                        Box(
                            modifier = Modifier.onSizeChanged { size ->
                                if (index >= groupHeights.size) {
                                    groupHeights.add(size.height)
                                } else {
                                    groupHeights[index] = size.height
                                }
                            },
                        ) {
                            TimelineGroup(
                                group = group,
                                axisX = axisX,
                                axisColumnWidth = dateColumnWidth,
                                expandedId = if (selection.isActive) null else expandedId,
                                selection = selection,
                                onClickTitle = { entry ->
                                    onUserInteract()
                                    if (selection.isActive) {
                                        selection.toggle(entry.id)
                                    } else {
                                        expandedId = if (expandedId == entry.id) null else entry.id
                                    }
                                },
                                onLongPressTitle = { entry, pos ->
                                    onUserInteract()
                                    if (!selection.isActive) {
                                        tooltipAnchor = pos
                                        tooltipEntry = entry
                                    }
                                },
                                onLongPressDate = {
                                    onUserInteract()
                                    showCalendar = true
                                },
                            )
                        }
                    }

                    // 滚到底时最后一组停在悬浮底栏上方
                    Spacer(Modifier.height(insets.bottom))
                }
            }
        }

        if (showCalendar) {
            CalendarSheet(
                onDismiss = { showCalendar = false },
                onPick = { date ->
                    showCalendar = false
                    val idx = groups.indexOfFirst { !it.date.isBefore(date) }
                        .let { if (it == -1) groups.lastIndex else it }
                    if (idx >= 0) {
                        expandedId = groups[idx].entries.firstOrNull()?.id
                    }
                },
            )
        }

        pendingDelete?.let { entry ->
            DeleteConfirmDialog(
                dontAskAgain = dontAskAgain,
                onDontAskAgainChange = { dontAskAgain = it },
                onCancel = { pendingDelete = null },
                onConfirm = {
                    if (dontAskAgain) skipDeleteConfirm = true
                    onDelete(entry)
                    pendingDelete = null
                },
            )
        }

        // ---- 长按标题唤起的 tooltip ----
        tooltipEntry?.let { entry ->
            ActionTooltip(
                anchor = tooltipAnchor,
                actions = listOf(
                    TooltipAction(
                        label = "编辑",
                        icon = IconPencilSmall,
                        onClick = {
                            tooltipEntry = null
                            onEdit(entry)
                        },
                    ),
                    TooltipAction(
                        label = "删除",
                        icon = IconTrash,
                        destructive = true,
                        onClick = {
                            tooltipEntry = null
                            if (skipDeleteConfirm) {
                                onDelete(entry)
                            } else {
                                dontAskAgain = false
                                pendingDelete = entry
                            }
                        },
                    ),
                    TooltipAction(
                        label = "多选",
                        icon = IconChecklist,
                        onClick = {
                            tooltipEntry = null
                            expandedId = null
                            selection.enter(entry.id)
                        },
                    ),
                ),
                onDismiss = { tooltipEntry = null },
            )
        }
    }
}

/**
 * 一个日期节点：左侧圆点 + 日期，右侧该日期的标题列表。
 *
 * 轴线由父级的 Canvas 统一绘制，这里只放圆点，行高完全由内容决定。
 */
@Composable
private fun TimelineGroup(
    group: DiaryGroup,
    axisX: androidx.compose.ui.unit.Dp,
    axisColumnWidth: androidx.compose.ui.unit.Dp,
    expandedId: Long?,
    selection: SelectionState,
    onClickTitle: (DiaryEntry) -> Unit,
    onLongPressTitle: (DiaryEntry, Offset) -> Unit,
    onLongPressDate: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // ---------- 左：圆点 + 日期 ----------
        // 固定宽度，保证"时间轴加日期"整体不超过屏幕左侧 1/4
        Box(modifier = Modifier.width(axisColumnWidth)) {
            // 圆点压在轴线上（白底先铺一层，形成截断的视觉）
            Box(
                modifier = Modifier
                    .padding(start = axisX - 3.5.dp, top = DotTop)
                    .size(7.dp)
                    .background(PaperWhite, CircleShape)
                    .background(Ink, CircleShape),
            )

            Text(
                text = group.date.format(DateTextFormatter),
                modifier = Modifier
                    .padding(start = axisX + DateTextStartOffset, top = DotTop - 5.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { onLongPressDate() })
                    },
                color = Ink,
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Start,
            )
        }

        // ---------- 右：标题列表 ----------
        Column(
            modifier = Modifier
                .weight(1f)
                // 左侧留白：让标题与日期之间保持清晰间距
                .padding(start = TitleStartPadding, end = 16.dp)
                .padding(top = 8.dp),
            // 同一天内多篇标题之间留一点缝隙即可，避免整组显得松散
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            group.entries.forEach { entry ->
                DiaryTitleRow(
                    entry = entry,
                    expanded = entry.id == expandedId,
                    selectionMode = selection.isActive,
                    selected = selection.contains(entry.id),
                    onClick = { onClickTitle(entry) },
                    onLongPress = { pos -> onLongPressTitle(entry, pos) },
                )
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
