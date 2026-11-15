package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 底栏四个页面。
 *
 * [icon] 是未选中时的线性图标，[selectedIcon] 是选中时的填充图标
 * （ICON.md：选中的页面图标使用填充，未选中的使用线性）。
 */
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

/** 底栏高度。新建按钮直径与之相等，两者底边对齐后高度完全一致。 */
private val CapsuleHeight = 58.dp
private val CapsuleHPad = 5.dp
private val CreateButtonSize = CapsuleHeight
private val BarGap = 12.dp

/** 底栏左右留白。页面里需要对齐底栏元素时引用这个常量，避免两处硬编码走样。 */
val BarSidePadding = 20.dp

/** 底栏总高度。 */
val BarHeight: Dp = CapsuleHeight

/**
 * 底栏第一个页面按键的圆心相对屏幕左缘的横向偏移。
 *
 * 时间页的竖向时间轴要对齐到这里（README：时间轴对齐左下角页面按键的中心）。
 *
 * 注意：收起态已改为正圆按钮，此时「第一个按键」指的是展开态的第一格，
 * 时间轴仍然对齐这个位置 —— 收起/展开切换时轴线不会跳。
 */
fun navFirstItemCenterX(screenWidth: Dp): Dp {
    val expandedWidth = screenWidth - CreateButtonSize - BarGap
    val cellWidth = (expandedWidth - CapsuleHPad * 2) / StariaTab.entries.size.toFloat()
    return BarSidePadding + CapsuleHPad + cellWidth / 2
}

/**
 * 底栏第一个页面按键的**左边缘**相对屏幕左缘的横向偏移。
 *
 * 时间页的时间轴竖线对齐到这里（需求：竖线对准页面按键左侧）。
 */
fun navFirstItemLeftX(): Dp = BarSidePadding + CapsuleHPad

/**
 * 底部：页面底栏（液态玻璃）+ 新建按钮（圆形，收在右侧）。
 *
 * 交互：
 * - 收起态是一个**正圆按钮**，只显示当前页面的图标与文字
 * - 点击展开为长条胶囊，四项依次淡入
 * - 切页后底栏保持展开（切页不收起）
 * - 每个按键按下时轻微缩放，给出手感反馈
 */
@Composable
fun StariaBottomBar(
    current: StariaTab,
    onSelect: (StariaTab) -> Unit,
    onCreate: (CreateKind) -> Unit,
    navExpanded: Boolean,
    onNavExpandedChange: (Boolean) -> Unit,
    createExpanded: Boolean,
    onCreateExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    deleteAction: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val tabCount = StariaTab.entries.size
        val reserved = CreateButtonSize + BarGap
        val expandedWidth = maxWidth - reserved
        val cellWidth = (expandedWidth - CapsuleHPad * 2) / tabCount.toFloat()

        NavCapsule(
            modifier = Modifier.align(Alignment.BottomStart),
            current = current,
            expanded = navExpanded,
            expandedWidth = expandedWidth,
            cellWidth = cellWidth,
            onExpandedChange = onNavExpandedChange,
            onSelect = onSelect,
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
 * 多选模式下的红色删除按钮。
 *
 * 尺寸与新建按钮完全一致，保证两种状态切换时位置不跳动。
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

/**
 * 页面底栏本体。
 *
 * 形态在「正圆（收起）」与「长胶囊（展开）」之间连续过渡：
 * - 宽度与圆角一起插值，视觉上就是一个胶囊被拉长 / 缩回圆
 * - 每个按键的透明度跟随展开进度淡入淡出，不会突然出现或消失
 * - 收起态下当前按键横向居中；展开过程中它平滑滑到第一格
 */
@Composable
private fun NavCapsule(
    modifier: Modifier = Modifier,
    current: StariaTab,
    expanded: Boolean,
    expandedWidth: Dp,
    cellWidth: Dp,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (StariaTab) -> Unit,
) {
    // 0 = 完全收起（正圆），1 = 完全展开（长胶囊）
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "nav-progress",
    )

    // 收起态直径 = 底栏高度，正好是一个正圆
    val collapsedDiameter = CapsuleHeight
    val width = androidx.compose.ui.unit.lerp(collapsedDiameter, expandedWidth, progress)

    // 圆角：收起时是圆（= 宽度/2），展开时是胶囊圆角（= 高度/2）。
    // 两者相等，所以这里统一用高度的一半即可平滑过渡。
    val shape = RoundedCornerShape(CapsuleHeight / 2)

    GlassPanel(
        backdrop = LocalGlassBackdrop.current,
        shape = shape,
        // 折射高度不能超过最小圆角半径；圆角 = 高度/2 = 29dp，
        // 按 0.6 收一下，实际折射带落在 8.4dp，既有厚边又不破形。
        refractionScale = 0.6f,
        edgeHighlight = 1.dp,
        modifier = modifier
            .width(width)
            .height(CapsuleHeight),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ---------- 展开态：四项固定在各自槽位 ----------
            // Row 用固定的 cellWidth + 固定的左内边距，宽度不随胶囊变化，
            // 所以胶囊拉长/缩回时**格子位置完全不动**，只有透明度在变。
            // 这正是「图标不位移、只原位淡入淡出」的要求。
            Row(
                modifier = Modifier
                    .height(CapsuleHeight)
                    .padding(horizontal = CapsuleHPad),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StariaTab.entries.forEach { tab ->
                    NavItem(
                        modifier = Modifier
                            .width(cellWidth)
                            .height(CapsuleHeight),
                        tab = tab,
                        selected = tab == current,
                        alpha = progress,
                        // 收起态整行都不可点：既避免点到看不见的格子，
                        // 也避免误切到别的页面（点击由下面的圆形层接管）
                        enabled = expanded,
                        onClick = { onSelect(tab) },
                    )
                }
            }

            // ---------- 收起态：当前页图标居中 ----------
            // 单独一份，居中渲染，随展开进度淡出。
            // 不放进上面的 Row 里，是因为 Row 的槽位固定在左，
            // 而圆形按钮的视觉中心在直径的中点 —— 两者差半个格子宽。
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                NavItemContent(
                    tab = current,
                    selected = true,
                    alpha = 1f - progress,
                )
            }

            // ---------- 点击层 ----------
            // 收起时铺满整个圆（58×58），点击展开。
            // 必须单独一层：上面 Row 的内容会被 clip 裁掉、且已 disabled，
            // 而这一层的命中区域就是圆本身，不会出现「点了没反应」。
            if (!expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickableNoIndication { onExpandedChange(true) },
                )
            }
        }
    }
}

/**
 * 一个底栏按键：图标 + 文字，按下时轻微缩放。
 *
 * @param alpha 透明度。展开/收起靠它做原位淡入淡出
 * @param enabled 透明但占位的项传 false，避免手指点到看不见的按钮
 */
@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    tab: StariaTab,
    selected: Boolean,
    alpha: Float,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val press = remember { Animatable(0f) }
    val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 300f)

    Column(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                // 按下时缩到 0.92，松手弹回。不用 ripple：
                // 纸面 + 墨线的基调下，Material 的水波会糊成一片灰。
                val s = 1f - 0.08f * press.value
                scaleX = s
                scaleY = s
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { press.animateTo(1f, pressSpec) }
                    waitForUpOrCancellation()
                    scope.launch { press.animateTo(0f, pressSpec) }
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        NavItemContent(tab = tab, selected = selected)
    }
}

/** 按键的纯视觉部分（图标 + 文字），点击与动画由外层负责。 */
@Composable
private fun NavItemContent(
    tab: StariaTab,
    selected: Boolean,
    alpha: Float = 1f,
) {
    Column(
        modifier = if (alpha >= 1f) Modifier else Modifier.graphicsLayer { this.alpha = alpha },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = stringResource(tab.labelRes),
            modifier = Modifier.size(20.dp),
            tint = if (selected) Ink else InkMuted,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(tab.labelRes),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            color = if (selected) Ink else InkMuted,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

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
                            // 图标单独淡入：面板本身始终是玻璃，不能被 alpha 洗淡
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
