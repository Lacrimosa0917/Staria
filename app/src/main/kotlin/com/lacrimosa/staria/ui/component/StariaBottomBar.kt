package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.R
import com.lacrimosa.staria.ui.icon.IconDiary
import com.lacrimosa.staria.ui.icon.IconEssay
import com.lacrimosa.staria.ui.icon.IconHome
import com.lacrimosa.staria.ui.icon.IconHourglass
import com.lacrimosa.staria.ui.icon.IconPaperStack
import com.lacrimosa.staria.ui.icon.IconPlus
import com.lacrimosa.staria.ui.icon.IconQuill
import com.lacrimosa.staria.ui.icon.IconQuote
import com.lacrimosa.staria.ui.icon.IconTrash
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.PaperWhite

/** 底栏四个页面。 */
enum class StariaTab(val labelRes: Int, val icon: ImageVector) {
    Home(R.string.tab_home, IconHome),
    Time(R.string.tab_time, IconHourglass),
    Paper(R.string.tab_paper, IconPaperStack),
    Think(R.string.tab_think, IconQuill),
}

/** 新建菜单三项。 */
enum class CreateKind(val labelRes: Int, val icon: ImageVector) {
    Diary(R.string.create_diary, IconDiary),
    Excerpt(R.string.create_excerpt, IconQuote),
    Essay(R.string.create_essay, IconEssay),
}

private val Hairline = Color(0xFFE6E2DA)

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
 * 可滚动列表末尾的留白。
 *
 * 上下栏是悬浮层，内容滚动时会从它们下方穿过（这是想要的效果）；
 * 但滚到底部时最后一项必须停在底栏上方，否则会被永久盖住。
 * 所以只在**列表末尾**加这段留白，而不是给整个内容区加 padding。
 */
val ScrollEndInset: Dp = CapsuleHeight + 18.dp + 8.dp

/**
 * 可滚动列表开头的留白。
 *
 * 与 [ScrollEndInset] 对称：滚到最上方时第一项停在悬浮头像下方，
 * 中途滚动时仍可从头像下方穿过。取值 = 头像直径 + 上下间距。
 */
val ScrollStartInset: Dp = 44.dp + 14.dp + 8.dp

/**
 * 底栏第一个页面按键的圆心相对屏幕左缘的横向偏移。
 *
 * 时间页的竖向时间轴要对齐到这里（README：时间轴对齐左下角页面按键的中心）。
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
 *
 * 由 BarSidePadding + 胶囊内边距组成。注意圆点自身有半径，
 * 页面里给圆点定位时要再减去半径才能让「圆点边缘」贴住按键左缘。
 */
fun navFirstItemLeftX(): Dp = BarSidePadding + CapsuleHPad

/**
 * 底部：页面底栏（长条胶囊）+ 新建按钮（圆形，收在胶囊右侧）。
 *
 * 交互（README）：
 * - 胶囊收起时只显示当前页面；点击展开露出四项，点其它项切换并收起
 * - 新建按钮点击后向上展开，上下排列三个圆形按钮
 *
 * 布局约定：
 * - 展开时胶囊占满「总宽 - 新建按钮 - 间距」，两者基本占满屏宽
 * - 收起态宽度**正好等于展开态的一个格子**，因此第一个按键的中心在
 *   展开/收起之间完全不动（不会出现首页按键左右跳动）
 * - 不绘制阴影，只保留 1dp 描边（后续会替换成新 UI）
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
    /**
     * 多选模式下的删除按钮。
     *
     * 传非空值时，右下角用红色删除按钮**顶替**新建按钮（同一个位置、
     * 同一个尺寸），而不是在页面里另画一个。这样两种状态的按钮不会错位。
     */
    deleteAction: (() -> Unit)? = null,
) {
    BoxWithConstraints(modifier = modifier) {
        val tabCount = StariaTab.entries.size
        val hasFloating = deleteAction != null || true
        // 展开态：吃掉除右下角圆钮与间距外的全部宽度。
        // 圆钮无论是新建还是删除都占同样位置，所以宽度始终扣掉它。
        val reserved = CreateButtonSize + BarGap
        val expandedWidth = maxWidth - reserved
        val cellWidth = (expandedWidth - CapsuleHPad * 2) / tabCount.toFloat()
        // 收起态：恰好等于一个格子，保证首个按键位置不变
        val collapsedWidth = cellWidth + CapsuleHPad * 2

        NavCapsule(
            modifier = Modifier.align(Alignment.BottomStart),
            current = current,
            expanded = navExpanded,
            expandedWidth = expandedWidth,
            collapsedWidth = collapsedWidth,
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
            .background(DestructiveRed, CircleShape)
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

@Composable
private fun NavCapsule(
    modifier: Modifier = Modifier,
    current: StariaTab,
    expanded: Boolean,
    expandedWidth: Dp,
    collapsedWidth: Dp,
    cellWidth: Dp,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (StariaTab) -> Unit,
) {
    // 显式圆角：percent 形状在裁剪/描边时会出现直角残留
    val shape = RoundedCornerShape(CapsuleHeight / 2)

    // 只对宽度做动画。不用 animateContentSize，因为它会把内容裁到当前尺寸。
    val width by animateDpAsState(
        targetValue = if (expanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "capsule-width",
    )

    Row(
        modifier = modifier
            .width(width)
            .height(CapsuleHeight)
            .background(PaperWhite, shape)
            .border(BorderStroke(1.dp, Hairline), shape)
            .clip(shape)
            .padding(horizontal = CapsuleHPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (expanded) {
            StariaTab.entries.forEach { tab ->
                NavItem(
                    modifier = Modifier.width(cellWidth).height(CapsuleHeight),
                    tab = tab,
                    selected = tab == current,
                    onClick = {
                        onSelect(tab)
                        onExpandedChange(false)
                    },
                )
            }
        } else {
            // 与展开态第一格完全同位同宽
            NavItem(
                modifier = Modifier.width(cellWidth).height(CapsuleHeight),
                tab = current,
                selected = true,
                onClick = { onExpandedChange(true) },
            )
        }
    }
}

@Composable
private fun NavItem(
    modifier: Modifier = Modifier,
    tab: StariaTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
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
        // 三个操作按钮：主按钮正上方，上下排列；Diary 在最上
        val kinds = CreateKind.entries
        kinds.forEachIndexed { index, kind ->
            val order = kinds.size - 1 - index // Diary = 2, Excerpt = 1, Essay = 0
            val distance = CreateButtonSize / 2 + actionSize / 2 + gap +
                (actionSize + gap) * order

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .graphicsLayer {
                        translationY = -distance.toPx() * progress
                        alpha = progress
                        val s = 0.55f + 0.45f * progress
                        scaleX = s
                        scaleY = s
                    }
                    .size(actionSize)
                    .background(CardPaper, CircleShape)
                    .border(BorderStroke(1.dp, Hairline), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = expanded,
                    ) {
                        onCreate(kind)
                        onExpandedChange(false)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = stringResource(kind.labelRes),
                    modifier = Modifier.size(19.dp),
                    tint = Ink,
                )
            }
        }

        // 主按钮：加号随展开旋转 45° 变成叉
        Box(
            modifier = Modifier
                .size(CreateButtonSize)
                .background(PaperWhite, CircleShape)
                .border(BorderStroke(1.dp, Hairline), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onExpandedChange(!expanded) },
            contentAlignment = Alignment.Center,
        ) {
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

/** 展开时铺一层透明捕获层，点空白处收起。 */
@Composable
fun OutsideClickCatcher(
    visible: Boolean,
    onClick: () -> Unit,
) {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    )
}
