package com.lacrimosa.staria.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.lacrimosa.staria.R
import com.lacrimosa.staria.ui.icon.IconDiary
import com.lacrimosa.staria.ui.icon.IconEssay
import com.lacrimosa.staria.ui.icon.IconExcerpt
import com.lacrimosa.staria.ui.icon.IconHome
import com.lacrimosa.staria.ui.icon.IconHomeSelected
import com.lacrimosa.staria.ui.icon.IconHourglass
import com.lacrimosa.staria.ui.icon.IconHourglassSelected
import com.lacrimosa.staria.ui.icon.IconNotebook
import com.lacrimosa.staria.ui.icon.IconNotebookSelected
import com.lacrimosa.staria.ui.icon.IconPaperStack
import com.lacrimosa.staria.ui.icon.IconPaperStackSelected
import com.lacrimosa.staria.ui.icon.IconPlus
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.accent
import com.lacrimosa.staria.ui.theme.stariaGlassEffects
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 底栏四个页面。 */
enum class StariaTab(
    val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Home(R.string.tab_home, IconHome, IconHomeSelected),
    Time(R.string.tab_time, IconHourglass, IconHourglassSelected),
    Paper(R.string.tab_paper, IconPaperStack, IconPaperStackSelected),
    Think(R.string.tab_think, IconNotebook, IconNotebookSelected),
}

/** 新建菜单三项。 */
enum class CreateKind(val labelRes: Int, val icon: ImageVector) {
    Diary(R.string.create_diary, IconDiary),
    Excerpt(R.string.create_excerpt, IconExcerpt),
    Essay(R.string.create_essay, IconEssay),
}

// ---------- 尺寸 ----------

/** 底栏高度。新建按钮直径与之相等，两者底边对齐后高度完全一致。 */
private val BarHeightDp = 58.dp

/** 新建按钮直径。 */
private val CreateButtonSize = BarHeightDp

/** 底栏与新建按钮之间的间隙。 */
private val BarGap = 12.dp

/** 滑块（水珠）比面板矮多少。三层架构里 L2/L3 都矮这一截。 */
private val DropletInset = 4.dp

/** 底栏左右留白。页面里需要对齐底栏元素时引用这个常量，避免两处硬编码走样。 */
val BarSidePadding = 20.dp

/** 底栏总高度。 */
val BarHeight: Dp = BarHeightDp

/** 底栏第一个页面按键的圆心相对屏幕左缘的横向偏移。时间页的竖轴对齐它。 */
fun navFirstItemCenterX(screenWidth: Dp): Dp {
    val expandedWidth = screenWidth - CreateButtonSize - BarGap
    val cellWidth = (expandedWidth - BarSidePadding * 2) / StariaTab.entries.size
    return BarSidePadding + cellWidth / 2
}

/** 底栏第一个页面按键的左边缘。时间页的竖线对齐它。 */
fun navFirstItemLeftX(): Dp = BarSidePadding

/**
 * 底部：页面底栏（液态玻璃 + 滑块指示器）+ 新建按钮。
 *
 * ## 三层结构（移植自 MeloX / BiliNext 的 LiquidGlassTabsBar）
 *
 * - **L1 可见面板**：真正的胶囊玻璃，含 vibrancy + blur + lens + 高光/阴影，
 *   承载四个 tab 的图标与文字。
 * - **L2 捕获层**：与 L1 同宽、矮 [DropletInset]，**同一份 tab 内容但图标染成强调色**，
 *   并作为 backdrop 录制下来。它是「滑块里能看见彩色图标」的来源。
 * - **L3 滑块（水珠）**：一个圆角小胶囊，采样 L2 的内容并施加更强的折射，
 *   横向停在当前格上。拖动时跟手，松手按弹簧吸附。
 *
 * 之所以要 L2：如果滑块直接采样 L1，它采到的就是「已经画好的面板 + 普通图标」，
 * 滑块里那格图标不会变成强调色。中间加一层专供采样的副本，
 * 才能做到「滑块罩住哪一格，哪一格就是强调色」。
 */
@Composable
fun StariaBottomBar(
    current: StariaTab,
    onSelect: (StariaTab) -> Unit,
    onCreate: (CreateKind) -> Unit,
    createExpanded: Boolean,
    onCreateExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deleteAction: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val reserved = CreateButtonSize + BarGap
        val barWidth = maxWidth - reserved

        NavBar(
            modifier = Modifier.align(Alignment.BottomStart),
            current = current,
            onSelect = onSelect,
            barWidth = barWidth,
        )

        // 新建按钮 ↔ 红色删除按钮：交叉淡入淡出，避免硬切
        AnimatedVisibility(
            visible = deleteAction == null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            CreateButton(
                expanded = createExpanded,
                onExpandedChange = onCreateExpandedChange,
                onCreate = onCreate,
            )
        }

        AnimatedVisibility(
            visible = deleteAction != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            if (deleteAction != null) {
                DeleteButton(onClick = deleteAction)
            }
        }
    }
}

/**
 * 底栏本体：常驻展开，不再收起。
 *
 * 交互（对齐 MeloX）：
 * - 点击任意 tab：滑块弹簧滑过去 + 该 tab 内容淡入
 * - 在滑块上左右拖动：滑块跟手，松手吸附到最近一格并切换
 * - 滑块始终显示，不再有「收起成圆」这一态
 */
@Composable
private fun NavBar(
    modifier: Modifier = Modifier,
    current: StariaTab,
    onSelect: (StariaTab) -> Unit,
    barWidth: Dp,
) {
    val tabs = StariaTab.entries
    val tabCount = tabs.size
    val selectedIndex = tabs.indexOf(current).coerceAtLeast(0)

    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ---- 几何：让滑块在最左/最右时，四边留白完全相等 ----
    //
    // 目标是「滑块停在第一格时，它的左边缘距胶囊左缘 == 上边缘距胶囊上缘」。
    // 上部留白由 DropletInset 决定（滑块比面板矮 2×DropletInset），
    // 所以左右也要等于这个值。推导：
    //
    //   设格子宽 w、滑块宽 d = 1.15w + 2dp、内缘 inner = DropletInset，
    //   第 0 格中心到左缘 = inner + d/2，
    //   相邻格中心距 = (barW - 2·inner - d) / (n - 1)  ← 首末格中心正好被内缘夹住
    //   联立 d = 1.15w + 2 解得：
    //     w = (barW - 2·inner - 2) / (4.15)
    //
    //   （4.15 = n - 1 + 1.15，n = 4 时成立）
    //
    // 这样格子本身会比原来宽一点 —— 也就是「略微增加四个按键之间的间距」，
    // 视觉上四个按键更疏朗，滑块贴边也贴得准。
    val innerEdge = DropletInset
    val tabCountF = tabCount.toFloat()
    val tabWidth = ((barWidth - innerEdge * 2 - 2.dp) / (tabCountF - 1f + 1.15f))
    val tabWidthPx = with(density) { tabWidth.toPx() }

    val dropletWidth = tabWidth * 1.15f + 2.dp
    val dropletHeight = BarHeightDp - DropletInset * 2
    val dropletWidthPx = with(density) { dropletWidth.toPx() }

    // 第 0 格中心（相对胶囊左缘）= 内缘 + 半个滑块
    val firstCenterPx = with(density) { (innerEdge + dropletWidth / 2).toPx() }
    // 相邻格中心距
    val centerStepPx = with(density) {
        (barWidth - innerEdge * 2 - dropletWidth).toPx() / (tabCountF - 1f)
    }

    // tab 行的左右内缩：让第 i 格中心恰好落在 firstCenter + i×step 上
    val rowPad = innerEdge + dropletWidth / 2 - tabWidth / 2

    // ---- 阻尼拖拽动画 ----
    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentTabs by rememberUpdatedState(tabs)
    val currentTabWidthPx by rememberUpdatedState(tabWidthPx)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val damped = remember(tabCount) {
        StariaDampedDragAnimation(
            animationScope = scope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            // 按下时放大到 1.39×（78/56，MeloX 原值）
            pressedScale = 78f / 56f,
            onTap = { position ->
                // position 是**整行内容坐标**（行已内缩 rowPad）。
                //
                // ⚠ 必须用「向下取整」而不是四舍五入：
                // 第 i 格覆盖 [i·w, (i+1)·w)，中心在 (i+0.5)·w。
                // 若用 round(x/w)，点在第 i 格中心会得到 round(i+0.5) = i+1
                // —— 也就是「点当前项反而跳到下一格」，正是这条 bug 的成因。
                val index = (position.x / currentTabWidthPx.coerceAtLeast(1f))
                    .toInt()
                    .fastCoerceIn(0, tabCount - 1)
                currentOnSelect(currentTabs[index])
            },
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, tabCount - 1)
                animateToValue(target.toFloat())
                if (target != currentSelectedIndex) {
                    currentOnSelect(currentTabs[target])
                }
            },
            onDrag = { _, dragAmount ->
                updateValue(
                    (targetValue + dragAmount.x / currentTabWidthPx.coerceAtLeast(1f))
                        .fastCoerceIn(0f, (tabCount - 1).toFloat()),
                )
            },
        )
    }

    // 外部选中变化 → 滑块滑过去
    var prevSelected by remember { mutableStateOf(selectedIndex) }
    LaunchedEffect(selectedIndex) {
        if (damped.targetValue != selectedIndex.toFloat()) {
            // 从别的 tab 切回来时带一点过冲（Q 弹），普通切换用临界阻尼
            if (prevSelected != selectedIndex) {
                damped.animateToValueBouncy(selectedIndex.toFloat())
            } else {
                damped.animateToValue(selectedIndex.toFloat())
            }
        }
        prevSelected = selectedIndex
    }

    // ---- backdrop ----
    // 页面背景：面板用它采样「下面的内容」。
    val rootBackdrop = LocalGlassBackdrop.current
    // 面板自身：滑块用它采样「底栏这一层」。
    //
    // 这是本轮的关键修正。之前滑块直接采 rootBackdrop（页面背景），
    // 于是滑块里显示的是**页面内容**而不是底栏的纸面 —— 看起来就是
    // 「滑块区域直接穿透底栏」。MeloX 的做法是让滑块采 L2 捕获层
    // （面板的同尺寸副本），滑块里永远是底栏自己的材质。
    //
    // 这里用 drawBackdrop 的 exportedBackdrop 参数：面板在画 backdrop 的
    // 同时把它录进 barBackdrop，滑块再以它为源，天然同源、不会成环
    // （这是库文档 Glass Bottom Sheet 一节给出的正解）。
    val barBackdrop = rememberLayerBackdrop()

    Box(
        modifier = modifier
            .width(barWidth)
            .height(BarHeightDp),
    ) {
        // ══ L1：面板玻璃 ══
        // 面板随按压一起变大，但**采样到的页面正文不会跟着变形**。
        //
        // 这个「只缩自己不缩背景」的效果正是库 `layerBlock` 的设计意图：
        // 它变换的是玻璃这一层，而 backdrop 采样仍在原始坐标系里进行。
        // 如果改用外层 graphicsLayer 缩放，整个子树（含采样结果）会被一起
        // 拉伸——正文就跟着变了，那正是上一版的问题。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBarSurface(
                    backdrop = rootBackdrop,
                    shape = RoundedCornerShape(BarHeightDp / 2),
                    pressProgress = { damped.pressProgress },
                    // 面板在画背景的同时把「自己」录进 barBackdrop，供滑块采样
                    exportedBackdrop = barBackdrop,
                ),
        )

        // ══ L3：滑块 ══
        // 绘制顺序：面板 → 滑块 → tab 文字。
        // 滑块垫在文字下面，所以文字永远是最上层、最清晰的那一份。
        //
        // 两态（对齐 MeloX 的 L3 水珠）：
        // - **静息**：灰底色块，无模糊/折射 —— 它只是「当前在这格」的标记
        // - **按下**：灰底淡出，变成带折射的透明玻璃
        //   由于采样源是 barBackdrop（底栏自己），折射弯折的是
        //   **底栏的纸面**，不会穿透到下面的页面内容。
        Box(
            modifier = Modifier
                .offset {
                    val centerX = firstCenterPx + centerStepPx * damped.value
                    val x = (centerX - dropletWidthPx / 2f).coerceAtLeast(0f)
                    androidx.compose.ui.unit.IntOffset(x.fastRoundToInt(), 0)
                }
                .align(Alignment.CenterStart)
                // 滑块只负责「看起来」跟随，手势由上层的 tab 行统一接管，
                // 避免两处 pointerInput 抢事件。
                .glassDroplet(
                    backdrop = barBackdrop,
                    shape = RoundedCornerShape(dropletHeight / 2),
                    damped = damped,
                )
                .size(dropletWidth, dropletHeight),
        )

        // ══ 最上层：tab 行（始终清晰，同时承载手势）══
        // 点击与拖动全部由这一层的 `damped.modifier` 统一处理：
        // 位移为 0 ⇒ 视作点击，按落到第几格切换；有位移 ⇒ 拖动滑块。
        //
        // ⚠ 每个 tab 自己**不能**再挂 clickable：
        // 那样它的命中坐标是各自格子的局部坐标，而手势层用的是整行坐标，
        // 两套坐标混在一起会让「点哪个都跳不对」。
        //
        // ★ 按压缩放：图标与文字跟随面板一起放大。
        //
        // 倍率与面板的 layerBlock 完全一致（见 glassBarSurface），
        // 两边都按「向外扩张固定 10dp」算，所以内容与玻璃边缘同步位移，
        // 不会出现「玻璃变大了、图标还在原位」的错位。
        //
        // 区别在于：面板用的是 drawBackdrop 的 layerBlock（只缩玻璃层），
        // 内容用的是这里普通 graphicsLayer（缩的是内容本身）。
        // 用的坐标系不同，但视觉倍率相同。
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = rowPad)
                .graphicsLayer {
                    val p = damped.pressProgress.coerceIn(0f, 1f)
                    if (p > 0f) {
                        val maxExtra = 10.dp.toPx() * 2f
                        val w = size.width.coerceAtLeast(1f)
                        scaleX = 1f + (maxExtra / w) * p
                        scaleY = 1f + 0.03f * p
                    }
                }
                .then(damped.modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                TabContent(
                    modifier = Modifier
                        .width(tabWidth)
                        .height(BarHeightDp),
                    tab = tab,
                    selected = tab == current,
                    highlight = false,
                )
            }
        }
    }
}

/**
 * 一个 tab 的内容：图标 + 文字。
 *
 * @param highlight true 时用强调色（供 L2 捕获层采样），false 时用常规墨色
 */
@Composable
private fun TabContent(
    modifier: Modifier = Modifier,
    tab: StariaTab,
    selected: Boolean,
    highlight: Boolean,
) {
    val accent = LocalThemeColor.current.accent()
    val color = when {
        highlight -> accent
        selected -> accent
        else -> InkMuted
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = stringResource(tab.labelRes),
            modifier = Modifier.size(19.dp),
            tint = color,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = stringResource(tab.labelRes),
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            color = color,
            fontWeight = if (selected && highlight) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * 底栏面板玻璃（L1）。
 *
 * 参数对齐 MeloX 的 `meloXLiquidBottomBar`：
 * - vibrancy + blur(2dp) + lens(24dp × pressProgress)
 * - 折射半径随按压进度增强：静息时很弱，按下时才明显
 *   （MeloX 用 `lens(r×s, r×s)`，s=0 时整段跳过，避免 NaN）
 */
@Composable
private fun Modifier.glassBarSurface(
    backdrop: Backdrop?,
    shape: androidx.compose.ui.graphics.Shape,
    pressProgress: () -> Float,
    exportedBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop? = null,
): Modifier {
    // 颜色令牌是 Composable 属性，组合期取出后供下方闭包捕获
    val paperWhite = PaperWhite
    if (backdrop == null) {
        return background(paperWhite.copy(alpha = 0.94f), shape)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        // 效果栈与其余玻璃控件（新建按钮、头像、二级页圆钮）**完全同源**：
        // 都走 stariaGlassEffects，参数只在 GlassSpec 里定义一次。
        effects = { stariaGlassEffects() },
        // 面板在画背景的同时把「自己」录进 exportedBackdrop。
        // 滑块以它为采样源，于是滑块里看到的永远是底栏纸面，
        // 而不是下面的页面内容（否则就是「穿透」）。
        //
        // 这是库文档 Glass Bottom Sheet 一节的正解：
        // 直接给内层控件传同一个 backdrop 会成环（画自己被自己画），
        // exportedBackdrop 会跳过内容绘制，只录外壳。
        exportedBackdrop = exportedBackdrop,
        layerBlock = {
            // 面板随按压放大：**只变换玻璃这一层**。
            // backdrop 的采样不受这里影响，所以面板变大时，
            // 透过它看到的页面正文仍是原尺寸、原位置 —— 这正是
            // `layerBlock` 与「外层 graphicsLayer」的本质区别。
            val p = pressProgress().coerceIn(0f, 1f)
            if (p > 0f) {
                val maxExtra = 10.dp.toPx() * 2f
                val w = size.width.coerceAtLeast(1f)
                scaleX = 1f + (maxExtra / w) * p
                scaleY = 1f + 0.03f * p
            }
        },
    )
}

/**
 * 滑块（L3），两态连续过渡。
 *
 * ## 为什么是两套材质叠在一起
 *
 * 需求是「静息灰底、按下玻璃」。如果做成 `if (pressed) 玻璃 else 灰底`，
 * 切换瞬间会硬切（灰块一闪变成玻璃），违反「所有元素平滑过渡」。
 * 所以这里**同时画两套**，用 [StariaDampedDragAnimation.pressProgress] 做交叉淡化：
 *
 * - 灰底层：alpha 从 0.10 降到 0（按下时淡出）
 * - 玻璃层：透明底 + 折射，折射强度从 0 升到满（按下时淡入）
 *
 * 两者在同一个 Box 上，不需要额外容器的 alpha（那会把折射一起洗淡）。
 *
 * ## 折射必须从 0 起算
 *
 * `lens(0, 0)` 会让着色器里出现 0/0 ⇒ NaN ⇒ 内容变成黑洞。
 * 所以 pressProgress 小于阈值时**整段跳过**透镜调用，这是每帧都会命中的路径。
 */
@Composable
private fun Modifier.glassDroplet(
    backdrop: Backdrop?,
    shape: androidx.compose.ui.graphics.Shape,
    damped: StariaDampedDragAnimation,
): Modifier {
    // 颜色令牌是 Composable 属性，组合期取出后供下方闭包捕获
    val ink = Ink
    if (backdrop == null) {
        // 没有背景层（低版本 / 预览）：只保留灰底，至少能看出选中的是哪格
        return this.background(ink.copy(alpha = 0.10f), shape)
    }

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            val p = damped.pressProgress.coerceIn(0f, 1f)
            // 只在按下时才有折射；静息完全不碰透镜（lens(0,0) 会算出 NaN）
            if (p > 0.02f) {
                lens(
                    refractionHeight = 12.dp.toPx() * p,
                    refractionAmount = 16.dp.toPx() * p,
                    depthEffect = true,
                    // 关掉色散：浅色玻璃上会析出明显彩虹边，
                    // 与「白纸 + 墨线」的克制基调冲突
                    chromaticAberration = false,
                )
            }
        },
        layerBlock = {
            // 按压缩放 + 甩动变形。写在 layerBlock 里才是「只变换这一层」
            val v = damped.velocity.let { if (it.isFinite()) it else 0f } / 10f
            val widenDenom = 1f - (v * 0.75f).fastCoerceIn(-0.2f, 0.2f)
            val widenFactor = 1f - (v * 0.25f).fastCoerceIn(-0.2f, 0.2f)
            val sx = damped.scaleX.let { if (it.isFinite()) it else 1f }
            val sy = damped.scaleY.let { if (it.isFinite()) it else 1f }
            scaleX = (sx / widenDenom).let { if (it.isFinite()) it else 1f }
            scaleY = (sy * widenFactor).let { if (it.isFinite()) it else 1f }
        },
        onDrawSurface = {
            // ⚠ 这里**只画灰底**，且按下时淡出到 0。
            //
            // 早先的写法有两处叠加导致「按下后滑块内部发白」：
            // ① 灰底写在 modifier 上（`background`），它永远不消失；
            // ② 这里又画了一层白（`0.72f * (1-p)`）。
            // 两层白色叠在玻璃之上，折射自然看不见。
            //
            // 现在只有这一处负责可见性：静息 0.10 灰、按下渐隐到 0，
            // 于是按下时滑块只剩折射 —— 即「全透明的玻璃」。
            val p = damped.pressProgress.coerceIn(0f, 1f)
            val gray = 0.10f * (1f - p)
            if (gray > 0.001f) {
                drawRect(ink.copy(alpha = gray))
            }
        },
    )
}

/**
 * 多选模式下的红色删除按钮。尺寸与新建按钮一致，切换时位置不跳动。
 */
@Composable
private fun DeleteButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(CreateButtonSize)
            // pressScale 在 background 之前：实心圆本体与图标一起扩大回弹
            .pressScale()
            .background(Danger, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
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

/** 新建按钮：点击向上展开三个动作钮。 */
@Composable
private fun CreateButton(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onCreate: (CreateKind) -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 每个动作钮独立的进度：展开时错峰飞出，收起时各自回落。
    // Animatable 驱动，支持「弹簧带过冲」和逐按钮延迟。
    val kinds = CreateKind.entries
    val progresses = remember { kinds.map { Animatable(0f) } }

    // 弹簧：带过冲（dampingRatio < 1），展开到达位置时回弹
    val overshootSpec = spring<Float>(
        dampingRatio = 0.55f,
        stiffness = Spring.StiffnessMediumLow,
    )

    // 收起弹簧：临界阻尼（无过冲）。收起时若过冲，progress 会短暂变负，
    // 玻璃层向下冲过主按钮、采样到下方空白，就是那个力度越大越大的色块。
    val collapseSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    LaunchedEffect(expanded) {
        if (expanded) {
            // 展开：下方按钮最先（order 越小越靠下、越先飞），时差 0.05s
            kinds.forEachIndexed { index, _ ->
                val order = kinds.size - 1 - index
                launch {
                    delay(order * 50L)
                    progresses[index].animateTo(1f, overshootSpec)
                }
            }
        } else {
            // 收起：动作钮错峰回落，无过冲（主按钮不做额外动画）
            kinds.forEachIndexed { index, _ ->
                val order = kinds.size - 1 - index
                launch {
                    delay(order * 50L)
                    progresses[index].animateTo(0f, collapseSpec)
                }
            }
        }
    }

    val actionSize = 46.dp
    val gap = 12.dp

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        kinds.forEachIndexed { index, kind ->
            val order = kinds.size - 1 - index
            val distance = CreateButtonSize / 2 + actionSize / 2 + gap +
                (actionSize + gap) * order

            val progress = progresses[index].value
            val distancePx = with(LocalDensity.current) { distance.toPx() }

            InteractiveGlassPanel(
                backdrop = LocalGlassBackdrop.current,
                shape = CircleShape,
                pressInflate = 3.dp,
                enabled = expanded,
                onClick = {
                    onCreate(kind)
                    onExpandedChange(false)
                },
                // 缩放放进玻璃层（layerBlock）：缩放以中心为原点，
                // backdrop 采样随缩放一起，不会露出未采样区域。
                layerScale = { 0.55f + 0.45f * progress },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // 平移用 layout offset（不是 layerBlock translation）：
                    // layout 偏移会让 drawBackdrop 的采样坐标跟随玻璃一起移动，
                    // 玻璃移到哪、采样就跟着采到哪，不会出现白色色块。
                    .offset { IntOffset(0, (-distancePx * progress).roundToInt()) }
                    .size(actionSize),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = kind.icon,
                        contentDescription = stringResource(kind.labelRes),
                        modifier = Modifier
                            .size(19.dp)
                            .graphicsLayer { alpha = progress },
                        tint = Ink,
                    )
                }
            }
        }

        // 主按钮加号 → 叉号的旋转，随展开/收起平滑过渡（带过冲，与动作钮同节奏）
        val plusRotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            animationSpec = spring(
                dampingRatio = 0.55f,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "create-plus-rotation",
        )

        InteractiveGlassPanel(
            backdrop = LocalGlassBackdrop.current,
            shape = CircleShape,
            pressInflate = 4.dp,
            onClick = { onExpandedChange(!expanded) },
            modifier = Modifier.size(CreateButtonSize),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = IconPlus,
                    contentDescription = stringResource(
                        if (expanded) R.string.cd_collapse else R.string.cd_create,
                    ),
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = plusRotation },
                    tint = Ink,
                )
            }
        }
    }
}
