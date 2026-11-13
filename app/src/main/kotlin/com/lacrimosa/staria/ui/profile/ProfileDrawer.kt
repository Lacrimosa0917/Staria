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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.RecordCounts
import com.lacrimosa.staria.ui.icon.IconChevronRight
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.PaperWhite
import androidx.compose.material3.Icon

private val Hairline = Color(0xFFE6E2DA)

/** 抽屉占屏宽比例（README：占屏幕左侧四分之三）。 */
private const val DRAWER_FRACTION = 0.75f

/**
 * 个人主页。
 *
 * 交互（README）：
 * - 唤起时从左侧滑出，占屏幕左侧 3/4
 * - 右侧 1/4 是压暗后的首页，点击收起
 * - 左上角昵称，点击弹窗修改
 * - 昵称下方是三行记录数量（标题用刻本宋体，数量用系统字体）
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
    if (!visible) return

    // 0 = 完全收起，1 = 完全展开
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            // 不能用 LowBouncy：过冲时 translationX 会变成正值，
            // 抽屉右侧越过屏幕左缘，左侧就会露出下一层页面。
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // ---- 右侧 1/4：压暗的当前页面，点击收起 ----
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.28f * progress.value))
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
                    .background(PaperWhite),
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

                    EntryRow(label = "星尘", onClick = onOpenRecycleBin)
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp),
                        thickness = 1.dp,
                        color = Hairline,
                    )
                    EntryRow(label = "设置", onClick = onOpenSettings)
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
 * 记录数量卡片：三行，标题刻本宋体、数量系统字体。
 * 格式见 README：时间：- 条 / 纸条：- 张 / 思索：- 篇
 */
@Composable
private fun CountsCard(counts: RecordCounts) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(CardPaper, RoundedCornerShape(16.dp))
            .border(1.dp, Hairline, RoundedCornerShape(16.dp))
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

@Composable
private fun EntryRow(
    label: String,
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
