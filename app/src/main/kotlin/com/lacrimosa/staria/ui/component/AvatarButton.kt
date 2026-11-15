package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop

/**
 * 圆形头像按钮：昵称首字符（液态玻璃）。
 *
 * README 要求四个查看页（首页 / 时间 / 纸条 / 思索）都能唤起个人主页，
 * 所以这个按钮放在根布局由所有页面共用，而不是各页面各画一份。
 *
 * 玻璃参数说明：头像只有 44dp，蒙层给到 0.90 —— 太小面积的玻璃如果太透，
 * 首字符会与背后透上来的内容糊在一起，认不出是头像。
 */
@Composable
fun AvatarButton(
    nickname: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InteractiveGlassPanel(
        backdrop = LocalGlassBackdrop.current,
        shape = CircleShape,
        pressInflate = 3.dp,
        onClick = onClick,
        modifier = modifier
            .size(AvatarSize)
            .semantics { contentDescription = "个人主页" },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                // 取首个码点，避免把表情这类代理对切成半个字符
                text = nickname.take(1),
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
