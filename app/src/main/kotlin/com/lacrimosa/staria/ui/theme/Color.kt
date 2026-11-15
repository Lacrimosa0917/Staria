package com.lacrimosa.staria.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.lacrimosa.staria.data.ThemeColor

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
 *
 * ## 深色模式
 *
 * 所有颜色令牌都是 [Composable] 属性，实际取值来自 [LocalStariaColors]：
 * 浅色走 [LightStariaColors]、深色走 [DarkStariaColors]。
 * 这样切深色模式时，全项目引用这些令牌的 224 处会一起随组合重算，
 * 不需要逐个调用点传参。
 *
 * ⚠ 因为令牌是 Composable 属性，**不能在非 Composable 上下文里读取**：
 * 例如图标构建器、backdrop 的 `drawRect`、`Canvas` 绘制块等。
 * 那些地方需要在 Composable 作用域先把颜色取出来、再闭包捕获。
 */

/** 一套完整语义色。浅色与深色各一份。 */
data class StariaColors(
    val pageBackground: Color,
    val paperWhite: Color,
    val cardPaper: Color,
    val ink: Color,
    val inkMuted: Color,
    val hairLine: Color,
    val border: Color,
    val borderOnCard: Color,
    val borderSelected: Color,
    val borderDisabled: Color,
    val danger: Color,
    val axis: Color,
    val inputBackground: Color,
    val chipBackground: Color,
    val scrim: Color,
)

/** 浅色（默认）配色。 */
val LightStariaColors = StariaColors(
    pageBackground = Color(0xFFF5F5F5),
    paperWhite = Color(0xFFFFFFFF),
    cardPaper = Color(0xFFFCFBF9),
    ink = Color(0xFF1A1A1A),
    inkMuted = Color(0xFF9A9A9A),
    hairLine = Color(0xFFEDEDED),
    border = Color(0xFFE6E2DA),
    borderOnCard = Color(0xFFE8E4DC),
    borderSelected = Color(0xFFC9C3B8),
    borderDisabled = Color(0xFFD8D4CC),
    danger = Color(0xFFB3261E),
    axis = Color(0xFFD9D4CA),
    inputBackground = Color(0xFFFAF8F5),
    chipBackground = Color(0xFFF3F0EA),
    scrim = Color.Black.copy(alpha = 0.32f),
)

/**
 * 深色配色。
 *
 * 用户约定：
 * - 全局背景 #121212
 * - 卡片界面 #2C2C2C
 * - 黑色图标 / 正文统一替换为白
 * - 主题色（八种强调色）不做改动
 *
 * 其余浅色系的「线 / 分隔 / 时间轴 / 输入底 / 小圆钮」按同一暗色阶推导，
 * 否则深色背景下会出现刺眼的白条。
 */
val DarkStariaColors = StariaColors(
    pageBackground = Color(0xFF121212),
    paperWhite = Color(0xFF2C2C2C),
    cardPaper = Color(0xFF2C2C2C),
    ink = Color(0xFFFFFFFF),
    inkMuted = Color(0xFFA6A6A6),
    hairLine = Color(0xFF3A3A3A),
    border = Color(0xFF3A3A3A),
    borderOnCard = Color(0xFF3A3A3A),
    borderSelected = Color(0xFF8A8A8A),
    borderDisabled = Color(0xFF4A4A4A),
    danger = Color(0xFFB3261E),
    axis = Color(0xFF4A4A4A),
    inputBackground = Color(0xFF1E1E1E),
    chipBackground = Color(0xFF333333),
    scrim = Color.Black.copy(alpha = 0.32f),
)

/** 当前配色，由根布局按深色模式开关提供。 */
val LocalStariaColors = staticCompositionLocalOf { LightStariaColors }

// ---------- 面 ----------

/** 页面底色。深色模式为 #121212。 */
val PageBackground: Color
    @Composable get() = LocalStariaColors.current.pageBackground

/** 纯白：玻璃面板、弹窗、卡片、按钮的「纸面本色」。深色模式为 #2C2C2C。 */
val PaperWhite: Color
    @Composable get() = LocalStariaColors.current.paperWhite

/** 卡面：比页面底色略暖一丝。深色模式同卡面 #2C2C2C。 */
val CardPaper: Color
    @Composable get() = LocalStariaColors.current.cardPaper

// ---------- 墨 ----------

/** 墨色：正文与图标。深色模式为白。 */
val Ink: Color
    @Composable get() = LocalStariaColors.current.ink

/** 次级文字（日期、出处、说明）。 */
val InkMuted: Color
    @Composable get() = LocalStariaColors.current.inkMuted

// ---------- 线 ----------

/** 分隔线。 */
val HairLine: Color
    @Composable get() = LocalStariaColors.current.hairLine

/** 标准描边：卡片、胶囊底栏、圆钮、tooltip、弹窗。 */
val Border: Color
    @Composable get() = LocalStariaColors.current.border

/** 纸面描边：比 [Border] 再暖一点，用于「纸」这一族。 */
val BorderOnCard: Color
    @Composable get() = LocalStariaColors.current.borderOnCard

/** 多选勾选框的未选中描边。 */
val BorderSelected: Color
    @Composable get() = LocalStariaColors.current.borderSelected

/** 禁用态的填充色。 */
val BorderDisabled: Color
    @Composable get() = LocalStariaColors.current.borderDisabled

// ---------- 功能色 ----------

/** 删除 / 危险操作的红。 */
val Danger: Color
    @Composable get() = LocalStariaColors.current.danger

// ---------- 局部面 ----------

/** 时间轴竖线与圆点之间的连线。 */
val Axis: Color
    @Composable get() = LocalStariaColors.current.axis

/** 输入框底色（昵称浮窗）。 */
val InputBackground: Color
    @Composable get() = LocalStariaColors.current.inputBackground

/** 小圆钮底色（日历月份切换的左右箭头）。 */
val ChipBackground: Color
    @Composable get() = LocalStariaColors.current.chipBackground

// ---------- 遮罩 ----------

/** 弹窗 / 展开浮层的压暗遮罩。 */
val Scrim: Color
    @Composable get() = LocalStariaColors.current.scrim

// ---------- 主题色 ----------

/**
 * 当前选中的主题色，由根布局读设置后下发。
 *
 * 默认 [ThemeColor.Plain]（简约白）。约定：简约白**不引入彩色**，着色位置
 * 保持原有的墨色 [Ink]，所以默认情况下界面与未接主题功能时完全一致。
 */
val LocalThemeColor = compositionLocalOf { ThemeColor.Plain }

/**
 * 主题色在「着色位置」的实际颜色。
 *
 * 简约白回落到 [Ink]（深色模式下为白）；其余主题色取其 [ThemeColor.argb]。
 * 着色位置只有三处：底栏选中项的图标与文字、编辑页保存键、首页 slogan。
 *
 * 主题色的八种强调色在深浅两种模式下都不变，只有「简约白」随 [Ink] 走。
 */
@Composable
fun ThemeColor.accent(): Color =
    if (this == ThemeColor.Plain) Ink else Color(argb.toInt())
