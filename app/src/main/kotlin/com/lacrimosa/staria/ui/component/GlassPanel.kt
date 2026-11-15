package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.GlassSpec
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.stariaGlassEffects

/**
 * 液态玻璃面板：全项目玻璃控件的唯一入口。
 *
 * 材质只由三样东西构成：
 * 1. **模糊 + 折射**（库的 RenderEffect / RuntimeShader）
 * 2. **1dp 描边**
 * 3. **上下纯白高光边**
 *
 * 刻意没有「半透明白色蒙层」——蒙层会把玻璃糊成一块实心白片，
 * 质感全丢。可读性靠模糊把背景正文糊成色晕来保证，见 GlassSpec.BLUR_DP。
 *
 * 传 `backdrop = null` 时（独立 Dialog 窗口采样不到宿主背景）
 * 退回半透明纸面，保证同一个调用点在哪都安全。
 *
 * @param backdrop 由根布局录制、经 `LocalGlassBackdrop` 下发的背景层
 * @param shape 必须是 RoundedCornerShape 系（库的 lens 只支持 CornerBasedShape）
 * @param refractionScale 折射强度倍率。胶囊类形状传小于 1 的值
 */
@Composable
fun GlassPanel(
    backdrop: Backdrop?,
    shape: Shape,
    modifier: Modifier = Modifier,
    border: Boolean = true,
    refractionScale: Float = 1f,
    /** 上下边缘的白色高光厚度。0.dp 表示不画（圆形控件不需要）。 */
    edgeHighlight: Dp = 0.dp,
    /** 模糊半径（dp）。默认走 GlassSpec.BLUR_DP；tooltip 传更高的值。 */
    blurDp: Float = GlassSpec.BLUR_DP,
    /** 蒙层不透明度（0..1）。tooltip 用它压背景、保可读性；普通控件为 0。 */
    surfaceAlpha: Float = 0f,
    /**
     * 只变换玻璃这一层的缩放块。
     *
     * 需要「面板缩放但采样到的背景不变」时用（tooltip 展开/收起的弹性缩放）。
     * 与底栏 glassBarSurface、InteractiveGlassPanel 同原理：
     * 若写在外层 graphicsLayer，会把采样结果（背景阴影、正文）一起缩放，
     * 阴影跟着弹动，很突兀。
     */
    layerBlock: GraphicsLayerScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    // 颜色令牌现在是 Composable 属性，需在组合期取出后供下方闭包捕获
    val paperWhite = PaperWhite
    val borderColor = Border

    val body = if (backdrop != null) {
        modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = { stariaGlassEffects(refractionScale, blurDp) },
            layerBlock = layerBlock,
        )
    } else {
        // 拿不到背景层时的退路：一层薄纸，不假装是玻璃
        modifier.background(paperWhite.copy(alpha = GlassSpec.FALLBACK_ALPHA), shape)
    }

    Box(
        modifier = body.then(
            if (border) Modifier.border(1.dp, borderColor.copy(alpha = 0.9f), shape) else Modifier,
        ),
    ) {
        // 蒙层：叠在玻璃之上、内容之下，只在需要保可读性时启用（tooltip）。
        // 用 clip 保证蒙层不溢出 shape 圆角。
        if (surfaceAlpha > 0f && backdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(paperWhite.copy(alpha = surfaceAlpha)),
            )
        }

        // 上下纯白高光：画在玻璃之上、内容之下。
        // 底色是 #F5F5F5，玻璃本身是透的，这两道白边是「它是一块独立玻璃」的
        // 主要视觉依据，也是与背景拉开层次的手段。
        if (edgeHighlight > 0.dp) {
            Canvas(modifier = Modifier.matchParentSize().clip(shape)) {
                val h = edgeHighlight.toPx()
                drawRect(
                    color = paperWhite,
                    topLeft = Offset.Zero,
                    size = Size(size.width, h),
                )
                drawRect(
                    color = paperWhite,
                    topLeft = Offset(0f, size.height - h),
                    size = Size(size.width, h),
                )
            }
        }
        content()
    }
}
