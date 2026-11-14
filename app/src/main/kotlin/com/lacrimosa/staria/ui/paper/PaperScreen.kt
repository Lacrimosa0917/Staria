package com.lacrimosa.staria.ui.paper

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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.LocalContentInsets
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.icon.IconChecklist
import com.lacrimosa.staria.ui.icon.IconPencilSmall
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.BorderSelected
import com.lacrimosa.staria.ui.theme.CardGap
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.SelectionBoxRadius
import com.lacrimosa.staria.ui.theme.SelectionBoxSize
import com.lacrimosa.staria.ui.theme.SelectionShift
import java.time.format.DateTimeFormatter

/** 卡片日期格式：精确至日。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 多选时日期右移的距离（为左侧选中框让位）。 */
private val SelectShift = SelectionShift

/**
 * 纸条（摘录查看页）。
 *
 * README 要求：
 * - 等宽卡片上下排列，卡片颜色略深于背景
 * - 第一行为最近编辑日期（精确至日），新卡片在下方
 * - 长按超过 1 秒弹出 编辑 / 删除 / 多选 的 tooltip
 * - 多选时所有卡片日期右移、左侧出现选中框；
 *   右下角新建按钮变为红色删除按钮（由底栏渲染，见 [SelectionState]）
 * - 第二行开始是正文；有出处时显示在右下角并带破折号
 * - 空列表时显示 paper slogan
 * - 点击卡片：以首页同款卡片样式展开单张，居中展示，其余压暗
 */
@Composable
fun PaperScreen(
    notes: List<PaperNote> = SampleData.notes,
    selection: SelectionState,
    onUserInteract: () -> Unit = {},
    onEdit: (PaperNote) -> Unit = {},
    onDelete: (PaperNote) -> Unit = {},
    /** 点击卡片时把该条抛出，由根布局渲染全屏展开浮层。 */
    onExpand: (PaperNote) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 新的在下方：按编辑日期升序
    val ordered = remember(notes) { notes.sortedBy { it.editedOn } }

    var tooltipNote by remember { mutableStateOf<PaperNote?>(null) }
    // 长按点在窗口里的位置，tooltip 据此决定贴哪个角
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }

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
        // 空列表也可能残留展开态，这里不处理即自然为空
        return
    }

    val scrollState = rememberScrollState()

    val insets = LocalContentInsets.current

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

            ordered.forEach { note ->
                PaperNoteCard(
                    note = note,
                    selectionMode = selection.isActive,
                    selected = selection.contains(note.id),
                    onClick = {
                        onUserInteract()
                        if (selection.isActive) {
                            selection.toggle(note.id)
                        } else {
                            // 交给根布局渲染全屏浮层，保证能盖住顶栏与底栏
                            onExpand(note)
                        }
                    },
                    onLongPress = { pos ->
                        onUserInteract()
                        if (!selection.isActive) {
                            tooltipAnchor = pos
                            tooltipNote = note
                        }
                    },
                )
                Spacer(Modifier.height(CardGap))
            }

            // 滚到底时最后一张停在悬浮底栏之上（含导航栏高度）
            Spacer(Modifier.height(insets.bottom))
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
                            selection.enter(note.id)
                        },
                    ),
                ),
                onDismiss = { tooltipNote = null },
            )
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
            .background(CardPaper, RoundedCornerShape(CardRadius))
            .border(1.dp, Border, RoundedCornerShape(CardRadius))
            // 点击与长按放在同一个 detectTapGestures 里：
            // 拆成两个 modifier 时内层会先消费按下事件，导致另一种手势失效
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
                        color = CardPaper,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
