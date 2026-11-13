package com.lacrimosa.stario.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 图标统一规格：24dp 画布、纯描边、黑色线条、圆头圆角。
 * README：「图标均使用黑色线条构成的简约标志」。
 */
private const val INK = 0xFF1A1A1A
private const val STROKE_WIDTH = 1.6f

private fun build(
    name: String,
    block: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = null,
        stroke = SolidColor(Color(INK)),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}.build()

/**
 * 首页：四角星芒。
 *
 * README 未指定首页图标；这里用星芒呼应应用名「星笺」，
 * 同时与「日记」的本子图标在轮廓上明显区分（避免两个都是方框加横线）。
 */
val IconHome: ImageVector by lazy {
    build("StarSparkle") {
        // 主星：上下左右四个尖角
        moveTo(12f, 3.2f)
        curveTo(12.9f, 8.6f, 15.4f, 11.1f, 20.8f, 12f)
        curveTo(15.4f, 12.9f, 12.9f, 15.4f, 12f, 20.8f)
        curveTo(11.1f, 15.4f, 8.6f, 12.9f, 3.2f, 12f)
        curveTo(8.6f, 11.1f, 11.1f, 8.6f, 12f, 3.2f)
        close()
    }
}

/** 时间：沙漏。 */
val IconHourglass: ImageVector by lazy {
    build("Hourglass") {
        moveTo(6.5f, 3.5f)
        lineTo(17.5f, 3.5f)
        moveTo(6.5f, 20.5f)
        lineTo(17.5f, 20.5f)

        moveTo(7.6f, 3.5f)
        lineTo(7.6f, 7.2f)
        curveTo(7.6f, 9.4f, 9.7f, 10.4f, 10.7f, 12f)
        curveTo(9.7f, 13.6f, 7.6f, 14.6f, 7.6f, 16.8f)
        lineTo(7.6f, 20.5f)

        moveTo(16.4f, 3.5f)
        lineTo(16.4f, 7.2f)
        curveTo(16.4f, 9.4f, 14.3f, 10.4f, 13.3f, 12f)
        curveTo(14.3f, 13.6f, 16.4f, 14.6f, 16.4f, 16.8f)
        lineTo(16.4f, 20.5f)
    }
}

/** 纸条：三张微微错开的纸条叠放。 */
val IconPaperStack: ImageVector by lazy {
    build("PaperStack") {
        // 最后一张：只露左、下两条边
        moveTo(4.5f, 7.4f)
        lineTo(4.5f, 18.6f)
        lineTo(19.4f, 18.6f)
        // 中间一张：只露左、上两条边
        moveTo(7.2f, 4.9f)
        lineTo(7.2f, 16.1f)
        lineTo(16.9f, 16.1f)
        // 最前一张：完整轮廓
        moveTo(9.5f, 6.6f)
        lineTo(18.4f, 6.6f)
        lineTo(18.4f, 14.2f)
        lineTo(9.5f, 14.2f)
        close()
    }
}

/**
 * 思索：羽毛笔。
 *
 * 羽身是一条沿对角线展开的叶形轮廓，笔杆从羽身下端伸出画到左下角，
 * 小尺寸下也能一眼认出是羽毛而不是一条斜线。
 */
val IconQuill: ImageVector by lazy {
    build("Quill") {
        // 羽身
        moveTo(19.6f, 4.4f)
        curveTo(20.4f, 11.6f, 16.4f, 15.4f, 12.2f, 15.6f)
        curveTo(9.4f, 15.7f, 7.6f, 14.2f, 8.2f, 11.8f)
        curveTo(9.2f, 8.0f, 14.0f, 5.2f, 19.6f, 4.4f)
        close()
        // 笔杆
        moveTo(16.6f, 7.4f)
        lineTo(4.4f, 19.6f)
        // 两道羽枝
        moveTo(13.6f, 8.6f)
        curveTo(11.6f, 9.4f, 10.4f, 10.8f, 10.0f, 12.6f)
        moveTo(16.4f, 11.0f)
        curveTo(14.0f, 11.6f, 12.6f, 12.8f, 12.2f, 14.4f)
    }
}

/** 新建：加号。 */
val IconPlus: ImageVector by lazy {
    build("Plus") {
        moveTo(12f, 6f)
        lineTo(12f, 18f)
        moveTo(6f, 12f)
        lineTo(18f, 12f)
    }
}

/** 日记：带书脊的本子（与首页的星芒区分开）。 */
val IconDiary: ImageVector by lazy {
    build("Notebook") {
        // 书脊
        moveTo(9.2f, 4.2f)
        lineTo(9.2f, 19.8f)
        // 封面
        moveTo(9.2f, 4.2f)
        lineTo(18.2f, 4.2f)
        lineTo(18.2f, 19.8f)
        lineTo(9.2f, 19.8f)
        // 内页横线
        moveTo(12.4f, 8.6f)
        lineTo(15.6f, 8.6f)
        moveTo(12.4f, 12f)
        lineTo(15.6f, 12f)
        moveTo(12.4f, 15.4f)
        lineTo(15.6f, 15.4f)
    }
}

/** 摘录：一对引号。 */
val IconQuote: ImageVector by lazy {
    build("Quote") {
        // 左引号
        moveTo(9.8f, 7.4f)
        curveTo(7.5f, 8.2f, 6.3f, 9.9f, 6.3f, 12.4f)
        lineTo(6.3f, 16.6f)
        lineTo(10.1f, 16.6f)
        lineTo(10.1f, 12.7f)
        lineTo(8.1f, 12.7f)
        // 右引号
        moveTo(17.7f, 7.4f)
        curveTo(15.4f, 8.2f, 14.2f, 9.9f, 14.2f, 12.4f)
        lineTo(14.2f, 16.6f)
        lineTo(18f, 16.6f)
        lineTo(18f, 12.7f)
        lineTo(16f, 12.7f)
    }
}

/** 随笔：斜握的一支笔。 */
val IconEssay: ImageVector by lazy {
    build("Pencil") {
        // 笔身
        moveTo(4.4f, 19.6f)
        lineTo(4.4f, 16.2f)
        lineTo(16.4f, 4.2f)
        lineTo(19.8f, 7.6f)
        lineTo(7.8f, 19.6f)
        close()
        // 笔尖分界
        moveTo(14.6f, 6f)
        lineTo(18f, 9.4f)
    }
}

/** 小铅笔：昵称可编辑的提示。 */
val IconPencilSmall: ImageVector by lazy {
    build("PencilSmall") {
        moveTo(5.2f, 18.8f)
        lineTo(5.2f, 15.6f)
        lineTo(15.8f, 5f)
        lineTo(19f, 8.2f)
        lineTo(8.4f, 18.8f)
        close()
        moveTo(14f, 6.8f)
        lineTo(17.2f, 10f)
    }
}

/** 右尖括号：列表项进入提示。 */
val IconChevronRight: ImageVector by lazy {
    build("ChevronRight") {
        moveTo(9.5f, 5.5f)
        lineTo(16f, 12f)
        lineTo(9.5f, 18.5f)
    }
}
