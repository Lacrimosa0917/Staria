package com.lacrimosa.stario.data

/**
 * 纸条：从摘录中抽取的一句话。
 *
 * 首页只做展示，暂不落库；[sampleNotes] 为占位数据，后续接入 Room 时替换。
 */
data class PaperNote(
    val id: Long,
    val text: String,
    val source: String,
)

/** 首页卡片堆叠上限（README：上限为五张，小于五按实际数量显示）。 */
const val MAX_STACKED_NOTES = 5

object SampleData {

    /** 当前用户昵称，头像取首字符。 */
    const val NICKNAME: String = "沈星回"


    val notes: List<PaperNote> = listOf(
        PaperNote(
            id = 1,
            text = "我们终其一生，就是要摆脱他人的期待，找到真正的自己。",
            source = "无声告白",
        ),
        PaperNote(
            id = 2,
            text = "所谓无底深渊，下去，也是前程万里。",
            source = "木心",
        ),
        PaperNote(
            id = 3,
            text = "在隆冬，我终于知道，我身上有一个不可战胜的夏天。",
            source = "加缪",
        ),
        PaperNote(
            id = 4,
            text = "所有的大人都曾经是小孩，虽然只有少数人记得。",
            source = "小王子",
        ),
        PaperNote(
            id = 5,
            text = "愿你天黑有灯，下雨有伞，路上有良人相伴。",
            source = "佚名",
        ),
        PaperNote(
            id = 6,
            text = "席地而坐，看一整夜的星星。",
            source = "随笔",
        ),
    )

    /** 个人主页展示的记录数量。接库后由仓储层提供，UI 无需改动。 */
    val counts: RecordCounts = RecordCounts(
        diary = 128,
        paper = notes.size,
        essay = 43,
    )
}
