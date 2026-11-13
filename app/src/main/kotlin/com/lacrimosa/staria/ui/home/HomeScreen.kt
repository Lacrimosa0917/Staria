package com.lacrimosa.staria.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.lacrimosa.staria.data.MAX_STACKED_NOTES
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.PaperCardStack
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.PaperWhite

/**
 * 首页：极简留白布局。
 *
 * 纵向两段：
 * 1. 中部偏上——刻本宋体大字号 slogan「星笺」
 * 2. 中部偏下——纸条卡片堆叠
 *
 * 左上角头像与底部胶囊底栏由根布局 [com.lacrimosa.staria.ui.StariaApp] 提供：
 * 四个查看页都要保留头像与底栏，放在这里会重复实现。
 */
@Composable
fun HomeScreen(
    notes: List<PaperNote> = SampleData.notes,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PaperWhite),
    ) {
        val topGap = maxHeight * 0.12f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // slogan 顶部留一点，配合根布局的头像区形成"中间靠上"
            Spacer(Modifier.height(topGap))

            Text(
                text = stringResource(R.string.slogan),
                fontFamily = KebenSong,
                fontSize = 46.sp,
                lineHeight = 60.sp,
                color = Ink,
                textAlign = TextAlign.Center,
                letterSpacing = 10.sp,
            )

            Spacer(Modifier.height(30.dp))

            PaperCardStack(
                notes = notes.take(MAX_STACKED_NOTES),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
