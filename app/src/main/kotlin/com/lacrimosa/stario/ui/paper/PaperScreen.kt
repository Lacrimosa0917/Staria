package com.lacrimosa.stario.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.data.PaperNote
import com.lacrimosa.stario.data.SampleData
import com.lacrimosa.stario.ui.component.ActionTooltip
import com.lacrimosa.stario.ui.component.BarSidePadding
import com.lacrimosa.stario.ui.component.DestructiveRed
import com.lacrimosa.stario.ui.component.TooltipAction
import com.lacrimosa.stario.ui.icon.IconChecklist
import com.lacrimosa.stario.ui.icon.IconPencilSmall
import com.lacrimosa.stario.ui.icon.IconTrash
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted
import com.lacrimosa.stario.ui.theme.PaperWhite
import java.time.format.DateTimeFormatter

private val Hairline = Color(0xFFE6E2DA)

/** 卡片日期格式：精确至日。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 多选时日期右移的距离（为左侧选中框让位）。 */
private val SelectShift = 26.dp

/**
 * 纸条（摘录查看页）。
 *
 * README 要求：
 * - 等宽卡片上下排列，卡片颜色略深于背景
 * - 第一行为最近编辑日期（精确至日），新卡片在下方
 * - 长按超过 1 秒弹出 编辑 / 删除 / 多选 的 tooltip
 * - 多选时所有卡片日期右移、左侧出现选中框；
 *   右下角新建按钮变为红色删除按钮，点击批量删除
 * - 第二行开始是正文
 * - 有出处时显示在右下角，前缀破折号
 * - 空列表时显示与时间页相同的 paper slogan
 */
@Composable
fun PaperScreen(
    notes: List<PaperNote> = SampleData.notes,
    onUserInteract: () -> Unit = {},
    onEdit: (PaperNote) -> Unit = {},
    onDelete: (PaperNote) -> Unit = {},
    /** 多选状态变化时通知外部，用于让底栏隐藏新建按钮。 */
    onSelectionModeChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 新的在下方：按编辑日期升序
    val ordered = remember(notes) { notes.sortedBy { it.editedOn } }

    var tooltipNote by remember { mutableStateOf<PaperNote?>(null) }
    // 长按点在窗口里的位置，tooltip 据此决定贴哪个角
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }
    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    // 多选开关同步给外部
    LaunchedEffect(selectionMode) {
        onSelectionModeChange(selectionMode)
    }
    // 离开本页时复位，避免底栏一直不显示新建按钮
    DisposableEffect(Unit) {
        onDispose { onSelectionModeChange(false) }
    }

    if (ordered.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "paper",
                color = InkMuted.copy(alpha = 0.55f),
                fontSize = 13.sp,
            )
        }
        return
    }

    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                // 只观察按下事件用于收起底栏，不消费：
                // 若用 detectTapGestures，它会参与手势竞争并吃掉卡片的长按。
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Press) onUserInteract()
                        }
                    }
                },
        ) {
            Spacer(Modifier.height(8.dp))

            ordered.forEach { note ->
                PaperNoteCard(
                    note = note,
                    selectionMode = selectionMode,
                    selected = selectedIds.contains(note.id),
                    onClick = {
                        onUserInteract()
                        if (selectionMode) {
                            if (selectedIds.contains(note.id)) {
                                selectedIds.remove(note.id)
                                if (selectedIds.isEmpty()) selectionMode = false
                            } else {
                                selectedIds.add(note.id)
                            }
                        }
                    },
                    onLongPress = { pos ->
                        onUserInteract()
                        if (!selectionMode) {
                            tooltipAnchor = pos
                            tooltipNote = note
                        }
                    },
                )
                Spacer(Modifier.height(12.dp))
            }

            // 底部留白，避免最后一张被底栏压住
            Spacer(Modifier.height(12.dp))
        }

        // ---- 长按唤起的 tooltip ----
        tooltipNote?.let { note ->
            ActionTooltip(
                anchor = tooltipAnchor,
                actions = listOf(
                    TooltipAction(
                        label = "编辑",
                        icon = IconPencilSmall,
                        onClick = {
                            tooltipNote = null
                            onEdit(note)
                        },
                    ),
                    TooltipAction(
                        label = "删除",
                        icon = IconTrash,
                        destructive = true,
                        onClick = {
                            tooltipNote = null
                            onDelete(note)
                        },
                    ),
                    TooltipAction(
                        label = "多选",
                        icon = IconChecklist,
                        onClick = {
                            tooltipNote = null
                            selectionMode = true
                            selectedIds.clear()
                            selectedIds.add(note.id)
                        },
                    ),
                ),
                onDismiss = { tooltipNote = null },
            )
        }

        // ---- 多选：右下角红色批量删除按钮 ----
        if (selectionMode) {
            val byId = remember(ordered) { ordered.associateBy { it.id } }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = BarSidePadding, bottom = 18.dp)
                    .size(58.dp)
                    .background(DestructiveRed, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = selectedIds.isNotEmpty(),
                    ) {
                        selectedIds.toList().forEach { id ->
                            byId[id]?.let(onDelete)
                        }
                        selectionMode = false
                        selectedIds.clear()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconTrash,
                    contentDescription = "删除所选",
                    modifier = Modifier.size(22.dp),
                    tint = PaperWhite,
                )
            }
        }
    }
}

/**
 * 一张纸条卡片。
 *
 * 版式：
 * - 第一行：最近编辑日期（多选时右移，左侧出现选中框）
 * - 第二行起：正文
 * - 右下角：出处（可空）
 */
@Composable
private fun PaperNoteCard(
    note: PaperNote,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: (Offset) -> Unit,
) {
    val shift = if (selectionMode) SelectShift else 0.dp

    // 卡片在窗口里的左上角，用来把手势坐标换算成窗口坐标
    var windowOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BarSidePadding)
            .onGloballyPositioned { coords ->
                windowOrigin = coords.positionInWindow()
            }
            // 卡片底色略深于纯白背景，但不影响阅读
            .background(CardPaper, RoundedCornerShape(14.dp))
            .border(1.dp, Hairline, RoundedCornerShape(14.dp))
            // 点击与长按放在同一个 detectTapGestures 里：
            // 拆成 combinedClickable + pointerInput 时，
            // 内层 pointerInput 会先消费按下事件，onClick 收不到。
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = shift),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = note.editedOn.format(DateFormatter),
                    color = InkMuted,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(9.dp))

            // ---------- 正文 ----------
            Text(
                text = note.text,
                modifier = Modifier.padding(start = shift),
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 26.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis,
            )

            // ---------- 出处（右下角）----------
            if (!note.source.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = "— ${note.source}",
                        color = InkMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // ---------- 选中框：多选时出现在最左侧 ----------
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
