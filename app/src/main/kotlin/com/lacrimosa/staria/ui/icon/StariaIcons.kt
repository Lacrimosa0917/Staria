package com.lacrimosa.staria.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * 图标统一规格：24dp 画布、纯描边、墨色线条、圆头圆角、线宽 1.6。
 * README：「图标均使用黑色线条构成的简约标志」。
 *
 * 几何来源分两类：
 * - **ICON.md 素材**（首页底栏四项、新建栏三项、个人主页两项、保存/删除/返回/撤销重做/恢复）：
 *   文档给的是 48 画布 + 4 线宽，按文档「参数仅供参考」的说明，
 *   这里统一按 0.5 等比换算到 24 画布，线宽仍取 1.6 —— 与未替换的图标保持同一笔画粗细。
 * - **项目自绘**：加号、关闭、勾、小铅笔、右尖括号、多选清单、更多、占位。
 *
 * 颜色内部用不透明的 [Color.Black] 占位：`Icon` 的 `tint` 只作用于不透明像素，
 * 所以 path 里必须是不透明色才能被 tint 统一覆盖（`Color.Unspecified` 是透明的，
 * tint 不会生效）。实际显示颜色完全由调用方传 `tint` 决定。
 * 深色模式下由 tint（通常传 [Ink]，即白）统一着色。
 */
private const val STROKE_WIDTH = 1.6f

// ---------- 构建器 ----------

/** 纯描边图标。 */
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
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}.build()

/**
 * 填充态图标（底栏选中项专用）。
 *
 * 文档的「填充」版本是「墨色实心 + 白色描边细节」，但 Compose 的 `Icon(tint = …)`
 * 会给整张图统一上色，双色图标会被压成一片。所以这里把白色细节改成
 * **EvenOdd 挖洞**（把细线加粗成窄条，从实心块里挖掉），
 * 这样既是单色、又能被 tint 着色，细节也不会糊成一团。
 *
 * @param fill 实心主体 + 挖洞（坐标需闭合）
 * @param detail 实心块之外的独立笔画（如沙漏上下两道横线）
 */
private fun buildFilled(
    name: String,
    fill: PathBuilder.() -> Unit,
    detail: (PathBuilder.() -> Unit)? = null,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(
        fill = SolidColor(Color.Black),
        fillAlpha = 1f,
        stroke = null,
        strokeAlpha = 1f,
        strokeLineWidth = 0f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathFillType = PathFillType.EvenOdd,
        pathBuilder = fill,
    )
    if (detail != null) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = STROKE_WIDTH,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = detail,
        )
    }
}.build()

// ---------- 首页底栏：首页 ----------

/** 首页：屋子（ICON.md 线性版）。 */
val IconHome: ImageVector by lazy {
    build("Home") {
        // 屋体
        moveTo(4.5f, 9f)
        lineTo(4.5f, 21f)
        lineTo(19.5f, 21f)
        lineTo(19.5f, 9f)
        lineTo(12f, 3f)
        close()
        // 门
        moveTo(9.5f, 14.5f)
        lineTo(9.5f, 21f)
        lineTo(14.5f, 21f)
        lineTo(14.5f, 14.5f)
        close()
        // 地脚线
        moveTo(4.5f, 21f)
        lineTo(19.5f, 21f)
    }
}

/** 首页（选中）：实心屋子，门挖空。 */
val IconHomeSelected: ImageVector by lazy {
    buildFilled(
        name = "HomeSelected",
        fill = {
            moveTo(4.5f, 9f)
            lineTo(4.5f, 21f)
            lineTo(19.5f, 21f)
            lineTo(19.5f, 9f)
            lineTo(12f, 3f)
            close()
            moveTo(9.5f, 14.5f)
            lineTo(9.5f, 21f)
            lineTo(14.5f, 21f)
            lineTo(14.5f, 14.5f)
            close()
        },
        detail = {
            moveTo(4.5f, 21f)
            lineTo(19.5f, 21f)
        },
    )
}

// ---------- 首页底栏：沙漏 ----------

/** 时间：沙漏（ICON.md 线性版）。 */
val IconHourglass: ImageVector by lazy {
    build("Hourglass") {
        // 上下横档
        moveTo(3.5f, 2f)
        lineTo(20.5f, 2f)
        moveTo(3.5f, 22f)
        lineTo(20.5f, 22f)
        // 下半砂囊
        moveTo(5.5f, 22f)
        curveTo(6.833f, 15.331f, 9f, 11.997f, 12f, 12f)
        curveTo(15f, 12.003f, 17.167f, 15.336f, 18.5f, 22f)
        close()
        // 上半砂囊
        moveTo(18.5f, 2f)
        curveTo(17.167f, 8.669f, 15f, 12.003f, 12f, 12f)
        curveTo(9f, 11.997f, 6.833f, 8.664f, 5.5f, 2f)
        close()
    }
}

/** 时间（选中）：上下砂囊实心，横档仍为线条。 */
val IconHourglassSelected: ImageVector by lazy {
    buildFilled(
        name = "HourglassSelected",
        fill = {
            moveTo(5.5f, 22f)
            curveTo(6.833f, 15.331f, 9f, 11.997f, 12f, 12f)
            curveTo(15f, 12.003f, 17.167f, 15.336f, 18.5f, 22f)
            close()
            moveTo(18.5f, 2f)
            curveTo(17.167f, 8.669f, 15f, 12.003f, 12f, 12f)
            curveTo(9f, 11.997f, 6.833f, 8.664f, 5.5f, 2f)
            close()
        },
        detail = {
            moveTo(3.5f, 2f)
            lineTo(20.5f, 2f)
            moveTo(3.5f, 22f)
            lineTo(20.5f, 22f)
        },
    )
}

// ---------- 首页底栏：纸条 ----------

/** 纸条：一张纸 + 后面斜出的一张（ICON.md 线性版）。 */
val IconPaperStack: ImageVector by lazy {
    build("PaperStack") {
        // 前一张纸
        moveTo(6.5f, 5f)
        lineTo(20.5f, 5f)
        lineTo(20.5f, 22f)
        lineTo(6.5f, 22f)
        close()
        // 后一张纸只露左边与上边
        moveTo(17.5f, 5f)
        lineTo(17.5f, 2f)
        lineTo(4f, 2f)
        curveTo(3.724f, 2f, 3.5f, 2.224f, 3.5f, 2.5f)
        lineTo(3.5f, 19f)
        lineTo(6.5f, 19f)
        // 两道正文线
        moveTo(10.5f, 11f)
        lineTo(16.5f, 11f)
        moveTo(10.5f, 15f)
        lineTo(16.5f, 15f)
    }
}

/** 纸条（选中）：实心纸面，两道正文线挖空。 */
val IconPaperStackSelected: ImageVector by lazy {
    buildFilled(
        name = "PaperStackSelected",
        fill = {
            moveTo(6.5f, 5f)
            lineTo(20.5f, 5f)
            lineTo(20.5f, 22f)
            lineTo(6.5f, 22f)
            close()
            // 正文线挖成槽：10.5..16.5，厚度 2
            moveTo(10.5f, 10f)
            lineTo(16.5f, 10f)
            lineTo(16.5f, 12f)
            lineTo(10.5f, 12f)
            close()
            moveTo(10.5f, 14f)
            lineTo(16.5f, 14f)
            lineTo(16.5f, 16f)
            lineTo(10.5f, 16f)
            close()
        },
        detail = {
            moveTo(17.5f, 5f)
            lineTo(17.5f, 2f)
            lineTo(4f, 2f)
            curveTo(3.724f, 2f, 3.5f, 2.224f, 3.5f, 2.5f)
            lineTo(3.5f, 19f)
            lineTo(6.5f, 19f)
        },
    )
}

// ---------- 首页底栏：思索 ----------

/**
 * 思索：笔记本（ICON.md）。
 *
 * 取代原先的羽毛笔。README 只要求「图标为黑色线条简约标志」，
 * 这里按素材文档换成笔记本。
 */
val IconNotebook: ImageVector by lazy {
    build("Notebook") {
        // 封面
        moveTo(5f, 3f)
        curveTo(5f, 2.448f, 5.448f, 2f, 6f, 2f)
        lineTo(20f, 2f)
        curveTo(20.552f, 2f, 21f, 2.448f, 21f, 3f)
        lineTo(21f, 21f)
        curveTo(21f, 21.552f, 20.552f, 22f, 20f, 22f)
        lineTo(6f, 22f)
        curveTo(5.448f, 22f, 5f, 21.552f, 5f, 21f)
        close()
        // 书脊
        moveTo(17f, 3f)
        lineTo(17f, 21f)
        // 左侧三个装订孔
        moveTo(3f, 7f)
        lineTo(7f, 7f)
        moveTo(3f, 12f)
        lineTo(7f, 12f)
        moveTo(3f, 17f)
        lineTo(7f, 17f)
        // 上下两道书脊压线
        moveTo(13.5f, 2f)
        lineTo(19.5f, 2f)
        moveTo(13.5f, 22f)
        lineTo(19.5f, 22f)
    }
}

/** 思索（选中）：实心封面，书脊挖成一道槽。 */
val IconNotebookSelected: ImageVector by lazy {
    buildFilled(
        name = "NotebookSelected",
        fill = {
            moveTo(5f, 3f)
            curveTo(5f, 2.448f, 5.448f, 2f, 6f, 2f)
            lineTo(20f, 2f)
            curveTo(20.552f, 2f, 21f, 2.448f, 21f, 3f)
            lineTo(21f, 21f)
            curveTo(21f, 21.552f, 20.552f, 22f, 20f, 22f)
            lineTo(6f, 22f)
            curveTo(5.448f, 22f, 5f, 21.552f, 5f, 21f)
            close()
            // 书脊槽：16..18
            moveTo(16f, 3f)
            lineTo(18f, 3f)
            lineTo(18f, 21f)
            lineTo(16f, 21f)
            close()
        },
        detail = {
            moveTo(3f, 7f)
            lineTo(7f, 7f)
            moveTo(3f, 12f)
            lineTo(7f, 12f)
            moveTo(3f, 17f)
            lineTo(7f, 17f)
            moveTo(13.5f, 2f)
            lineTo(19.5f, 2f)
            moveTo(13.5f, 22f)
            lineTo(19.5f, 22f)
        },
    )
}

// ---------- 通用 ----------

/** 新建：加号。 */
val IconPlus: ImageVector by lazy {
    build("Plus") {
        moveTo(12f, 6f)
        lineTo(12f, 18f)
        moveTo(6f, 12f)
        lineTo(18f, 12f)
    }
}

/** 日记：一条起伏的页边（ICON.md 新建日记）。 */
val IconDiary: ImageVector by lazy {
    build("NewDiary") {
        // 笔与纸张的折角
        moveTo(12f, 12f)
        lineTo(12f, 9.5f)
        lineTo(19.5f, 2f)
        lineTo(22f, 4.5f)
        lineTo(14.5f, 12f)
        close()
        // 摊开的纸页
        moveTo(8f, 12f)
        lineTo(4.5f, 12f)
        curveTo(3.119f, 12f, 2f, 13.119f, 2f, 14.5f)
        curveTo(2f, 15.881f, 3.119f, 17f, 4.5f, 17f)
        lineTo(19.5f, 17f)
        curveTo(20.881f, 17f, 22f, 18.119f, 22f, 19.5f)
        curveTo(22f, 20.881f, 20.881f, 22f, 19.5f, 22f)
        lineTo(9f, 22f)
    }
}

/** 摘录：三条错开的横线（ICON.md 新建摘录）。 */
val IconExcerpt: ImageVector by lazy {
    build("NewExcerpt") {
        moveTo(7f, 7f)
        lineTo(21f, 7f)
        moveTo(5f, 12f)
        lineTo(19f, 12f)
        moveTo(3f, 17f)
        lineTo(17f, 17f)
    }
}

/** 随笔：两圈叠在一起的云（ICON.md 新建思索）。 */
val IconEssay: ImageVector by lazy {
    build("NewEssay") {
        moveTo(15f, 9.5f)
        lineTo(10f, 9.5f)
        curveTo(7.791f, 9.5f, 6f, 11.291f, 6f, 13.5f)
        curveTo(6f, 15.709f, 7.791f, 17.5f, 10f, 17.5f)
        lineTo(18f, 17.5f)
        curveTo(20.209f, 17.5f, 22f, 15.709f, 22f, 13.5f)
        curveTo(22f, 12.486f, 21.622f, 11.559f, 21f, 10.854f)

        moveTo(3f, 12.146f)
        curveTo(2.378f, 11.441f, 2f, 10.514f, 2f, 9.5f)
        curveTo(2f, 7.291f, 3.791f, 5.5f, 6f, 5.5f)
        lineTo(14f, 5.5f)
        curveTo(16.209f, 5.5f, 18f, 7.291f, 18f, 9.5f)
        curveTo(18f, 11.709f, 16.209f, 13.5f, 14f, 13.5f)
        lineTo(9f, 13.5f)
    }
}

// ---------- 个人主页 ----------

/** 星尘：八边星章 + 一道横线（ICON.md）。 */
val IconStarDust: ImageVector by lazy {
    build("StarDust") {
        moveTo(12.002f, 2f)
        lineTo(14.637f, 4.635f)
        lineTo(19.365f, 4.635f)
        lineTo(19.365f, 9.363f)
        lineTo(22f, 11.998f)
        lineTo(19.365f, 14.637f)
        lineTo(19.365f, 19.365f)
        lineTo(14.637f, 19.365f)
        lineTo(12.002f, 22f)
        lineTo(9.363f, 19.365f)
        lineTo(4.635f, 19.365f)
        lineTo(4.635f, 14.637f)
        lineTo(2f, 11.998f)
        lineTo(4.635f, 9.363f)
        lineTo(4.635f, 4.635f)
        lineTo(9.363f, 4.635f)
        close()
        moveTo(8.5f, 12f)
        lineTo(15.5f, 12f)
    }
}

/** 设置：六边花盘 + 中心圆（ICON.md）。 */
val IconSettings: ImageVector by lazy {
    build("Settings") {
        moveTo(17f, 20.5f)
        lineTo(22f, 12f)
        lineTo(17f, 3.5f)
        lineTo(7f, 3.5f)
        lineTo(2f, 12f)
        lineTo(7f, 20.5f)
        close()
        moveTo(12f, 14.5f)
        curveTo(13.381f, 14.5f, 14.5f, 13.381f, 14.5f, 12f)
        curveTo(14.5f, 10.619f, 13.381f, 9.5f, 12f, 9.5f)
        curveTo(10.619f, 9.5f, 9.5f, 10.619f, 9.5f, 12f)
        curveTo(9.5f, 13.381f, 10.619f, 14.5f, 12f, 14.5f)
        close()
    }
}

// ---------- 杂项 ----------

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

/** 右尖括号：列表项进入提示、日历下一月。 */
val IconChevronRight: ImageVector by lazy {
    build("ChevronRight") {
        moveTo(9.5f, 5.5f)
        lineTo(16f, 12f)
        lineTo(9.5f, 18.5f)
    }
}

/** 左尖括号：日历上一月。 */
val IconChevronLeft: ImageVector by lazy {
    build("ChevronLeft") {
        moveTo(14.5f, 5.5f)
        lineTo(8f, 12f)
        lineTo(14.5f, 18.5f)
    }
}

/**
 * 返回：左尖括号（ICON.md）。
 *
 * 与 [IconChevronLeft] 分开：素材给的返回箭头更窄更长，
 * 用在二级页 38dp 圆钮里比日历上的月份切换更合比例。
 */
val IconBack: ImageVector by lazy {
    build("Back") {
        moveTo(15.5f, 18f)
        lineTo(9.5f, 12f)
        lineTo(15.5f, 6f)
    }
}

/** 删除：垃圾桶（ICON.md）。 */
val IconTrash: ImageVector by lazy {
    build("Trash") {
        // 提手
        moveTo(7.5f, 6f)
        lineTo(8.1f, 2.5f)
        lineTo(15.9f, 2.5f)
        lineTo(16.5f, 6f)
        // 盖
        moveTo(3f, 6f)
        lineTo(21f, 6f)
        // 桶身
        moveTo(18.5f, 6f)
        lineTo(17.5f, 21.5f)
        lineTo(6.5f, 21.5f)
        lineTo(5.5f, 6f)
        close()
        // 桶内一道横线
        moveTo(9.5f, 17.5f)
        lineTo(14.5f, 17.5f)
    }
}

/** 多选：清单 + 勾。 */
val IconChecklist: ImageVector by lazy {
    build("Checklist") {
        moveTo(4.2f, 6.4f)
        lineTo(6f, 6.4f)
        moveTo(4.2f, 12f)
        lineTo(6f, 12f)
        moveTo(4.2f, 17.6f)
        lineTo(6f, 17.6f)

        moveTo(9.4f, 6.4f)
        lineTo(19.4f, 6.4f)
        moveTo(9.4f, 12f)
        lineTo(19.4f, 12f)
        moveTo(9.4f, 17.6f)
        lineTo(15.4f, 17.6f)
    }
}

/** 撤回：向左回卷的箭头（ICON.md 撤销）。 */
val IconUndo: ImageVector by lazy {
    build("Undo") {
        moveTo(6.5f, 4f)
        lineTo(3f, 7f)
        lineTo(6.5f, 10.5f)

        moveTo(3f, 7f)
        lineTo(14.497f, 7f)
        curveTo(17.938f, 7f, 20.861f, 9.81f, 20.995f, 13.25f)
        curveTo(21.137f, 16.885f, 18.134f, 20f, 14.497f, 20f)
        lineTo(6f, 20f)
    }
}

/** 重做：撤回的镜像（ICON.md 重做）。 */
val IconRedo: ImageVector by lazy {
    build("Redo") {
        moveTo(18f, 3.5f)
        lineTo(21.5f, 6.731f)
        lineTo(18f, 10.5f)

        moveTo(20f, 7f)
        lineTo(8.503f, 7f)
        curveTo(5.062f, 7f, 2.139f, 9.81f, 2.005f, 13.25f)
        curveTo(1.863f, 16.885f, 4.866f, 20f, 8.503f, 20f)
        lineTo(17f, 20f)
    }
}

/** 恢复：环形箭头（ICON.md 恢复）。星尘页的恢复操作专用。 */
val IconRestore: ImageVector by lazy {
    build("Restore") {
        moveTo(5.636f, 18.364f)
        curveTo(7.265f, 19.993f, 9.515f, 21f, 12f, 21f)
        curveTo(16.971f, 21f, 21f, 16.971f, 21f, 12f)
        curveTo(21f, 7.029f, 16.971f, 3f, 12f, 3f)
        curveTo(9.515f, 3f, 7.265f, 4.007f, 5.636f, 5.636f)
        curveTo(4.807f, 6.465f, 3f, 8.5f, 3f, 8.5f)

        moveTo(3f, 4.5f)
        lineTo(3f, 8.5f)
        lineTo(7f, 8.5f)
    }
}

/** 保存：软盘（ICON.md）。 */
val IconSave: ImageVector by lazy {
    build("Save") {
        // 盘体
        moveTo(3f, 4.5f)
        curveTo(3f, 3.672f, 3.672f, 3f, 4.5f, 3f)
        lineTo(17.141f, 3f)
        lineTo(21f, 6.603f)
        lineTo(21f, 19.5f)
        curveTo(21f, 20.328f, 20.328f, 21f, 19.5f, 21f)
        lineTo(4.5f, 21f)
        curveTo(3.672f, 21f, 3f, 20.328f, 3f, 19.5f)
        close()
        // 上方的写保护滑片
        moveTo(12.004f, 3f)
        lineTo(12f, 6.692f)
        curveTo(12f, 6.862f, 11.776f, 7f, 11.5f, 7f)
        lineTo(7.5f, 7f)
        curveTo(7.224f, 7f, 7f, 6.862f, 7f, 6.692f)
        lineTo(7f, 3f)
        close()
        // 顶部折线
        moveTo(4.5f, 3f)
        lineTo(17.141f, 3f)
        // 标签两道线
        moveTo(7f, 13f)
        lineTo(17f, 13f)
        moveTo(7f, 17f)
        lineTo(12.004f, 17f)
    }
}

/** 关闭：叉。编辑页的退出按键使用。 */
val IconClose: ImageVector by lazy {
    build("Close") {
        moveTo(6.6f, 6.6f)
        lineTo(17.4f, 17.4f)
        moveTo(17.4f, 6.6f)
        lineTo(6.6f, 17.4f)
    }
}

/** 勾：确认 / 已选中。 */
val IconCheck: ImageVector by lazy {
    build("Check") {
        moveTo(5.2f, 12.6f)
        lineTo(9.8f, 17.2f)
        lineTo(18.8f, 6.8f)
    }
}

/**
 * 空白占位图标。
 *
 * tooltip 的每一项都必须带图标才能保持文字左对齐，
 * 未选中项用这个只有一段不可见路径的图标占据同样的宽度。
 */
val IconDotPlaceholder: ImageVector by lazy {
    ImageVector.Builder(
        name = "DotPlaceholder",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = null,
            strokeLineWidth = 0f,
            pathBuilder = {
                moveTo(12f, 12f)
                lineTo(12f, 12f)
            },
        )
    }.build()
}

/**
 * 更多：三个圆点。
 *
 * 用线段无法得到圆点（描边线宽只有 1.6），所以这里直接画三条极短的
 * 圆头线段，并给该图标单独放大描边宽度，视觉上就是三个实心点。
 */
val IconMore: ImageVector by lazy {
    ImageVector.Builder(
        name = "More",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        listOf(6f, 12f, 18f).forEach { cx ->
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                pathBuilder = {
                    moveTo(cx, 12f)
                    lineTo(cx + 0.1f, 12f)
                },
            )
        }
    }.build()
}
