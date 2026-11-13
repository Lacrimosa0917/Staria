package com.lacrimosa.staria.data

import java.time.LocalDate

/**
 * 纸条：从摘录中抽取的一句话。
 *
 * 首页卡片堆叠与纸条页共用这一份数据。
 * [editedOn] 为最近编辑日期（精确至日），纸条页卡片第一行展示它。
 * [source] 可为空：未写明出处时卡片右下角不展示。
 */
data class PaperNote(
    val id: Long,
    val text: String,
    val source: String?,
    val editedOn: LocalDate,
)

/** 首页卡片堆叠上限（README：上限为五张，小于五按实际数量显示）。 */
const val MAX_STACKED_NOTES = 5

object SampleData {

    /** 当前用户昵称，头像取首字符。 */
    const val NICKNAME: String = "沈星回"

    val notes: List<PaperNote> = run {
        val base = LocalDate.now()
        fun d(daysAgo: Long) = base.minusDays(daysAgo)
        listOf(
            // 第一条不写出处，用于验证「出处可空」
            PaperNote(
                id = 1,
                text = "我们终其一生，就是要摆脱他人的期待，找到真正的自己。",
                source = "无声告白",
                editedOn = d(1),
            ),
            PaperNote(
                id = 2,
                text = "所谓无底深渊，下去，也是前程万里。",
                source = "木心",
                editedOn = d(2),
            ),
            PaperNote(
                id = 3,
                text = "在隆冬，我终于知道，我身上有一个不可战胜的夏天。",
                source = "加缪",
                editedOn = d(4),
            ),
            PaperNote(
                id = 4,
                text = "所有的大人都曾经是小孩，虽然只有少数人记得。",
                source = "小王子",
                editedOn = d(6),
            ),
            PaperNote(
                id = 5,
                text = "愿你天黑有灯，下雨有伞，路上有良人相伴。",
                source = null,
                editedOn = d(9),
            ),
            PaperNote(
                id = 6,
                text = "席地而坐，看一整夜的星星。",
                source = "随笔",
                editedOn = d(14),
            ),
        )
    }

    /** 个人主页展示的记录数量。接库后由仓储层提供，UI 无需改动。 */
    val counts: RecordCounts = RecordCounts(
        diary = 128,
        paper = notes.size,
        essay = 43,
    )

    /**
     * 日记占位数据，供时间页展示。
     *
     * 日期从今天往前铺，方便验证时间轴「靠下为最新日期」的排布。
     */
    val diaryEntries: List<DiaryEntry> = run {
        val base = java.time.LocalDate.now()
        fun d(daysAgo: Long) = base.minusDays(daysAgo)
        listOf(
            DiaryEntry(1, d(0), "今天的风很轻"),
            DiaryEntry(2, d(1), "关于早起这件事"),
            DiaryEntry(3, d(1), "晚上读完了《小王子》"),
            DiaryEntry(4, d(3), "下雨天适合发呆"),
            DiaryEntry(5, d(5), "换了新的台灯"),
            DiaryEntry(6, d(8), "生日"),
            DiaryEntry(7, d(8), "收到一封信"),
            DiaryEntry(8, d(12), "第一次做手冲"),
            DiaryEntry(9, d(20), "搬到新城市的第一周"),
            DiaryEntry(10, d(35), "旧年的最后一天"),
        )
    }
}
