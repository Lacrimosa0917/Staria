package com.lacrimosa.staria.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.lacrimosa.staria.data.DarkMode
import com.lacrimosa.staria.data.ThemeColor
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.GlassCircleButton
import com.lacrimosa.staria.ui.component.GlassPanel
import com.lacrimosa.staria.ui.component.PageGlassLayer
import com.lacrimosa.staria.ui.icon.IconCheck
import com.lacrimosa.staria.ui.icon.IconBack
import com.lacrimosa.staria.ui.icon.IconChevronRight
import com.lacrimosa.staria.ui.icon.IconDotPlaceholder
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.GlassSpec
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.PanelRadius
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.PopupRadius
import com.lacrimosa.staria.ui.theme.SecondaryButtonSize
import com.lacrimosa.staria.ui.theme.SecondaryTopBarPad
import androidx.compose.ui.graphics.Color

/** 圆形按键直径。 */
private val ButtonSize = SecondaryButtonSize

/** 色块直径。 */
private val SwatchSize = 40.dp

/** 色块一行放四个（README：八种颜色分两行预览）。 */
private const val SWATCHES_PER_ROW = 4

/**
 * 设置页。
 *
 * README：目前只展示交互按键、不加实际功能。
 *
 * - 左上角返回，右侧刻本宋体「设置」标题
 * - 第一行主题色卡片：`当前：颜色名` + 八个色块（两行）
 * - 第二行深色模式：右侧显示当前模式，点击向下展开 tooltip 三选一
 *
 * 选择结果仍然落盘（[SettingsStore]），否则重进页面又变回默认值，
 * 交互看起来就像没生效。
 */
@Composable
fun SettingsScreen(
    themeColor: ThemeColor,
    darkMode: DarkMode,
    onBack: () -> Unit,
    onPickThemeColor: (ThemeColor) -> Unit,
    onPickDarkMode: (DarkMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDarkTooltip by remember { mutableStateOf(false) }
    // 胶囊在 content 层里的占位坐标（窗口坐标），chrome 层的真实胶囊据此定位
    var darkCapsulePos by remember { mutableStateOf(Offset.Zero) }

    // PageGlassLayer：本页自己录一份 backdrop，让返回圆钮透出的是本页内容，
    // 而不是根布局那层（那只录了四个 tab 的内容）。
    // content 会被录进 backdrop；chrome 里的玻璃控件不录制（否则画自己、栈溢出）。
    PageGlassLayer(
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    // 顶栏悬浮，内容让出它的高度
                    .padding(top = ButtonSize + 34.dp),
            ) {
            // ---------- 主题色卡片 ----------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding)
                    // 纯白卡面，不描边：与 #F5F5F5 底色的层次只靠色差
                    .background(PaperWhite, RoundedCornerShape(PanelRadius))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                // 当前：颜色名
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "当前",
                        fontFamily = KebenSong,
                        fontSize = 15.sp,
                        color = Ink,
                    )
                    Text(
                        text = "：",
                        fontFamily = KebenSong,
                        fontSize = 15.sp,
                        color = Ink,
                    )
                    Text(
                        text = themeColor.label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 八个色块，两行
                ThemeColor.entries.chunked(SWATCHES_PER_ROW).forEachIndexed { rowIndex, row ->
                    if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { color ->
                            ColorSwatch(
                                color = color,
                                selected = color == themeColor,
                                onClick = { onPickThemeColor(color) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---------- 深色模式 ----------
            // 右侧「当前状态」本身是一枚玻璃胶囊：点击后胶囊自身形变成菜单
            // （与随笔查看页「更多」按钮同一套逻辑），而不是弹独立浮层。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding)
                    // 纯白卡面，不描边：与 #F5F5F5 底色的层次只靠色差
                    .background(PaperWhite, RoundedCornerShape(PanelRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showDarkTooltip = true },
                    )
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "深色模式",
                    fontFamily = KebenSong,
                    fontSize = 15.sp,
                    color = Ink,
                )
                Spacer(Modifier.weight(1f))
                // 胶囊本体要采样本页 backdrop，不能画在 content（会画自己、栈溢出），
                // 所以这里只放一个同尺寸占位并记录位置，真正的胶囊在 chrome 里渲染。
                Box(
                    modifier = Modifier
                        .size(CapsuleWidth, CapsuleHeight)
                        .onGloballyPositioned { coords ->
                            darkCapsulePos = coords.positionInWindow()
                        },
                )
            }

            // 底部留白
            Spacer(Modifier.height(40.dp))
        }
        },
        chrome = {
        // ---------- 顶栏 ----------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = SecondaryTopBarPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassCircleButton(
                icon = IconBack,
                description = "返回",
                onClick = onBack,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "设置",
                fontFamily = KebenSong,
                fontSize = 19.sp,
                color = Ink,
            )
        }

        // ---------- 深色模式胶囊（展开时点空白处收起） ----------
        // 声明在胶囊之前，层级低于胶囊，不会挡住面板自身
        if (showDarkTooltip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showDarkTooltip = false },
                    ),
            )
        }

        // ---------- 深色模式胶囊本体 ----------
        // 位置取 content 层占位的窗口坐标；面板展开时向下生长（顶部对齐）。
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        darkCapsulePos.x.roundToInt(),
                        darkCapsulePos.y.roundToInt(),
                    )
                },
        ) {
            DarkModeCapsule(
                current = darkMode,
                expanded = showDarkTooltip,
                onToggle = { showDarkTooltip = !showDarkTooltip },
                onPick = { mode ->
                    onPickDarkMode(mode)
                    showDarkTooltip = false
                },
            )
        }
        },
    )
}

/** 一个色块。选中时套一圈墨色描边，中间以墨色旋转45°的小方块收边。 */
@Composable
private fun ColorSwatch(
    color: ThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(SwatchSize)
            .background(color.argb.toComposeColor(), CircleShape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Ink else Border,
                shape = CircleShape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}

/** Long(0xAARRGGBB) → Compose Color。 */
private fun Long.toComposeColor(): Color = Color(this.toInt())

// ---------- 深色模式胶囊（点击后自身形变成菜单） ----------

/** 收起态胶囊高度。 */
private val CapsuleHeight = 30.dp

/** 收起态胶囊宽度（容纳「跟随系统」这样的最长文案 + 箭头）。 */
private val CapsuleWidth = 104.dp

/** 展开态菜单宽度。 */
private val CapsuleMenuWidth = 132.dp

/** 菜单单项高度。 */
private val CapsuleItemHeight = 34.dp

/** 菜单上下内边距。 */
private val CapsuleMenuPadV = 4.dp

/**
 * 深色模式的「当前状态」胶囊。
 *
 * 收起时是一枚玻璃小胶囊（当前模式名 + 向下箭头）；点击后**自身**形变成
 * 三项菜单——与随笔查看页「更多」按钮同一套逻辑：
 * 宽度/高度/圆角用同一套带过冲的弹簧形变，文字淡入淡出。
 *
 * 展开时顶部固定、向下生长，所以不会顶动上面那行内容；
 * 面板用 [GlassPanel] 采样本页 backdrop（位于 chrome 内，不参与录制）。
 */
@Composable
private fun DarkModeCapsule(
    current: DarkMode,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPick: (DarkMode) -> Unit,
) {
    val menuHeight = CapsuleItemHeight * DarkMode.entries.size + CapsuleMenuPadV * 2

    // 形变弹簧：与随笔查看页「更多」按钮完全一致（带过冲，展开后小幅回弹）
    val morphSpec: SpringSpec<Dp> = spring(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val width by animateDpAsState(
        targetValue = if (expanded) CapsuleMenuWidth else CapsuleWidth,
        animationSpec = morphSpec,
        label = "dark-capsule-width",
    )
    val height by animateDpAsState(
        targetValue = if (expanded) menuHeight else CapsuleHeight,
        animationSpec = morphSpec,
        label = "dark-capsule-height",
    )
    val corner by animateDpAsState(
        targetValue = if (expanded) PopupRadius else CapsuleHeight / 2,
        animationSpec = morphSpec,
        label = "dark-capsule-corner",
    )

    // 收起态文字淡出、菜单淡入（与随笔查看页的 90/140ms 节奏一致）
    val capsuleTextAlpha by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "dark-capsule-text",
    )
    val menuAlpha by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "dark-capsule-menu",
    )

    // 面板自由尺寸、向右下生长（右上角锚定），不限制外层高度，
    // 否则展开后的菜单会被裁掉。
    Box(
        modifier = Modifier,
        contentAlignment = Alignment.TopEnd,
    ) {
        GlassPanel(
            backdrop = LocalGlassBackdrop.current,
            shape = RoundedCornerShape(corner),
            refractionScale = 0.6f,
            // 展开时收掉上下高光边，正面看起来更干净；材质切到 tooltip 那一套
            edgeHighlight = if (expanded) 0.dp else 1.dp,
            blurDp = if (expanded) GlassSpec.TOOLTIP_BLUR_DP else GlassSpec.BLUR_DP,
            surfaceAlpha = if (expanded) GlassSpec.TOOLTIP_SURFACE_ALPHA else 0f,
            modifier = Modifier.size(width, height),
        ) {
            // ---------- 收起态：当前模式名 + 向下箭头 ----------
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(capsuleTextAlpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = current.label,
                    fontSize = 13.sp,
                    color = InkMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    // 用右尖括号旋转 90° 表示「向下展开」
                    imageVector = IconChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(if (expanded) -90f else 90f),
                    tint = InkMuted,
                )
            }

            // ---------- 收起态：整块可点展开 ----------
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onToggle,
                        ),
                )
            }

            // ---------- 展开态：三项菜单 ----------
            if (menuAlpha > 0.01f) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(menuAlpha)
                        .padding(vertical = CapsuleMenuPadV),
                ) {
                    DarkMode.entries.forEach { mode ->
                        CapsuleMenuRow(
                            label = mode.label,
                            selected = mode == current,
                            onClick = { onPick(mode) },
                        )
                    }
                }
            }
        }
    }
}

/** 胶囊菜单里的一行：选中项显示勾，其余用同尺寸占位保持左对齐。 */
@Composable
private fun CapsuleMenuRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(CapsuleItemHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (selected) IconCheck else IconDotPlaceholder,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Ink,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
