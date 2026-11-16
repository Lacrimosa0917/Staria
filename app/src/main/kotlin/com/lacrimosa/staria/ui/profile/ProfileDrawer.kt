package com.lacrimosa.staria.ui.profile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.RecordCounts
import com.lacrimosa.staria.ui.icon.IconChevronRight
import com.lacrimosa.staria.ui.icon.IconSettings
import com.lacrimosa.staria.ui.icon.IconStarDust
import androidx.compose.material3.Icon
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.PanelRadius
import com.lacrimosa.staria.ui.theme.PaperWhite
import androidx.compose.ui.graphics.vector.ImageVector

/** 抽屉占屏宽比例（README：占屏幕左侧四分之三）。 */
private const val DRAWER_FRACTION = 0.75f

/**
 * 个人主页。
 *
 * 交互（README）：
 * - 唤起时从左侧滑出，占屏幕左侧 3/4
 * - 右侧 1/4 是压暗后的首页，点击收起
 * - 左上角昵称，点击弹窗修改
 * - 昵称下方是三行记录数量（标题用品牌宋体，数量用系统字体）
 * - 再下方两行：星尘（回收站）、设置
 */
@Composable
fun ProfileDrawer(
    visible: Boolean,
    nickname: String,
    counts: RecordCounts,
    onDismiss: () -> Unit,
    onEditNickname: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 0 = 完全收起，1 = 完全展开。遮罩与抽屉共用它：
    // - 遮罩 alpha = 0.28 * progress，只淡入淡出、不位移（原地出现）
    // - 抽屉 translationX 由 progress 驱动，从左侧滑入 / 向左滑出
    val progress = remember { Animatable(0f) }

    // 退出动画播完后才真正移除。visible 变 false 时不能立即 return，
    // 否则看不到滑出动画；也不能一直 return（那会挡住下层点击）。
    var actuallyGone by remember { mutableStateOf(!visible) }

    LaunchedEffect(visible) {
        if (visible) {
            actuallyGone = false
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                // 不能用 LowBouncy：过冲时 translationX 会变成正值，
                // 抽屉右侧越过屏幕左缘，左侧就会露出下一层页面。
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        } else {
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260),
            )
            actuallyGone = true
        }
    }

    if (actuallyGone) return

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 右侧 1/4：压暗的当前页面，点击收起 ----
        // 遮罩原地淡入淡出，不参与抽屉的水平位移。
        // 深度统一为 Scrim（与纸条页卡片展开、日历展开一致）。
        val scrimColor = Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor.copy(alpha = scrimColor.alpha * progress.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        // ---- 左侧 3/4：抽屉本体 ----
        // 外层用 clipToBounds 兜住，即使动画中途有微小过冲也不会漏出下层页面
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(DRAWER_FRACTION)
                .clipToBounds(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 顺序很重要：先加 padding 再 graphicsLayer，
                    // 否则 size.width 是内缩后的宽度，translationX 会算小，
                    // 导致动画结束后仍有偏移、点击坐标与视觉位置错位。
                    .systemBarsPadding()
                    .graphicsLayer {
                        translationX = (-size.width * (1f - progress.value))
                            .coerceAtMost(0f)
                            .coerceAtLeast(-size.width)
                    }
                    .background(PageBackground),
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(28.dp))

                    NicknameRow(
                        nickname = nickname,
                        onEdit = onEditNickname,
                    )

                    Spacer(Modifier.height(28.dp))

                    CountsCard(counts = counts)

                    Spacer(Modifier.height(20.dp))

                    EntryRow(
                        label = "星尘",
                        icon = IconStarDust,
                        onClick = onOpenRecycleBin,
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp),
                        thickness = 1.dp,
                        color = Border,
                    )
                    EntryRow(
                        label = "设置",
                        icon = IconSettings,
                        onClick = onOpenSettings,
                    )
                }
            }
        }
    }
}

@Composable
private fun NicknameRow(
    nickname: String,
    onEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEdit,
            )
            .padding(horizontal = 28.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = nickname,
            color = Ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.size(6.dp))
        // 铅笔提示可编辑
        Icon(
            imageVector = com.lacrimosa.staria.ui.icon.IconPencilSmall,
            contentDescription = "修改昵称",
            modifier = Modifier.size(16.dp),
            tint = InkMuted,
        )
    }
}

/**
 * 记录数量卡片：三行，标题品牌宋体、数量系统字体。
 * 格式见 README：时间：- 条 / 纸条：- 张 / 思索：- 篇
 */
@Composable
private fun CountsCard(counts: RecordCounts) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            // 纯白卡面，不描边：与抽屉 #F5F5F5 底色的层次只靠色差
            .background(PaperWhite, RoundedCornerShape(PanelRadius))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CountRow(label = "时间", value = counts.diary, unit = "条")
        CountRow(label = "纸条", value = counts.paper, unit = "张")
        CountRow(label = "思索", value = counts.essay, unit = "篇")
    }
}

@Composable
private fun CountRow(label: String, value: Int, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            fontFamily = KebenSong,
            fontSize = 17.sp,
            color = Ink,
        )
        Text(
            text = "：",
            fontFamily = KebenSong,
            fontSize = 17.sp,
            color = Ink,
        )
        Text(
            text = value.toString(),
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = Ink,
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = unit,
            fontSize = 14.sp,
            color = InkMuted,
        )
    }
}

/**
 * 一行入口：图标 + 文字 + 右箭头。
 *
 * ICON.md「个人主页」：图标显示在选项文字左侧。
 */
@Composable
private fun EntryRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 28.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Ink,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Ink,
            fontSize = 16.sp,
        )
        Icon(
            imageVector = IconChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = InkMuted,
        )
    }
}
