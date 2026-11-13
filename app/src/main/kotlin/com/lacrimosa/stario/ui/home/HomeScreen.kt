package com.lacrimosa.stario.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.R
import com.lacrimosa.stario.data.MAX_STACKED_NOTES
import com.lacrimosa.stario.data.PaperNote
import com.lacrimosa.stario.data.SampleData
import com.lacrimosa.stario.ui.component.CreateKind
import com.lacrimosa.stario.ui.component.PaperCardStack
import com.lacrimosa.stario.ui.component.StarioBottomBar
import com.lacrimosa.stario.ui.component.StarioTab
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.KebenSong
import com.lacrimosa.stario.ui.theme.PaperWhite

/**
 * 首页：极简留白布局。
 *
 * 纵向四段：
 * 1. 顶部左上——圆形头像按钮（昵称首字符）
 * 2. 中部偏上——刻本宋体大字号 slogan「星笺」
 * 3. 中部偏下——纸条卡片堆叠
 * 4. 底部——胶囊页面底栏 + 圆形新建按钮
 */
@Composable
fun HomeScreen(
    nickname: String = SampleData.NICKNAME,
    notes: List<PaperNote> = SampleData.notes,
    onAvatarClick: () -> Unit = {},
    onCreate: (CreateKind) -> Unit = {},
) {
    var currentTab by remember { mutableStateOf(StarioTab.Home) }
    var navExpanded by remember { mutableStateOf(false) }
    var createExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // 点空白处收起展开项
                if (navExpanded || createExpanded) {
                    navExpanded = false
                    createExpanded = false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // ---------- 顶部：头像 ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 14.dp),
            ) {
                AvatarButton(nickname = nickname, onClick = onAvatarClick)
            }

            // ---------- 中部：slogan + 卡片堆叠 ----------
            // 用 Column + weight 分配剩余高度：slogan 固定在中部偏上，
            // 卡片堆叠吃掉余下空间并自适应高度
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val topGap = maxHeight * 0.12f

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
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

            // ---------- 底部：底栏 + 新建 ----------
            StarioBottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                current = currentTab,
                onSelect = { currentTab = it },
                onCreate = onCreate,
                navExpanded = navExpanded,
                onNavExpandedChange = { navExpanded = it },
                createExpanded = createExpanded,
                onCreateExpandedChange = { createExpanded = it },
            )

            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun AvatarButton(
    nickname: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.size(44.dp),
        shape = CircleShape,
        color = CardPaper,
        border = BorderStroke(1.dp, Color(0xFFE6E2DA)),
        shadowElevation = 2.dp,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = nickname.take(1),
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
