package com.lacrimosa.stario.ui.time

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lacrimosa.stario.ui.icon.IconChevronLeft
import com.lacrimosa.stario.ui.icon.IconChevronRight
import com.lacrimosa.stario.ui.theme.Ink
import com.lacrimosa.stario.ui.theme.InkMuted
import com.lacrimosa.stario.ui.theme.KebenSong
import com.lacrimosa.stario.ui.theme.PaperWhite
import java.time.LocalDate
import java.time.YearMonth

private val Hairline = Color(0xFFE6E2DA)
private val WeekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 日历卡片：长按时间轴日期后唤起，选择日期跳转。
 * 左上角标题使用刻本宋体（README）。
 */
@Composable
fun CalendarSheet(
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = LocalDate.now()

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
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
                    .background(PaperWhite, RoundedCornerShape(18.dp))
                    .border(1.dp, Hairline, RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {}
                    .padding(horizontal = 18.dp, vertical = 18.dp),
            ) {
                // ---- 标题：刻本宋体 ----
                Text(
                    text = "选择日期",
                    fontFamily = KebenSong,
                    fontSize = 18.sp,
                    color = Ink,
                )

                Spacer(Modifier.height(14.dp))

                // ---- 月份切换 ----
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonthArrow(
                        icon = IconChevronLeft,
                        description = "上个月",
                        onClick = { month = month.minusMonths(1) },
                    )
                    Text(
                        text = "${month.year} 年 ${month.monthValue} 月",
                        modifier = Modifier.weight(1f),
                        color = Ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                    MonthArrow(
                        icon = IconChevronRight,
                        description = "下个月",
                        onClick = { month = month.plusMonths(1) },
                    )
                }

                Spacer(Modifier.height(10.dp))

                // ---- 星期表头 ----
                Row(modifier = Modifier.fillMaxWidth()) {
                    WeekLabels.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            color = InkMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ---- 日期网格 ----
                val firstDay = month.atDay(1)
                // 周一为一周起点
                val leading = (firstDay.dayOfWeek.value - 1)
                val total = leading + month.lengthOfMonth()
                val rows = (total + 6) / 7

                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val index = r * 7 + c
                            val dayOfMonth = index - leading + 1
                            val inMonth = dayOfMonth in 1..month.lengthOfMonth()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (inMonth) {
                                    val date = month.atDay(dayOfMonth)
                                    val isToday = date == today
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .then(
                                                if (isToday) {
                                                    Modifier.border(1.dp, Ink, CircleShape)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = { onPick(date) },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            color = Ink,
                                            fontSize = 13.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(Color(0xFFF3F0EA), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(14.dp),
            tint = Ink,
        )
    }
}
