package com.lacrimosa.staria.data

import java.time.LocalDate

/**
 * 随笔。
 *
 * 思索页列表展示 [title] 与 [body] 的预览；
 * 点击进入二级查看页展示 [body] 全文。
 */
data class Essay(
    val id: Long,
    val date: LocalDate,
    val title: String,
    val body: String,
)
