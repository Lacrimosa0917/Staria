package com.lacrimosa.staria.ui.time

import androidx.compose.foundation.background
import com.lacrimosa.staria.ui.theme.ActionRadius
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.BorderSelected
import com.lacrimosa.staria.ui.theme.DialogRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.SelectionBoxRadius
import com.lacrimosa.staria.ui.theme.SelectionBoxSize
import com.lacrimosa.staria.ui.theme.accent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 删除确认弹窗。
 *
 * README：首次点击删除时弹窗确认，并提供「不再展示」选项，
 * 未勾选则下次还会展示。
 */
@Composable
fun DeleteConfirmDialog(
    dontAskAgain: Boolean,
    onDontAskAgainChange: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    // Dialog 是独立窗口，CompositionLocal 不跨窗口传递，
    // 在 Dialog 外读取主题色，再在窗口内重新 provide。
    val themeColor = LocalThemeColor.current

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(LocalThemeColor provides themeColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 40.dp)
                    .fillMaxWidth()
                    .background(PaperWhite, RoundedCornerShape(DialogRadius))
                    .border(1.dp, Border, RoundedCornerShape(DialogRadius))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(horizontal = 22.dp, vertical = 20.dp),
            ) {
                Text(
                    text = "删除这篇日记？",
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "删除后将移入星尘，不再在此页展示。",
                    color = InkMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )

                Spacer(Modifier.height(16.dp))

                // ---- 不再展示 ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onDontAskAgainChange(!dontAskAgain) },
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(SelectionBoxSize)
                            .then(
                                if (dontAskAgain) {
                                    Modifier.background(Ink, RoundedCornerShape(SelectionBoxRadius))
                                } else {
                                    Modifier.border(1.dp, BorderSelected, RoundedCornerShape(SelectionBoxRadius))
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dontAskAgain) {
                            Text(
                                text = "✓",
                                color = PaperWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = "不再展示此确认",
                        color = Ink,
                        fontSize = 13.sp,
                    )
                }

                Spacer(Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogButton(label = "取消", primary = false, onClick = onCancel)
                    Spacer(Modifier.width(10.dp))
                    DialogButton(label = "删除", primary = true, onClick = onConfirm)
                }
            }
        }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    // 主按钮（确认）用主题色：简约白回落为墨色，其余主题取其强调色
    val accent = LocalThemeColor.current.accent()
    Box(
        modifier = Modifier
            .background(
                color = if (primary) accent else Color.Transparent,
                shape = RoundedCornerShape(ActionRadius),
            )
            .then(
                if (primary) Modifier
                else Modifier.border(1.dp, Border, RoundedCornerShape(ActionRadius)),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 9.dp),
    ) {
        Text(
            text = label,
            color = if (primary) PaperWhite else Ink,
            fontSize = 14.sp,
            fontWeight = if (primary) FontWeight.Medium else FontWeight.Normal,
        )
    }
}
