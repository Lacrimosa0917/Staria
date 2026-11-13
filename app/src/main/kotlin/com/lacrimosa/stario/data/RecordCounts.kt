package com.lacrimosa.stario.data

/**
 * 个人主页上展示的三类记录数量。
 *
 * 目前没有持久层，数量取自 [SampleData]；接入数据库后替换这里的数据源即可，
 * UI 不需要改动。
 */
data class RecordCounts(
    val diary: Int,
    val paper: Int,
    val essay: Int,
)
