package com.lacrimosa.staria.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.R
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.LocalContentInsets
import com.lacrimosa.staria.ui.component.PaperCardStack
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.accent

/** slogan 与卡片堆之间的固定间距。 */
private val SloganToStack = 34.dp

/**
 * 首页：极简留白布局。
 *
 * 纵向两段：
 * 1. slogan——刻本宋体大字号「星笺」
 * 2. 卡片堆叠——纸条摘录
 *
 * ## 为什么用两个 weight 夹住
 *
 * 早先 slogan 靠 `maxHeight * 0.12f` 定位、卡片靠 `weight(1f)` 撑满剩余高度，
 * 两者各算各的，真机上中间那道缝被撑到 200dp 以上，整组内容还偏上。
 *
 * 现在改成「上下各一个等权重的空白」把整组夹在中间：
 * 组内间距是常量 [SloganToStack]，组外留白由系统自动均分。
 * 可用区域用 [LocalContentInsets] 避开头像与底栏，仍是运行时值，
 * 所以不同分辨率下都不会被悬浮控件压到。
 *
 * 左上角头像与底部胶囊底栏由根布局 [com.lacrimosa.staria.ui.StariaApp] 提供：
 * 四个查看页都要保留头像与底栏，放在这里会重复实现。
 */
@Composable
fun HomeScreen(
    notes: List<PaperNote> = SampleData.notes,
    modifier: Modifier = Modifier,
) {
    val insets = LocalContentInsets.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(top = insets.top, bottom = insets.bottom),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 上方余量
            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.slogan),
                fontFamily = KebenSong,
                fontSize = 46.sp,
                lineHeight = 60.sp,
                color = LocalThemeColor.current.accent(),
                textAlign = TextAlign.Center,
                letterSpacing = 10.sp,
            )

            Spacer(Modifier.height(SloganToStack))

            // 卡片堆自身是顶对齐的定高盒子（高度 = 前排卡高 + 纸堆错开量），
            // 所以这里不抢剩余高度，由上下两个 weight 决定它落在哪。
            // 传入完整纸条列表：PaperCardStack 内部维护最多 5 张的展示队列，
            // 每次换卡从全部纸条中随机抽一张补到队尾，覆盖所有纸条。
            PaperCardStack(
                notes = notes,
                modifier = Modifier.fillMaxWidth(),
            )

            // 下方余量
            Spacer(Modifier.weight(1f))
        }
    }
}
