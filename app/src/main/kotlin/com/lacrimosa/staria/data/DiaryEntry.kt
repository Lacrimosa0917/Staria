package com.lacrimosa.staria.data

import java.time.LocalDate

/**
 * 日记条目。
 *
 * 时间页按 [date] 分组展示，同一天的日记各占一行。
 */
data class DiaryEntry(
    val id: Long,
    val date: LocalDate,
    val title: String,
    /** 首次删除后置为 true，移入星尘；时间页不再展示。 */
    val deleted: Boolean = false,
)

/** 按日期分组后的结果，时间轴上一个节点对应一组。 */
data class DiaryGroup(
    val date: LocalDate,
    val entries: List<DiaryEntry>,
)
