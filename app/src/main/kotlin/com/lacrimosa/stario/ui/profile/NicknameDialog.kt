package com.lacrimosa.stario.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lacrimosa.stario.data.clampNickname
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted
import com.lacrimosa.stario.ui.theme.PaperWhite

private val Hairline = Color(0xFFE6E2DA)

/**
 * 修改昵称浮窗（README：包含取消与确定两个选项）。
 *
 * 输入通过 [clampNickname] 实时截断，超出上限时直接不允许继续输入，
 * 而不是输入完再报错。
 */
@Composable
fun NicknameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(PaperWhite, RoundedCornerShape(18.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    // 阻止点击穿透到遮罩
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "修改昵称",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF8F5), RoundedCornerShape(10.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = "输入昵称",
                            color = InkMuted,
                            fontSize = 15.sp,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = clampNickname(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            color = Ink,
                            fontSize = 15.sp,
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(Ink),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "最多 8 个汉字，3 个英文或数字字符视为 1 个汉字",
                    color = InkMuted,
                    fontSize = 11.sp,
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogAction(
                        label = "取消",
                        primary = false,
                        onClick = {
                            focusManager.clearFocus()
                            onDismiss()
                        },
                    )
                    Spacer(Modifier.width(10.dp))
                    DialogAction(
                        label = "确定",
                        primary = true,
                        enabled = text.isNotBlank(),
                        onClick = {
                            focusManager.clearFocus()
                            onConfirm(text)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DialogAction(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .background(
                color = if (primary) Ink else Color.Transparent,
                shape = RoundedCornerShape(9.dp),
            )
            .then(
                if (primary) Modifier
                else Modifier.border(1.dp, Hairline, RoundedCornerShape(9.dp)),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = when {
                !enabled -> InkMuted
                primary -> PaperWhite
                else -> Ink
            },
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
