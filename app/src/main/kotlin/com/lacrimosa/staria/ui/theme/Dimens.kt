package com.lacrimosa.staria.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全局尺寸令牌。
 *
 * 原先这些数值在各页面里各写一遍（`ButtonSize = 38.dp` 出现 4 次、
 * 选中框 `17.dp / 4.dp` 拼了 5 遍、卡片圆角 14dp 散在 6 处）。
 * 收进这里之后，调一次全局生效。
 *
 * 底栏的左右边距 [com.lacrimosa.staria.ui.component.BarSidePadding] 不在这里：
 * 它是全项目左右边距的锚点，时间轴对齐底栏首键也依赖它，留在底栏文件内。
 */

/** 二级页（随笔查看 / 编辑 / 星尘 / 设置）的圆形按键直径。 */
val SecondaryButtonSize = 38.dp

/** 二级页顶栏里，圆钮下方内容区的起始留白（圆钮直径 + 呼吸间距）。 */
val SecondaryContentTopPad = SecondaryButtonSize + 28.dp

/** 二级页顶栏自身的上边距。 */
val SecondaryTopBarPad = 14.dp

/** 多选勾选框的边长。 */
val SelectionBoxSize = 17.dp

/** 多选勾选框的圆角。 */
val SelectionBoxRadius = 4.dp

/** 进入多选时，卡片内容右移的距离（为左侧勾选框让位）。 */
val SelectionShift = 26.dp

/** 卡片圆角（纸条 / 思索 / 星尘 / 卡片堆叠 / 日记展开卡片）。 */
val CardRadius = 14.dp

/** 卡片之间的纵向间距。 */
val CardGap = 12.dp

/** 分组面板圆角（个人主页统计卡片、设置页两张卡片）。 */
val PanelRadius = 16.dp

/** 弹窗圆角（未保存确认、删除确认、日历、批量操作确认、昵称浮窗）。 */
val DialogRadius = 18.dp

/** 弹窗内按钮圆角。 */
val ActionRadius = 9.dp

/** 浮层圆角（长按 tooltip、随笔查看页的更多菜单）。 */
val PopupRadius = 12.dp
