package com.lacrimosa.stario.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.stario.ui.theme.CardPaper
import com.lacrimosa.stario.ui.theme.Ink

/**
 * 圆形头像按钮：昵称首字符。
 *
 * README 要求四个查看页（首页 / 时间 / 纸条 / 思索）都能唤起个人主页，
 * 所以这个按钮放在根布局由所有页面共用，而不是各页面各画一份。
 */
@Composable
fun AvatarButton(
    nickname: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(44.dp)
            .semantics { contentDescription = "个人主页" },
        shape = CircleShape,
        color = CardPaper,
        border = BorderStroke(1.dp, Color(0xFFE6E2DA)),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
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
