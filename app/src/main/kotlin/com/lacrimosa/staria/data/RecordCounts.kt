package com.lacrimosa.staria.data

/**
 * 个人主页上展示的三类记录数量。
 *
 * **必须由持有数据的调用方按当前列表实时计算**（根布局的 StariaApp 负责），
 * 这样新建、删除后数字立刻跟上。不要在这里或 [SampleData] 里存一份静态值。
 */
data class RecordCounts(
    val diary: Int,
    val paper: Int,
    val essay: Int,
)
