package com.lacrimosa.staria.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.ui.theme.BorderOnCard
import com.lacrimosa.staria.ui.theme.CardPaper
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.Scrim

/**
 * 纸条展开浮层。
 *
 * 版式与首页卡片堆叠的前排卡片一致：
 * 纸色卡面、正文居中、出处以破折号前缀放在下方。
 * 只显示一张，位于屏幕中心；背景整体压暗，点击压暗区退出。
 */
@Composable
fun ExpandedNoteOverlay(
    note: PaperNote,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 压暗其余内容
            .background(Scrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            // 与首页卡片同样的比例：宽取屏宽 56%，上限 224dp，宽高比 1.36
            val cardWidth = minOf(maxWidth * 0.62f, 260.dp)
            val cardHeight = cardWidth * 1.36f

            Box(
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    // 吞掉卡片自身的点击，避免穿透到压暗层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .background(CardPaper, RoundedCornerShape(CardRadius))
                    .border(1.dp, BorderOnCard, RoundedCornerShape(CardRadius)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = note.text,
                            // 必须撑满宽度，否则 Text 按内容宽摆放，
                            // textAlign = Center 不起作用
                            modifier = Modifier.fillMaxWidth(),
                            color = Ink,
                            fontSize = 16.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // 出处：与首页一致，破折号前缀；未写明时不展示
                    note.source?.takeIf { it.isNotBlank() }?.let { source ->
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = "— $source",
                            color = InkMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
