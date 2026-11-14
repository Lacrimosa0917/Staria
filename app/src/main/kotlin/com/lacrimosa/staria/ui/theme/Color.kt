package com.lacrimosa.staria.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 全局颜色令牌。
 *
 * 项目原先把这套颜色以 `private val Hairline = Color(0xFFE6E2DA)` 的形式
 * 复制在十多个文件里（实测 31 处字面量），改一处要动十几个文件。
 * 现在全部收进这里，其它文件只允许引用语义名，不再出现 `Color(0xFF…)`。
 *
 * 命名约定：
 * - `Paper*` 是面（背景、卡面）
 * - `Ink*` 是墨（正文、图标）
 * - `Border*` 是线（描边，按使用场景分档）
 * - 其余按用途命名
 */

// ---------- 面 ----------

/** 全局纯白背景（README：全局背景采用白色）。 */
val PaperWhite = Color(0xFFFFFFFF)

/**
 * 卡面：与图标母版底色一致（#FCFBF9），比纯白背景略暖一丝，
 * 让纸条卡片在白色背景上仍有边界感。
 */
val CardPaper = Color(0xFFFCFBF9)

// ---------- 墨 ----------

/** 墨色：正文与图标统一使用近黑，避免纯黑在 OLED 上过硬。 */
val Ink = Color(0xFF1A1A1A)

/** 次级文字（日期、出处、说明）。 */
val InkMuted = Color(0xFF9A9A9A)

// ---------- 线 ----------

/**
 * 分隔线。
 *
 * 注意：这是**内容分隔线**（日期下方那条、抽屉里两条入口之间那条），
 * 不要拿它当卡片描边——卡片描边用 [Border]，两者差一个色阶。
 */
val HairLine = Color(0xFFEDEDED)

/**
 * 标准描边：卡片、胶囊底栏、圆钮、tooltip、弹窗。
 *
 * 全项目原先有 14 处各写一遍 `Color(0xFFE6E2DA)`，这是唯一来源。
 */
val Border = Color(0xFFE6E2DA)

/**
 * 纸面描边：比 [Border] 再暖一点，只用于「纸」这一族（首页卡片堆叠、纸条展开浮层），
 * 让纸片在纯白背景上更像一张单独的纸而不是一个卡片控件。
 */
val BorderOnCard = Color(0xFFE8E4DC)

/** 多选勾选框的未选中描边。比常规描边深，因为它在白底上要能被看见。 */
val BorderSelected = Color(0xFFC9C3B8)

/** 禁用态的填充色（禁用主按钮、禁用的批量删除键）。 */
val BorderDisabled = Color(0xFFD8D4CC)

// ---------- 功能色 ----------

/**
 * 删除 / 危险操作的红。
 *
 * 项目原先有两个红：`0xFFB3261E`（tooltip、随笔查看页、删除确认）与
 * `0xFFD93A3A`（昵称浮窗超限提示）。统一取前者——它更沉，
 * 符合整体克制的气质。
 */
val Danger = Color(0xFFB3261E)

// ---------- 局部面 ----------

/** 时间轴竖线与圆点之间的连线。 */
val Axis = Color(0xFFD9D4CA)

/** 输入框底色（昵称浮窗）。 */
val InputBackground = Color(0xFFFAF8F5)

/** 小圆钮底色（日历月份切换的左右箭头）。 */
val ChipBackground = Color(0xFFF3F0EA)

// ---------- 遮罩 ----------

/**
 * 弹窗 / 展开浮层的压暗遮罩。
 *
 * 用常量而不是各处写 `Color.Black.copy(alpha = 0.32f)`，
 * 保证所有浮层的压暗程度一致。
 */
val Scrim = Color.Black.copy(alpha = 0.32f)
