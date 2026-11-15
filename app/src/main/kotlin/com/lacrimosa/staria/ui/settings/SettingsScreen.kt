package com.lacrimosa.staria.ui.settings

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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.DarkMode
import com.lacrimosa.staria.data.ThemeColor
import com.lacrimosa.staria.ui.component.ActionTooltip
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.TooltipAction
import com.lacrimosa.staria.ui.icon.IconCheck
import com.lacrimosa.staria.ui.icon.IconBack
import com.lacrimosa.staria.ui.icon.IconChevronRight
import com.lacrimosa.staria.ui.icon.IconDotPlaceholder
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.PanelRadius
import com.lacrimosa.staria.ui.theme.PaperWhite
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
    var darkAnchor by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize().background(PageBackground)) {
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
                    .background(CardPaper, RoundedCornerShape(PanelRadius))
                    .border(1.dp, Border, RoundedCornerShape(PanelRadius))
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BarSidePadding)
                    .background(CardPaper, RoundedCornerShape(PanelRadius))
                    .border(1.dp, Border, RoundedCornerShape(PanelRadius))
                    .onGloballyPositioned { darkAnchor = it.positionInWindow() }
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
                Text(
                    text = darkMode.label,
                    fontSize = 13.sp,
                    color = InkMuted,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    // 用右尖括号旋转 90° 表示「向下展开」，避免再画一个图标
                    imageVector = IconChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(90f),
                    tint = InkMuted,
                )
            }

            // 底部留白
            Spacer(Modifier.height(40.dp))
        }

        // ---------- 顶栏 ----------
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = SecondaryTopBarPad),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ButtonSize)
                    .background(PaperWhite, CircleShape)
                    .border(1.dp, Border, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = IconBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(17.dp),
                    tint = Ink,
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "设置",
                fontFamily = KebenSong,
                fontSize = 19.sp,
                color = Ink,
            )
        }
    }

    // ---------- 深色模式 tooltip（向下展开） ----------
    if (showDarkTooltip) {
        ActionTooltip(
            anchor = darkAnchor,
            actions = DarkMode.entries.map { mode ->
                TooltipAction(
                    label = mode.label,
                    // 已选中的那项用勾图标标记，其余用同尺寸占位图标保持左对齐
                    icon = if (mode == darkMode) IconCheck else IconDotPlaceholder,
                    onClick = {
                        showDarkTooltip = false
                        onPickDarkMode(mode)
                    },
                )
            },
            onDismiss = { showDarkTooltip = false },
        )
    }
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
