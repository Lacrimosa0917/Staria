package com.lacrimosa.staria.ui.trash

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.TrashItem
import com.lacrimosa.staria.data.TrashKind
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.GlassCircleButton
import com.lacrimosa.staria.ui.component.GlassPanel
import com.lacrimosa.staria.ui.component.PageGlassLayer
import com.lacrimosa.staria.ui.component.PressScaleSpec
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.component.clickableNoIndication
import com.lacrimosa.staria.ui.component.pressScale
import com.lacrimosa.staria.ui.icon.IconChecklist
import com.lacrimosa.staria.ui.icon.IconBack
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.icon.IconRestore
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import com.lacrimosa.staria.ui.theme.ActionRadius
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.BorderDisabled
import com.lacrimosa.staria.ui.theme.BorderSelected
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.DialogRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize
import com.lacrimosa.staria.ui.theme.SelectionBoxRadius
import com.lacrimosa.staria.ui.theme.SelectionBoxSize
import com.lacrimosa.staria.ui.theme.SelectionShift
import com.lacrimosa.staria.ui.theme.accent

/** 日期格式：与查看页统一。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 圆形按键直径：与其它二级页保持一致。 */
private val ButtonSize = SecondaryButtonSize

/** 卡片高度固定（README：等宽等高），正文只显示一行。 */
private val CardHeight = 84.dp

/** 多选时内容右移的距离（为左侧选中框让位）。 */
private val SelectShift = SelectionShift

/**
 * 星尘（回收站）。
 *
 * README 要求：
 * - 二级页面；左上角返回，右侧品牌宋体「星尘」标题
 * - 条目以等宽等高卡片排列，新删除的在上方
 * - 卡片第一行「类型 + 日期」，第二行正文预览单行省略
 * - 长按一秒弹出 恢复 / 删除 / 多选
 *   - 删除为永久删除
 *   - 恢复移出星尘，回到对应查看页
 * - 多选：卡片第一行左移、左侧出现选中框；
 *   左下角批量恢复、右下角批量删除，两者执行前都要确认
 */
@Composable
fun TrashScreen(
    items: List<TrashItem>,
    onBack: () -> Unit,
    onRestore: (List<Long>) -> Unit,
    onDelete: (List<Long>) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ordered 为仓库层给的数据顺序（已按删除顺序倒序）
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<Long>() }

    var tooltipItem by remember { mutableStateOf<TrashItem?>(null) }
    var tooltipAnchor by remember { mutableStateOf(Offset.Zero) }

    // 待确认的批量动作：null / 恢复 / 删除
    var pendingRestore by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf(false) }

    /**
     * 兜底确认：多选状态下如果列表被清空（例如恢复/删除后只剩空），
     * 顺手退出多选，否则底栏会停在一个没内容的模式里。
     */
    if (items.isEmpty() && selectionMode) {
        selectionMode = false
        selected.clear()
    }

    fun exitSelection() {
        selectionMode = false
        selected.clear()
    }

    // 系统返回键：多选时只退出多选，不关闭星尘页。
    // 注册在页面内，优先级高于根布局那个「关星尘」的处理器。
    // 确认弹窗、tooltip 一并先收掉，避免留下浮层。
    BackHandler(enabled = selectionMode || pendingRestore || pendingDelete || tooltipItem != null) {
        when {
            pendingRestore -> pendingRestore = false
            pendingDelete -> pendingDelete = false
            tooltipItem != null -> tooltipItem = null
            selectionMode -> exitSelection()
        }
    }

    // PageGlassLayer：本页自己录一份 backdrop，让顶栏圆钮透出的是本页内容，
    // 而不是根布局那层（那只录了四个 tab 的内容）。
    // content 会被录进 backdrop；chrome 里的玻璃控件不录制（否则画自己、栈溢出）。
    PageGlassLayer(
        modifier = modifier,
        content = {
            // ---------- 内容 ----------
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "星尘",
                        color = InkMuted.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        // 顶栏按钮悬浮，内容让出它的高度
                        .padding(top = ButtonSize + 34.dp),
                ) {
                    items.forEach { item ->
                        TrashCard(
                            item = item,
                            selectionMode = selectionMode,
                            selected = selected.contains(item.trashId),
                            onClick = {
                                if (selectionMode) {
                                    if (selected.contains(item.trashId)) {
                                        selected.remove(item.trashId)
                                    } else {
                                        selected.add(item.trashId)
                                    }
                                }
                            },
                            onLongPress = { pos ->
                                if (!selectionMode) {
                                    tooltipAnchor = pos
                                    tooltipItem = item
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    // 滚到底时最后一张停在底部按钮之上
                    Spacer(Modifier.height(CardHeight + 40.dp))
                }
            }
        },
        chrome = {
        // ---------- 顶栏 ----------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(icon = IconBack, description = "返回", onClick = onBack)
            Spacer(Modifier.width(14.dp))
            Text(
                text = "星尘",
                fontFamily = KebenSong,
                fontSize = 19.sp,
                color = Ink,
            )
        }

        // ---------- 多选态底部：左恢复 / 右删除（淡入淡出） ----------
        AnimatedVisibility(
            visible = selectionMode,
            enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    label = "恢复",
                    icon = IconRestore,
                    enabled = selected.isNotEmpty(),
                    onClick = { pendingRestore = true },
                )
                CircleActionButton(
                    icon = IconTrash,
                    description = "删除",
                    enabled = selected.isNotEmpty(),
                    onClick = { pendingDelete = true },
                )
            }
        }
        },
    )

    // ---------- 长按 tooltip ----------
    tooltipItem?.let { item ->
        ActionTooltip(
            anchor = tooltipAnchor,
            actions = listOf(
                TooltipAction(
                    label = "恢复",
                    icon = IconRestore,
                    onClick = {
                        tooltipItem = null
                        onRestore(listOf(item.trashId))
                    },
                ),
                TooltipAction(
                    label = "删除",
                    icon = IconTrash,
                    destructive = true,
                    onClick = {
                        tooltipItem = null
                        onDelete(listOf(item.trashId))
                    },
                ),
                TooltipAction(
                    label = "多选",
                    icon = IconChecklist,
                    onClick = {
                        tooltipItem = null
                        selectionMode = true
                        selected.clear()
                        selected.add(item.trashId)
                    },
                ),
            ),
            onDismiss = { tooltipItem = null },
        )
    }

    // ---------- 批量恢复确认 ----------
    if (pendingRestore) {
        ConfirmDialog(
            title = "恢复 ${selected.size} 条记录？",
            message = "恢复后将回到对应的查看页。",
            confirmLabel = "恢复",
            destructive = false,
            onCancel = { pendingRestore = false },
            onConfirm = {
                pendingRestore = false
                onRestore(selected.toList())
                exitSelection()
            },
        )
    }

    // ---------- 批量删除确认 ----------
    if (pendingDelete) {
        ConfirmDialog(
            title = "永久删除 ${selected.size} 条记录？",
            message = "此操作不可撤销，删除后不再保留。",
            confirmLabel = "删除",
            destructive = true,
            onCancel = { pendingDelete = false },
            onConfirm = {
                pendingDelete = false
                onDelete(selected.toList())
                exitSelection()
            },
        )
    }
}

/** mutableStateListOf 的 Long 版本别名，避免顶部再引一次包。 */
/**
 * 一张回收站卡片。
 *
 * 版式（README）：
 * - 第一行：类型 + 日期（多选时整体右移，左侧出现选中框）
 * - 第二行：正文预览，单行，超出省略
 * - 卡片等宽等高
 */
@Composable
private fun TrashCard(
    item: TrashItem,
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
            .height(CardHeight)
            .onGloballyPositioned { windowOrigin = it.positionInWindow() }
            // 纯白卡面，不描边：与 #F5F5F5 底色的层次只靠色差
            .background(PaperWhite, RoundedCornerShape(CardRadius))
            // 点击与长按必须在同一个 detectTapGestures 里
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = shift),
        ) {
            // 第一行：类型 + 日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.kind.label,
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.date.format(DateFormatter),
                    color = InkMuted,
                    fontSize = 11.sp,
                )
            }

            Spacer(Modifier.height(9.dp))

            // 第二行：正文预览，只显示一行
            Text(
                text = previewText(item),
                color = Ink,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // 选中框：多选时出现在最左侧
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
                        color = PaperWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * 卡片第二行展示的文本。
 *
 * 摘录没有标题，直接展示正文；有来源时并在后面，
 * 让预览也能看出这条摘录的出处。
 */
private fun previewText(item: TrashItem): String = when {
    item.kind == TrashKind.Excerpt && !item.source.isNullOrBlank() ->
        "${item.body}　— ${item.source}"
    else -> item.body.ifBlank { item.title }
}

/** 圆形图标按键（返回用）。 */
@Composable
private fun CircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    GlassCircleButton(
        icon = icon,
        description = description,
        onClick = onClick,
    )
}

/** 胶囊按钮：左下角的批量恢复（液态玻璃）。 */
@Composable
private fun PillButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val inflatePx = with(LocalDensity.current) { 3.dp.toPx() }

    GlassPanel(
        backdrop = LocalGlassBackdrop.current,
        shape = RoundedCornerShape(SecondaryButtonSize / 2),
        refractionScale = 0.6f,
        edgeHighlight = 1.dp,
        // 按压扩大回弹：layerBlock 缩放玻璃外壳，内容 Row 用 graphicsLayer 同步缩放
        layerBlock = {
            val p = press.value
            if (p > 0f) {
                val maxScale = if (size.width > 0f) {
                    (size.width + inflatePx * 2) / size.width
                } else {
                    1f
                }
                val s = 1f + (maxScale - 1f) * p
                scaleX = s
                scaleY = s
            }
        },
        modifier = Modifier
            .height(SecondaryButtonSize)
            .clickableNoIndication(enabled = enabled, onClick = onClick)
            // 只检测按下/抬起用于缩放，不消费事件，点击仍由 clickableNoIndication 处理
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { press.animateTo(1f, PressScaleSpec) }
                    waitForUpOrCancellation()
                    scope.launch { press.animateTo(0f, PressScaleSpec) }
                }
            },
    ) {
        Row(
            modifier = Modifier
                // 关键：只由内容撑开宽度，不能 fillMaxSize ——
                // 否则胶囊会占满整个底栏，把右侧删除按钮挤出去。
                .height(SecondaryButtonSize)
                .wrapContentWidth()
                .graphicsLayer {
                    val p = press.value
                    if (p > 0f) {
                        val maxScale = if (size.width > 0f) {
                            (size.width + inflatePx * 2) / size.width
                        } else {
                            1f
                        }
                        val s = 1f + (maxScale - 1f) * p
                        scaleX = s
                        scaleY = s
                    }
                }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = if (enabled) Ink else InkMuted.copy(alpha = 0.4f),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = label,
                color = if (enabled) Ink else InkMuted.copy(alpha = 0.4f),
                fontSize = 13.sp,
            )
        }
    }
}

/** 圆形按钮：右下角的批量删除（红色实心，与纸条页的删除按钮呼应）。 */
@Composable
private fun CircleActionButton(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SecondaryButtonSize)
            // pressScale 在 background 之前：实心圆本体与图标一起扩大回弹
            .pressScale(enabled = enabled)
            .background(
                if (enabled) Danger else BorderDisabled,
                CircleShape,
            )
            .clickableNoIndication(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(SelectionBoxSize),
            tint = PaperWhite,
        )
    }
}

/** 批量操作前的确认弹窗。 */
@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    // Dialog 是独立窗口，CompositionLocal 不跨窗口传递，
    // 在 Dialog 外读取主题色，再在窗口内重新 provide。
    val themeColor = LocalThemeColor.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onCancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(PaperWhite, RoundedCornerShape(DialogRadius))
                    .border(1.dp, Border, RoundedCornerShape(DialogRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = title,
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message,
                    color = InkMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogButton(label = "取消", primary = false, onClick = onCancel)
                    Spacer(Modifier.width(10.dp))
                    DialogButton(
                        label = confirmLabel,
                        primary = true,
                        destructive = destructive,
                        onClick = onConfirm,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    // 主按钮（确认）用主题色：简约白回落为墨色，其余主题取其强调色
    val accent = LocalThemeColor.current.accent()
    Box(
        modifier = Modifier
            .background(
                color = if (primary) accent else Color.Transparent,
                shape = RoundedCornerShape(ActionRadius),
            )
            .then(
                if (primary) Modifier else Modifier.border(1.dp, Border, RoundedCornerShape(ActionRadius)),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (primary) PaperWhite else Ink,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
