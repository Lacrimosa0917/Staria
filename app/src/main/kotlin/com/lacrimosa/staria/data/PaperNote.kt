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
    const val NICKNAME: String = "Staria"

    /** 所有内置默认内容的统一日期（README：日期均默认为 2026-09-17）。 */
    private val DEFAULT_DATE: LocalDate = LocalDate.of(2026, 9, 17)

    val notes: List<PaperNote> = listOf(
        PaperNote(
            id = 1,
            text = "世界上只有一种真正的英雄主义，那就是认清生活真相，依旧热爱生活。",
            source = "罗曼·罗兰",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 2,
            text = "你能在浪费时间中获得乐趣，就不是浪费时间。",
            source = "罗素",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 3,
            text = "对未来的真正慷慨，是把一切都献给现在。",
            source = "加缪",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 4,
            text = "不要在喧嚣处歌吟，否则，你的歌声也将成为那喧嚣的一部分。",
            source = "川端康成《雪国》",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 5,
            text = "我们都还看不清楚自身，就渴望被人理解，这是错误的。",
            source = "西蒙娜·薇依《恩典与重负》",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 6,
            text = "春天按时到来，保证这颗星球不会死去。",
            source = "史铁生《比如摇滚与写作》",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 7,
            text = "为何为好事泪流？谁能凭爱意要富士山私有。",
            source = "陈奕迅《富士山下》",
            editedOn = DEFAULT_DATE,
        ),
        PaperNote(
            id = 8,
            text = "所有的大人都曾经是小孩，虽然只有少数人记得。",
            source = "《小王子》",
            editedOn = DEFAULT_DATE,
        ),
    )

    /**
     * 日记默认数据（README：仅一条，日期 2026-09-17）。
     */
    val diaryEntries: List<DiaryEntry> = listOf(
        DiaryEntry(
            id = 1,
            date = DEFAULT_DATE,
            title = "今天天气还不错",
            body = "你那里今天如何？",
        ),
    )

    /**
     * 随笔默认数据（README：仅一篇，正文见默认文本的「随笔.md」）。
     */
    val essays: List<Essay> = listOf(
        Essay(
            id = 1,
            date = DEFAULT_DATE,
            title = "欢迎来到星笺",
            body = "这是一个集成了日记、摘抄、随笔的轻量化笔记软件。\n\n" +
                "在首页，你可以查看你摘抄的句子。点击或者滑动切换到下一张。卡片是随机的。\n\n" +
                "下方的底栏可以切换页面。\n\n" +
                "在“时间”，你可以在时间轴旁查看你每一天的日记，点击标题展开正文。" +
                "长按日期打开日历，快速切换到想要看的日期；长按标题选择更多操作，编辑或者删除。\n\n" +
                "在“纸条”，你可以快速阅览你摘抄过的句子。点击卡片，将会展开这一条。\n\n" +
                "在“思索”，你可以查看你曾写下过的随笔。\n\n" +
                "底栏右边的是新建，展开的三个按键由上到下分别是：日记、摘抄、随笔。" +
                "在编辑页里，你也可以长按第一行的类型标签，快速切换编辑的类型。\n\n" +
                "随笔的编辑页支持基础的markdown文本写作，你可以更清晰地写下你心中所想。\n\n" +
                "左上角的按钮是个人主页，点击，你可以在这个页面里修改昵称，查看星尘" +
                "（也就是回收站，你删除的条目都会暂时保存在这里），与设置软件。\n\n" +
                "嗯……现在的功能好像还不是很多。不过我还在持续更新，希望以后会有你想要的功能。\n\n" +
                "如果想要下载最新版，或者反馈你的想法，可以在GitHub上搜索我的仓库：" +
                "https://www.github.com/Lacrimosa0917/Staria 。\n\n" +
                "希望这里，能成为一片你的纸上星空。",
        ),
    )
}
