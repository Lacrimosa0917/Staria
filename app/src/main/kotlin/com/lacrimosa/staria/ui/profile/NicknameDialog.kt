package com.lacrimosa.staria.ui.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lacrimosa.staria.data.NICKNAME_MAX_WEIGHT
import com.lacrimosa.staria.data.nicknameWeight
import com.lacrimosa.staria.ui.theme.ActionRadius
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.BorderDisabled
import com.lacrimosa.staria.ui.theme.Danger
import com.lacrimosa.staria.ui.theme.DialogRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.InputBackground
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.Scrim

/**
 * 修改昵称浮窗（README：包含取消与确定两个选项）。
 *
 * 字数以「权重」计：汉字 3、英文与数字 1，上限 24（即 8 个汉字 / 24 个字母）。
 * 输入框右侧实时显示 `已用/24`，超出时转为红色，同时确定按钮置灰不可点。
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

    val used = nicknameWeight(text)
    val overLimit = used > NICKNAME_MAX_WEIGHT
    val canConfirm = text.isNotBlank() && !overLimit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
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
                    .background(PaperWhite, RoundedCornerShape(DialogRadius))
                    .border(1.dp, Border, RoundedCornerShape(DialogRadius))
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

                // 输入框 + 右侧计数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(InputBackground, RoundedCornerShape(ActionRadius))
                            .border(
                                width = 1.dp,
                                color = if (overLimit) Danger else Border,
                                shape = RoundedCornerShape(ActionRadius),
                            )
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
                            // 不再截断输入：允许超出后由计数变红并禁用确定，
                            // 让用户看到自己到底超了多少，而不是输不进去。
                            onValueChange = { text = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(color = Ink, fontSize = 15.sp),
                            singleLine = true,
                            cursorBrush = SolidColor(Ink),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    Text(
                        text = "$used/$NICKNAME_MAX_WEIGHT",
                        color = if (overLimit) Danger else InkMuted,
                        fontSize = 12.sp,
                        fontWeight = if (overLimit) FontWeight.Medium else FontWeight.Normal,
                    )
                }

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
                        enabled = canConfirm,
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
    // 不可用时整体压成全灰，避免和「可用的黑色主按钮」混淆
    val bg = when {
        !primary -> Color.Transparent
        !enabled -> BorderDisabled
        else -> Ink
    }
    val fg = when {
        !primary && !enabled -> InkMuted.copy(alpha = 0.5f)
        !primary -> Ink
        !enabled -> PaperWhite
        else -> PaperWhite
    }

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(ActionRadius))
            .then(
                if (primary) Modifier
                else Modifier.border(1.dp, Border, RoundedCornerShape(ActionRadius)),
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
            color = fg,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
