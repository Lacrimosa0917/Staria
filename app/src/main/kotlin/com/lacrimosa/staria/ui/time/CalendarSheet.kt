package com.lacrimosa.staria.ui.time

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.ui.icon.IconChevronLeft
import com.lacrimosa.staria.ui.icon.IconChevronRight
import java.time.LocalDate
import java.time.YearMonth
import com.lacrimosa.staria.ui.theme.ChipBackground
import com.lacrimosa.staria.ui.theme.DialogRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.KebenSong
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.theme.Scrim
import com.lacrimosa.staria.ui.theme.Border
import com.lacrimosa.staria.ui.theme.accent

private val WeekLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 日历卡片：长按时间轴日期后唤起，选择日期跳转。
 * 左上角标题使用刻本宋体（README）。
 *
 * @param diaryDates 有日记的日期集合。有日记的日期以主题色圆形背景标记，
 *   且只有这些日期可以点击；无日记的日期不可交互。
 */
@Composable
fun CalendarSheet(
    initialDate: LocalDate = LocalDate.now(),
    diaryDates: Set<LocalDate> = emptySet(),
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = LocalDate.now()
    // 最多翻到当前月份的下一个月
    val maxMonth = YearMonth.from(today).plusMonths(1)
    val canGoNext = month.isBefore(maxMonth)
    // 主题色：有日记的日期圆形背景。简约白回落墨色。
    val accent = LocalThemeColor.current.accent()

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
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(PaperWhite, RoundedCornerShape(DialogRadius))
                .border(1.dp, Border, RoundedCornerShape(DialogRadius))
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
                        enabled = canGoNext,
                        onClick = { if (canGoNext) month = month.plusMonths(1) },
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

                // ---- 日期网格：固定 6 行，窗口高度不再随行数变化 ----
                val firstDay = month.atDay(1)
                // 周一为一周起点
                val leading = (firstDay.dayOfWeek.value - 1)

                for (r in 0 until 6) {
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
                                    val hasDiary = date in diaryDates
                                    val isToday = date == today
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            // 有日记：主题色圆形背景；今天：墨色描边
                                            .then(
                                                if (hasDiary) {
                                                    Modifier.background(accent, CircleShape)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .then(
                                                if (isToday && !hasDiary) {
                                                    Modifier.border(1.dp, Ink, CircleShape)
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            // 只有写了日记的日期可交互
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                enabled = hasDiary,
                                                onClick = { onPick(date) },
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = dayOfMonth.toString(),
                                            color = if (hasDiary) PaperWhite else Ink,
                                            fontSize = 13.sp,
                                            fontWeight = if (hasDiary) FontWeight.Medium else FontWeight.Normal,
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

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(ChipBackground, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(14.dp),
            tint = if (enabled) Ink else InkMuted.copy(alpha = 0.4f),
        )
    }
}
