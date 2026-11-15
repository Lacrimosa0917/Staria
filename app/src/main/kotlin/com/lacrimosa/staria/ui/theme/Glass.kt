package com.lacrimosa.staria.ui.theme

import android.os.Build
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 液态玻璃材质的参数与效果栈。
 *
 * 底层用 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)
 * （`io.github.kyant0:backdrop`）：它把背景内容录进一个 GraphicsLayer，
 * 再在浮层里重新绘制并施加 RenderEffect。
 *
 * ## 为什么要包一层
 *
 * 1. **效果有系统版本门槛**。库的 [blur] 依赖 `RenderEffect`（Android 12 / API 31），
 *    [lens] 折射依赖 `RuntimeShader`（Android 13 / API 33）。本项目 minSdk 26，
 *    低版本上库内部会静默跳过效果，只剩一层纯色。
 *    这里主动检测并降级为「半透明纸面 + 描边」，与原有视觉语言一致。
 *
 * 2. **`lens` 的形状限制**。库要求 shape 是 `CornerBasedShape`，否则抛
 *    `UnsupportedOperationException`。调用方只传 RoundedCornerShape 系
 *    （胶囊、圆钮、圆角卡片都属于这一类）。
 *
 * 3. **阅读性**。README 规定全局背景为白色、正文为墨色。玻璃若太透，
 *    白底上的墨字会跟背景一起糊。这里固定叠一层半透明白 surface 保对比度。
 *
 * ## 与「白纸 + 墨线」基调的关系
 *
 * 参数刻意压得很轻：折射高度 10dp、模糊 2dp、surface 白度 0.62。
 * 目的是让控件有「一层薄玻璃盖在纸面上」的通透感，
 * 而不是 iOS 那种强烈的厚玻璃反光。
 */
internal object GlassSpec {

    /**
     * 折射高度（dp）：玻璃边缘那圈折射带的厚度。
     *
     * 库的硬约束是 <= shape 的最小圆角半径。这里取 14dp ——
     * 圆角最小的控件是 38dp 的二级页圆钮（半径 19dp），14dp 仍在安全区间；
     * 胶囊类形状会额外乘 refractionScale（0.4），不会因为半径大而失控。
     *
     * 数值越大，边缘「把背景折过来」的范围越宽，玻璃看起来越厚实。
     */
    const val REFRACTION_HEIGHT_DP = 14f

    /**
     * 折射位移：折射带内像素被推开多远。
     *
     * 与 [REFRACTION_HEIGHT_DP] 配合：高度管「多宽」，位移管「推多远」。
     * 位移应略大于高度，边缘才会出现明显的「厚边聚光」而不是一道干瘪的线。
     */
    const val REFRACTION_AMOUNT_DP = 18f

    /**
     * 背景模糊半径。
     *
     * 决定透过来的内容「糊到什么程度」：值越大越像磨砂，越小越像清玻璃。
     * 这里取 2dp，保留背景的大致形态（能看出下面有卡片、有文字），
     * 只做轻微虚化 —— 追求的是玻璃的通透感，而不是靠模糊掩盖内容。
     * 可读性不在这层解决。
     */
    const val BLUR_DP = 2f

    /** 低版本降级时的纸面不透明度。只有拿不到 backdrop 时才用。 */
    const val FALLBACK_ALPHA = 0.94f
}

/** 当前系统是否支持 RenderEffect（模糊）。 */
internal val supportsRenderEffect: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** 当前系统是否支持 RuntimeShader（折射）。 */
internal val supportsRuntimeShader: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * 效果栈：饱和 ⇒ 模糊 ⇒ 折射。
 *
 * 顺序是库的硬要求：折射必须作用在已模糊的像素上，
 * 反了之后边缘会出现锯齿状伪影。
 *
 * @param refractionScale 折射强度倍率。胶囊这类「圆角半径 = 高度/2」的形状
 *   几何上接近圆形，`lens` 会在两端生成很宽的弧形折射带（看起来像两道月牙）。
 *   这类形状传小于 1 的倍率把它压细；普通圆角卡片传 1。
 */
internal fun BackdropEffectScope.stariaGlassEffects(
    refractionScale: Float = 1f,
) {
    // 玻璃盖住白纸后颜色会发灰，vibrancy（饱和度 ×1.5）把纸色拉回来
    vibrancy()
    if (supportsRenderEffect) {
        blur(GlassSpec.BLUR_DP.dp.toPx())
    }
    if (supportsRuntimeShader && refractionScale > 0f) {
        // 折射高度不得超过 shape 的最小圆角，否则角落会出现断裂。
        //
        // depthEffect 关掉：它会给玻璃下沿加一道厚厚的立体阴影，
        // 在纯白背景上反而像「浮起来的塑料块」，与「薄玻璃盖在纸上」的基调相反。
        lens(
            refractionHeight = GlassSpec.REFRACTION_HEIGHT_DP.dp.toPx() * refractionScale,
            refractionAmount = GlassSpec.REFRACTION_AMOUNT_DP.dp.toPx() * refractionScale,
            depthEffect = false,
            chromaticAberration = false,
        )
    }
}

