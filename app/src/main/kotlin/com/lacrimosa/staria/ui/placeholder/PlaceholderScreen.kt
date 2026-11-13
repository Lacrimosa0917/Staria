package com.lacrimosa.staria.ui.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.ui.component.StariaTab
import com.lacrimosa.staria.ui.theme.InkMuted

/**
 * 空白占位页。
 *
 * 时间 / 纸条 / 思索三个页面暂未设计，这里只保证底栏切换可用。
 * 内容层铺满全屏、上下栏悬浮，这里的居中标识仍按整屏居中即可。
 */
@Composable
fun PlaceholderScreen(
    tab: StariaTab,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // 极淡的提示文字，仅用于确认当前停在哪一页
        Text(
            text = tab.name.lowercase(),
            color = InkMuted.copy(alpha = 0.45f),
            fontSize = 12.sp,
        )
    }
}
