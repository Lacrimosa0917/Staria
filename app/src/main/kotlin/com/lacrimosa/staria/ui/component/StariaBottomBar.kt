package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.stariaGlassEffects
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        if (deleteAction != null) {
            DeleteButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = deleteAction,
            )
        } else {
            CreateButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                expanded = createExpanded,
                onExpandedChange = onCreateExpandedChange,
                onCreate = onCreate,
            )
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

    // ---- L2 捕获层用的独立 backdrop ----
    // 之前这里录的是「tab 内容副本」，滑块采样出来就是一份模糊的图标 + 文字，
    // 再叠在清晰的 tab 行上 ⇒ 文字看着发虚（重影）。
    // 现在滑块直接采页面背景：它是纯玻璃块，里面不含任何文字，
    // 层级也挪到 tab 行之下，文字始终由上层清晰绘制。
    val rootBackdrop = LocalGlassBackdrop.current

    Box(
        modifier = modifier
            .width(barWidth)
            .height(BarHeightDp),
    ) {
        // ══ L1：面板玻璃 ══
        // 按下时整条面板轻微膨胀（scale = 1 + 14dp/宽度 × p），
        // 与 MeloX 的 `layerBlock` 做法一致 —— 手指压上去时底栏像被按了一下。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassBarSurface(
                    backdrop = rootBackdrop,
                    shape = RoundedCornerShape(BarHeightDp / 2),
                    pressProgress = { damped.pressProgress },
                ),
        )

        // ══ L3：滑块 ══
        // 绘制顺序：面板 → 滑块 → tab 文字。
        // 滑块垫在文字下面，所以文字永远是最上层、最清晰的那一份。
        //
        // 两态（对齐 MeloX 的 L3 水珠）：
        // - **静息**：灰底色块，无模糊/折射 —— 它只是「当前在这格」的标记
        // - **按压缩放**：变成全透明带折射的玻璃，能看见下方面板内容被弯折
        //   切换由 `glassDroplet` 内部按 pressProgress 连续插值，不是硬切
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
                    backdrop = rootBackdrop,
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
        // ⚠ 行本身**不能**跟着面板一起缩放：
        // 面板按下时膨胀，若这一行也在同一个缩放层里，图标和文字会一起变大。
        // 面板的 scale 写在它自己的 layerBlock 里（只变换玻璃那一层），
        // 所以这一行天然不受影响 —— 这里只做位置对齐。
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = rowPad)
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
    val color = when {
        highlight -> Ink
        selected -> Ink
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
private fun Modifier.glassBarSurface(
    backdrop: Backdrop?,
    shape: androidx.compose.ui.graphics.Shape,
    pressProgress: () -> Float,
): Modifier {
    if (backdrop == null) {
        return background(PaperWhite.copy(alpha = 0.94f), shape)
    }
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        // 效果栈与其余玻璃控件（新建按钮、头像、二级页圆钮）**完全同源**：
        // 都走 stariaGlassEffects，参数只在 GlassSpec 里定义一次。
        // 之前底栏自己写了一套 blur(2dp) + lens(24dp)，与右侧新建按钮
        // 的 6dp/14dp 不一致，并排看就是两种材质。
        effects = { stariaGlassEffects() },
        layerBlock = {
            // 按下时整条面板膨胀：向外扩散固定的 14dp（不是按比例），
            // 与 MeloX 的 `1 + 16dp/width × p` 同思路 —— 这样面板「被按了一下」
            // 的视觉强度与宽度无关，宽屏窄屏手感一致。
            //
            // 必须写在 layerBlock：它只变换玻璃这一层，backdrop 采样仍是原始尺寸，
            // 否则膨胀时背景会跟着放大（看起来像画面在抖）。
            // 内部的 tab 行不在这个变换里，所以图标与文字不会跟着变大。
            val p = pressProgress().coerceIn(0f, 1f)
            if (p > 0f) {
                val maxExtra = 14.dp.toPx() * 2f
                val w = size.width.coerceAtLeast(1f)
                scaleX = 1f + (maxExtra / w) * p
                scaleY = 1f + 0.035f * p
            }
        },
        // 不额外叠蒙层：与 GlassPanel 一致（它现在也不画 surface，
        // 玻璃的可见性由 blur + lens + 细描边共同给出）。
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
private fun Modifier.glassDroplet(
    backdrop: Backdrop?,
    shape: androidx.compose.ui.graphics.Shape,
    damped: StariaDampedDragAnimation,
): Modifier {
    if (backdrop == null) {
        // 没有背景层（低版本 / 预览）：只保留灰底，至少能看出选中的是哪格
        return this.background(Ink.copy(alpha = 0.10f), shape)
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
                drawRect(Ink.copy(alpha = gray))
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
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "create-progress",
    )

    val actionSize = 46.dp
    val gap = 12.dp

    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        val kinds = CreateKind.entries
        kinds.forEachIndexed { index, kind ->
            val order = kinds.size - 1 - index
            val distance = CreateButtonSize / 2 + actionSize / 2 + gap +
                (actionSize + gap) * order

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        translationY = -distance.toPx() * progress
                        val s = 0.55f + 0.45f * progress
                        scaleX = s
                        scaleY = s
                    }
                    .size(actionSize),
            ) {
                InteractiveGlassPanel(
                    backdrop = LocalGlassBackdrop.current,
                    shape = CircleShape,
                    pressInflate = 3.dp,
                    enabled = expanded,
                    onClick = {
                        onCreate(kind)
                        onExpandedChange(false)
                    },
                    modifier = Modifier.fillMaxSize(),
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
        }

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
                        .graphicsLayer { rotationZ = 45f * progress },
                    tint = Ink,
                )
            }
        }
    }
}
