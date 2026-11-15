package com.lacrimosa.staria.ui.paper

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.ui.component.GlassPanel
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.GlassSpec
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.Scrim
import kotlinx.coroutines.launch

/**
 * 纸条展开浮层。
 *
 * 版式与首页卡片堆叠的前排卡片一致：
 * 正文居中、出处以破折号前缀放在下方。
 * 卡面改为与 tooltip 相同的液态玻璃（高模糊 + 薄蒙层），
 * 压在被压暗的背景上，层次更通透；点击压暗区退出，带淡入淡出。
 */
@Composable
fun ExpandedNoteOverlay(
    note: PaperNote,
    onDismiss: () -> Unit,
) {
    // 进入淡入：alpha 0 → 1，带轻微回弹的弹簧
    val appear = remember { Animatable(0f) }
    // 正在退出：淡出动画播完才回调 onDismiss
    var closing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        appear.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f),
        )
    }

    fun requestClose() {
        if (closing) return
        closing = true
        scope.launch {
            appear.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 420f))
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 压暗其余内容，随进入/退出一起淡入淡出
            .background(Scrim.copy(alpha = Scrim.alpha * appear.value.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::requestClose,
            ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints {
            // 与首页卡片同样的比例：宽取屏宽 56%，上限 224dp，宽高比 1.36
            val cardWidth = minOf(maxWidth * 0.62f, 260.dp)
            val cardHeight = cardWidth * 1.36f

            GlassPanel(
                backdrop = LocalGlassBackdrop.current,
                shape = RoundedCornerShape(CardRadius),
                // 与 tooltip 相同的玻璃材质：高模糊 + 薄蒙层，保证正文可读
                refractionScale = 0.6f,
                blurDp = GlassSpec.TOOLTIP_BLUR_DP,
                surfaceAlpha = GlassSpec.TOOLTIP_SURFACE_ALPHA,
                modifier = Modifier
                    .size(cardWidth, cardHeight)
                    // 吞掉卡片自身的点击，避免穿透到压暗层
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .graphicsLayer { alpha = appear.value.coerceIn(0f, 1f) },
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
