package com.lacrimosa.staria.data

import java.time.LocalDate

/**
 * 星尘（回收站）里一条记录的内容类型。
 *
 * 三类内容的字段不完全一样：日记、随笔有标题，摘录有来源。
 * 回收站把它们摊平成一个统一结构，恢复时再按类型还原回各自的列表。
 */
enum class TrashKind {
    Diary,
    Excerpt,
    Essay;

    /** 卡片第一行显示的文案。 */
    val label: String
        get() = when (this) {
            Diary -> "日记"
            Excerpt -> "摘录"
            Essay -> "随笔"
        }
}

/**
 * 星尘里的一条记录。
 *
 * [trashId] 是回收站内部的唯一键（不能直接用 [originalId]：
 * 三种类型的 id 各自从 1 开始，会撞号）。
 *
 * [deletedAt] 只用于排序（README：新删除的在上方），不展示。
 */
data class TrashItem(
    val trashId: Long,
    val kind: TrashKind,
    /** 原条目 id，恢复时按它写回对应列表。 */
    val originalId: Long,
    /** 原条目的创建日期（不是删除日期）。 */
    val date: LocalDate,
    /** 日记、随笔的标题；摘录为空。 */
    val title: String,
    /** 正文。摘录即为摘录内容。 */
    val body: String,
    /** 摘录的来源，可为空。 */
    val source: String?,
    /** 删除顺序，用于倒序排列。 */
    val deletedAt: Long,
)
