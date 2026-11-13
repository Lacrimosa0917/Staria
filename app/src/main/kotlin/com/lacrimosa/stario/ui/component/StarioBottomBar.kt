package com.lacrimosa.stario.ui.component

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
import com.lacrimosa.stario.R
import com.lacrimosa.stario.ui.icon.IconDiary
import com.lacrimosa.stario.ui.icon.IconEssay
import com.lacrimosa.stario.ui.icon.IconHome
import com.lacrimosa.stario.ui.icon.IconHourglass
import com.lacrimosa.stario.ui.icon.IconPaperStack
import com.lacrimosa.stario.ui.icon.IconPlus
import com.lacrimosa.stario.ui.icon.IconQuill
import com.lacrimosa.stario.ui.icon.IconQuote
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted
import com.lacrimosa.stario.ui.theme.PaperWhite

/** 底栏四个页面。 */
enum class StarioTab(val labelRes: Int, val icon: ImageVector) {
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

private val CapsuleHeight = 58.dp
private val CapsuleHPad = 5.dp
private val CreateButtonSize = 54.dp
private val BarGap = 12.dp

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
fun StarioBottomBar(
    current: StarioTab,
    onSelect: (StarioTab) -> Unit,
    onCreate: (CreateKind) -> Unit,
    navExpanded: Boolean,
    onNavExpandedChange: (Boolean) -> Unit,
    createExpanded: Boolean,
    onCreateExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val tabCount = StarioTab.entries.size
        // 展开态：吃掉除新建按钮与间距外的全部宽度
        val expandedWidth = maxWidth - CreateButtonSize - BarGap
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

        CreateButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            expanded = createExpanded,
            onExpandedChange = onCreateExpandedChange,
            onCreate = onCreate,
        )
    }
}

@Composable
private fun NavCapsule(
    modifier: Modifier = Modifier,
    current: StarioTab,
    expanded: Boolean,
    expandedWidth: Dp,
    collapsedWidth: Dp,
    cellWidth: Dp,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (StarioTab) -> Unit,
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
            StarioTab.entries.forEach { tab ->
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
    tab: StarioTab,
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
