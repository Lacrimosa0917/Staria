package com.lacrimosa.staria.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.R
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.PageBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 图标边长。 */
private val SplashIconSize = 108.dp

/** 圆角半径。取边长的 24%，接近系统图标「方圆之间」的比例。 */
private val SplashIconRadius = 26.dp

/** 图标与下方小字的间距。 */
private val SplashGap = 22.dp

/**
 * 整段动画的绝对时间轴（毫秒），总和恰好 1000ms。
 *
 * 关键点：**用绝对时刻 `delay` 推进，不用 `coroutineScope` 等所有动画跑完**。
 * `coroutineScope` 会等到每一条 `animateTo` 真正收敛才返回，而弹簧动画的
 * 收敛时间远大于 `durationMillis`（实测多出约 200ms），总时长就会漂到 1.2s。
 * 这里每条动画各自 `launch` 出去，主协程只按时间轴走，节奏才是确定的。
 *
 * ⚠️ 主协程里 `delay` 的时长必须写**绝对时刻**，不能写「相对上一段的差值」：
 * 上面那些 `launch` 是立即返回的，主协程在那一刻只经过了 0ms。
 * 曾经写成 `delay(ExitStartMs - IconFadeInMs)`，整段动画因此只剩 600ms。
 */
private const val IconFadeInMs = 400      // 0 → 400：图标淡入
private const val TagDelayMs = 160        // 160 起：小字淡入 + 上移
private const val TagFadeInMs = 340       // 160 → 500
private const val TagRiseMs = 400         // 160 → 560
private const val ExitStartMs = 820       // 820 起淡出
private const val ExitMs = 180            // 820 → 1000

/**
 * 开屏动画。
 *
 * 目的不是「好看」，而是**填补进程启动到首帧之间的空窗**：
 * 系统窗口背景（见 `themes.xml` 的 `windowBackground`）已经改成
 * 与 [PageBackground] 相同的 #F5F5F5，所以从点图标到这段动画出现
 * 中间不会有白屏或色差跳变；动画结束后淡出，露出下面已经就绪的 [StariaApp]。
 *
 * 节奏（总 1 秒，时间轴见文件头常量）：
 * - 0–400ms：图标淡入，同时从 0.92 轻微放大到 1.0（弹簧，落定时有一点点回弹）
 * - 160–560ms：下方小字淡入并上移 8dp，比图标晚一点，形成先后关系
 * - 820–1000ms：整层淡出
 *
 * 图标用 `splash_icon`（由 ic_launcher_foreground 重新合成）按圆角矩形裁切——
 * 「星笺」二字是品牌资产里唯一一眼能认出的图形，开屏直接用比另画一套更稳。
 *
 * @param onFinished 淡出结束时回调，由调用方移除本层
 */
@Composable
fun StariaSplash(onFinished: () -> Unit) {
    val containerAlpha = remember { Animatable(1f) }
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.92f) }
    val tagAlpha = remember { Animatable(0f) }
    // 小字的上移量（dp），从 8dp 收到 0
    val tagRise = remember { Animatable(8f) }

    // 入场动画单独一个 effect：它的生命周期与「总时长」无关，
    // 不会被主时间轴的结束打断。
    LaunchedEffect(Unit) {
        launch {
            iconAlpha.animateTo(1f, tween(durationMillis = IconFadeInMs))
        }
        launch {
            iconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 260f),
            )
        }
        launch {
            delay(TagDelayMs.toLong())
            tagAlpha.animateTo(1f, tween(durationMillis = TagFadeInMs))
        }
        launch {
            delay(TagDelayMs.toLong())
            tagRise.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = TagRiseMs, easing = LinearOutSlowInEasing),
            )
        }
    }

    // 计时与淡出单独一个 effect，保证总时长严格可控
    LaunchedEffect(Unit) {
        val startedAt = System.currentTimeMillis()
        android.util.Log.i("StariaSplash", "splash start")

        // 必须写绝对时刻，不能减 IconFadeInMs：
        // 上面的 launch 立即返回，主协程此刻只经过了 0ms，
        // 减掉入场时长会让整段动画提前 400ms 结束（曾经因此只剩 600ms）。
        delay(ExitStartMs.toLong())
        containerAlpha.animateTo(0f, tween(durationMillis = ExitMs))
        android.util.Log.i(
            "StariaSplash",
            "splash end, elapsed=${System.currentTimeMillis() - startedAt}ms",
        )
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = containerAlpha.value }
            .background(PageBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                // 用独立的位图 splash_icon，不能用 @mipmap/ic_launcher：
                // API 26+ 会把后者解析成 mipmap-anydpi-v26/ic_launcher.xml
                // （自适应图标），painterResource 只接受矢量或栅格，会直接抛
                // IllegalArgumentException: Only VectorDrawables and rasterized asset types…
                //
                // splash_icon 由 ic_launcher_foreground 的「星笺」二字重新合成：
                // 原图文字只占画布 55.6% 宽，直接放大到 108dp 会显得又小又空，
                // 这里按 58% 重新排版并铺上 #FCFBF9 底色，各 dpi 均已生成。
                painter = painterResource(R.mipmap.splash_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(SplashIconSize)
                    .graphicsLayer {
                        alpha = iconAlpha.value
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    }
                    .clip(RoundedCornerShape(SplashIconRadius)),
            )

            Spacer(Modifier.height(SplashGap))

            Text(
                text = stringResource(R.string.splash_tagline),
                color = InkMuted,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = tagAlpha.value
                    // tagRise 是 dp 值，graphicsLayer 的 translationY 要像素，
                    // 这里用 scope 自带的 density 换算（它就是 Density.density）。
                    translationY = tagRise.value * density
                },
            )
        }
    }
}
