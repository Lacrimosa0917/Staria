package com.lacrimosa.staria.ui.editor

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.EditDraft
import com.lacrimosa.staria.data.EditKind
import com.lacrimosa.staria.data.ThemeColor
import com.lacrimosa.staria.ui.component.AnchorCorner
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.GlassCircleButton
import com.lacrimosa.staria.ui.component.GlassPanel
import com.lacrimosa.staria.ui.component.GlassPill
import com.lacrimosa.staria.ui.component.GlassPillIcon
import com.lacrimosa.staria.ui.component.PageGlassLayer
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.component.TooltipSurface
import com.lacrimosa.staria.ui.icon.IconClose
import com.lacrimosa.staria.ui.icon.IconDiary
import com.lacrimosa.staria.ui.icon.IconEssay
import com.lacrimosa.staria.ui.icon.IconExcerpt
import com.lacrimosa.staria.ui.icon.IconRedo
import com.lacrimosa.staria.ui.icon.IconSave
import com.lacrimosa.staria.ui.icon.IconUndo
import java.time.format.DateTimeFormatter
import com.lacrimosa.staria.ui.theme.ActionRadius
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.DialogRadius
import com.lacrimosa.staria.ui.theme.HairLine
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize
import com.lacrimosa.staria.ui.theme.accent
import kotlinx.coroutines.delay

/** 圆形按键直径。保存 / 退出都用它，与查看页保持一致。 */
private val ButtonSize = SecondaryButtonSize

/** 撤回重做胶囊的高度：与圆形按键同高（README：高度相同的胶囊）。 */
private val CapsuleHeight = ButtonSize

/** 胶囊内单个图标的触控宽度。 */
private val CapsuleCellWidth = 36.dp

/** 日期格式：与查看页统一。 */
private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d")

/** 类型标签对应的展示文本。 */
private fun EditKind.label(): String = when (this) {
    EditKind.Diary -> "日记"
    EditKind.Excerpt -> "摘录"
    EditKind.Essay -> "随笔"
}

/** 类型标签对应的图标，用于切换类型的 tooltip。 */
private fun EditKind.icon(): ImageVector = when (this) {
    EditKind.Diary -> IconDiary
    EditKind.Excerpt -> IconExcerpt
    EditKind.Essay -> IconEssay
}

/**
 * 编辑页（三种类型共用）。
 *
 * README 要求：
 * - 左上角退出按键；有未保存修改时弹窗询问是否保存
 * - 右上角圆形保存按钮，其左侧为同高胶囊，内含撤回与重做
 * - 按钮与胶囊悬浮在内容之上，滚动时标题与正文整体滚动
 * - 第一行左侧用刻本宋体显示当前类型
 *   - 日记 / 随笔：该行右侧是标题输入
 *   - 摘录：显示 from，右侧是来源输入
 * - 长按类型文字弹出 tooltip，可切换类型
 * - 第一行与正文之间以日期分割
 * - 日记与摘录为普通文本；随笔为 markdown，边输入边预览
 *
 * 日期：三种类型都沿用创建时的日期，保存不刷新（用户确认的口径）。
 */
@Composable
fun EditorScreen(
    initial: EditDraft,
    onExit: () -> Unit,
    onSave: (EditDraft) -> Long,
    modifier: Modifier = Modifier,
) {
    val history = remember(initial) {
        TextHistory(title = initial.title, source = initial.source, body = initial.body)
    }

    var kind by remember(initial) { mutableStateOf(initial.kind) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showKindTooltip by remember { mutableStateOf(false) }
    var kindAnchor by remember { mutableStateOf(Offset.Zero) }

    // 保存反馈：左上角返回键短暂变宽成「已保存」胶囊。
    // savedNoticeTick 递增用于重置 2 秒计时（连续保存会重新计时）。
    var savedNoticeTick by remember { mutableStateOf(0) }
    var showSavedNotice by remember { mutableStateOf(false) }

    // 当前条目 id。新建时为空，保存后由 onSave 返回的真实 id 更新。
    // 用独立 state 而不是改 initial，避免 remember(initial) 重建编辑器、
    // 清空输入与光标。
    var currentId by remember(initial) { mutableStateOf(initial.id) }

    val bodyScroll = rememberScrollState()

    // 修改过的判定：初始快照之外还有别的记录，或内容与初始不同
    val dirty = history.isDirty()

    fun requestExit() {
        if (dirty) showDiscardDialog = true else onExit()
    }

    fun buildDraft(): EditDraft = initial.copy(
        kind = kind,
        id = currentId,
        title = history.title,
        source = history.source,
        body = history.body,
        // 日期原样带回：三种类型都只写创建日期
        date = initial.date,
    )

    fun doSave() {
        currentId = onSave(buildDraft())
        history.markSaved()
        // 触发左上角「已保存」反馈，并重置 2 秒计时
        showSavedNotice = true
        savedNoticeTick++
    }

    // 2 秒后自动收起「已保存」胶囊（期间点击返回键会提前收起）
    LaunchedEffect(savedNoticeTick) {
        if (savedNoticeTick > 0) {
            delay(2000)
            showSavedNotice = false
        }
    }

    // 系统返回键也走「未保存」拦截，行为与左上角退出键一致。
    // 这里注册的处理器晚于根布局，因此会优先拿到返回事件。
    androidx.activity.compose.BackHandler { requestExit() }

    // PageGlassLayer：本页自己录一份 backdrop，让顶栏的退出/撤回/保存
    // 透出的是本页正文，而不是根布局那层（那只录了四个 tab 的内容）。
    // content 会被录进 backdrop；chrome 里的玻璃控件不录制（否则画自己、栈溢出）。
    PageGlassLayer(
        modifier = modifier,
        content = {
        // ---------- 内容层 ----------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(bodyScroll)
                .statusBarsPadding()
                // 顶栏是悬浮的，内容顶部让出它的高度
                .padding(top = ButtonSize + 28.dp),
        ) {
            // ---------- 第一行：类型 + 标题 / 来源 ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 类型：刻本宋体，长按可切换
                Text(
                    text = kind.label(),
                    modifier = Modifier
                        .onGloballyPositioned { coords ->
                            val p = coords.positionInWindow()
                            // 锚点取文字左下角：tooltip 用 TopLeft 从这里向下展开
                            kindAnchor = Offset(p.x, p.y + coords.size.height)
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { showKindTooltip = true })
                        },
                    fontFamily = KebenSong,
                    fontSize = 16.sp,
                    color = InkMuted,
                )

                if (kind == EditKind.Excerpt) {
                    Text(
                        text = "　from",
                        fontFamily = KebenSong,
                        fontSize = 16.sp,
                        color = InkMuted,
                    )
                    Spacer(Modifier.width(6.dp))
                    InlineField(
                        value = history.source,
                        placeholder = "来源",
                        onValueChange = { history.editSource(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        imeAction = ImeAction.Next,
                    )
                } else {
                    Spacer(Modifier.width(8.dp))
                    InlineField(
                        value = history.title,
                        placeholder = "标题",
                        onValueChange = { history.editTitle(it) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(
                            color = Ink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        imeAction = ImeAction.Next,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ---------- 日期分割 ----------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = initial.date.format(DateFormatter),
                    color = InkMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(HairLine),
                )
            }

            Spacer(Modifier.height(18.dp))

            // ---------- 正文 ----------
            // 随笔走 markdown 实时预览，其余两种是普通文本
            BodyField(
                value = history.body,
                markdown = kind == EditKind.Essay,
                onValueChange = { history.editBody(it) },
            )

            // 底部留白：滚到底时正文停在键盘/导航之上
            Spacer(Modifier.height(120.dp))
        }
        },
        chrome = {
        // ---------- 左上角：退出 / 已保存反馈 ----------
        // 保存后短暂向右拉伸成「已保存」胶囊；期间点击只收起胶囊，不退出。
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = 14.dp),
        ) {
            BackWithSavedNotice(
                showSaved = showSavedNotice,
                onExit = { requestExit() },
                onCollapseNotice = { showSavedNotice = false },
            )
        }

        // ---------- 右上角：撤回重做胶囊 + 保存 ----------
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = BarSidePadding, top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryCapsule(
                canUndo = history.canUndo,
                canRedo = history.canRedo,
                onUndo = { history.undo() },
                onRedo = { history.redo() },
            )
            Spacer(Modifier.width(10.dp))
            SaveButton(onClick = { doSave() })
        }

        // ---------- 切换类型的 tooltip（向下展开，采样本页背景） ----------
        // 与设置页/长按菜单一致：直接在 chrome 内用 TooltipSurface，
        // 走本页 backdrop（否则 ActionTooltip 会转发根布局、采到 tab 内容）。
        if (showKindTooltip) {
            TooltipSurface(
                anchor = kindAnchor,
                actions = EditKind.entries
                    .filterNot { it == kind }
                    .map { target ->
                        TooltipAction(
                            label = "改为${target.label()}",
                            icon = target.icon(),
                            onClick = {
                                showKindTooltip = false
                                kind = target
                            },
                        )
                    },
                onDismiss = { showKindTooltip = false },
                preferredCorner = AnchorCorner.TopLeft,
            )
        }
        },
    )

    // ---------- 未保存确认 ----------
    if (showDiscardDialog) {
        UnsavedChangesDialog(
            onCancel = { showDiscardDialog = false },
            onDiscard = onExit,
            onSave = {
                showDiscardDialog = false
                doSave()
            },
        )
    }
}

/** 单行内联输入框（标题 / 来源）。 */
@Composable
private fun InlineField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    imeAction: ImeAction,
) {
    Box(modifier = modifier) {
        if (value.isEmpty()) {
            Text(text = placeholder, color = InkMuted.copy(alpha = 0.7f), fontSize = 16.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(Ink),
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
        )
    }
}

/**
 * 正文输入区。
 *
 * [markdown] 为 true 时套上 [MarkdownTransformation]：
 * 输入 `# ` 后该行立刻变成标题样式，并且 `#` 与空格被隐藏。
 */
@Composable
private fun BodyField(
    value: String,
    markdown: Boolean,
    onValueChange: (String) -> Unit,
) {
    val transformation = remember { MarkdownTransformation() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BarSidePadding),
    ) {
        if (value.isEmpty()) {
            Text(
                text = if (markdown) "写点什么，# 开头是标题" else "写点什么",
                color = InkMuted.copy(alpha = 0.6f),
                fontSize = 15.sp,
                lineHeight = 28.sp,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = Ink, fontSize = 15.sp, lineHeight = 28.sp),
            cursorBrush = SolidColor(Ink),
            visualTransformation = if (markdown) transformation else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        )
    }
}

/** 撤回重做胶囊（液态玻璃）。 */
@Composable
private fun HistoryCapsule(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    GlassPill(height = CapsuleHeight) {
        GlassPillIcon(
            icon = IconUndo,
            description = "撤回",
            enabled = canUndo,
            onClick = onUndo,
            height = CapsuleHeight,
            cellWidth = CapsuleCellWidth,
        )
        GlassPillIcon(
            icon = IconRedo,
            description = "重做",
            enabled = canRedo,
            onClick = onRedo,
            height = CapsuleHeight,
            cellWidth = CapsuleCellWidth,
        )
    }
}

/** 圆形图标按键。 */
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

/** 保存按键：实心，颜色跟随主题色（简约白回落为墨色）。 */
@Composable
private fun SaveButton(onClick: () -> Unit) {
    GlassCircleButton(
        icon = IconSave,
        description = "保存",
        onClick = onClick,
        filled = true,
        fillColor = LocalThemeColor.current.accent(),
    )
}

/** 「已保存」胶囊展开后的宽度。 */
private val SavedCapsuleWidth = 96.dp

/**
 * 左上角返回键：保存后向右拉伸成「已保存」胶囊。
 *
 * 收起态是原来的圆形返回键；[showSaved] 为 true 时：
 * - 图标淡出，文字「已保存」淡入
 * - 面板宽度用带过冲的弹簧向右拉伸（圆角由圆转胶囊）
 * 文字显示期间点击只收起胶囊（[onCollapseNotice]），不触发退出。
 */
@Composable
private fun BackWithSavedNotice(
    showSaved: Boolean,
    onExit: () -> Unit,
    onCollapseNotice: () -> Unit,
) {
    val morphSpec: SpringSpec<Dp> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val width by animateDpAsState(
        targetValue = if (showSaved) SavedCapsuleWidth else SecondaryButtonSize,
        animationSpec = morphSpec,
        label = "saved-capsule-width",
    )

    // 图标淡出 / 文字淡入
    val iconAlpha by animateFloatAsState(
        targetValue = if (showSaved) 0f else 1f,
        animationSpec = tween(durationMillis = 160),
        label = "saved-icon-alpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (showSaved) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "saved-text-alpha",
    )

    // 主题色蒙层：展开时淡入、收起时淡出。
    // 简约白（argb 为纯白）本身就是白纸底色，不加蒙层，否则会糊成一块色块。
    val themeColor = LocalThemeColor.current
    val tintColor = if (themeColor == ThemeColor.Plain) null else themeColor.accent()
    val tintAlpha by animateFloatAsState(
        targetValue = if (showSaved && tintColor != null) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "saved-tint-alpha",
    )

    GlassPanel(
        backdrop = LocalGlassBackdrop.current,
        // 圆角恒为高度的一半：圆形时是正圆，拉宽后自然成为胶囊
        shape = RoundedCornerShape(SecondaryButtonSize / 2),
        refractionScale = 0.6f,
        edgeHighlight = 1.dp,
        modifier = Modifier
            .height(SecondaryButtonSize)
            .width(width)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = if (showSaved) onCollapseNotice else onExit,
            ),
    ) {
        // 主题色蒙层：压在玻璃之上、内容之下
        if (tintColor != null && tintAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(SecondaryButtonSize / 2))
                    .background(tintColor.copy(alpha = tintAlpha)),
            )
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // 返回图标
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(iconAlpha),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconClose,
                    contentDescription = "退出",
                    modifier = Modifier.size(17.dp),
                    tint = Ink,
                )
            }
            // 「已保存」文字
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(textAlpha),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "已保存",
                    // 有主题色蒙层时用白字，保证在彩色底上可读；
                    // 简约白没有蒙层，仍用墨色。
                    color = if (tintColor != null) PaperWhite else Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** 未保存时的询问弹窗。 */
@Composable
private fun UnsavedChangesDialog(
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    DialogShell(onDismiss = onCancel) {
        Text(
            text = "内容尚未保存",
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "退出前是否保存这次的修改？",
            color = InkMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            DialogAction(label = "取消", onClick = onCancel)
            Spacer(Modifier.width(8.dp))
            DialogAction(label = "不保存", onClick = onDiscard)
            Spacer(Modifier.width(8.dp))
            DialogAction(label = "保存", primary = true, onClick = onSave)
        }
    }
}

/** 弹窗外壳：居中卡片 + 压暗背景。 */
@Composable
private fun DialogShell(
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
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
                content = content,
            )
        }
    }
}

@Composable
private fun DialogAction(
    label: String,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (primary) Ink else Color.Transparent,
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
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (primary) PaperWhite else Ink,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
