package com.lacrimosa.staria.data

import java.time.LocalDate

/**
 * 编辑页的三种内容类型。
 *
 * 三者共用同一个编辑页（README：三种类型编辑页格式相同），
 * 只有「首行右侧那一栏」的含义不同：
 * 日记、随笔是标题，摘录是来源。
 */
enum class EditKind {
    Diary,
    Excerpt,
    Essay,
}

/**
 * 编辑页的一份草稿。
 *
 * 新建与编辑复用同一个结构：编辑已有条目时传入非空 [id] 与创建时的 [date]。
 *
 * 日期规则（用户确认）：**三种类型都只保存创建时的日期**，保存编辑不刷新日期。
 * 因此 [date] 一旦确定就不再改动，由调用方在新建时填入今天、
 * 编辑时沿用原条目的日期。
 */
data class EditDraft(
    val kind: EditKind,
    /** 空表示新建，非空表示编辑该 id 的条目。 */
    val id: Long? = null,
    /** 创建日期。保存时原样写回，不随编辑时间变化。 */
    val date: LocalDate = LocalDate.now(),
    /** 日记、随笔的标题。摘录不使用。 */
    val title: String = "",
    /** 摘录的来源。日记、随笔不使用。 */
    val source: String = "",
    /** 正文。随笔为 markdown 原文。 */
    val body: String = "",
) {
    /** 三类内容的首行右侧是不是标题输入区（摘录是来源）。 */
    val hasTitleField: Boolean get() = kind != EditKind.Excerpt
}
