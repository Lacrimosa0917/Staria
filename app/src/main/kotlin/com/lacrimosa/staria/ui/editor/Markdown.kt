package com.lacrimosa.staria.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/** 支持的标题层级上限。 */
private const val HEADING_MAX_LEVEL = 6

/**
 * 判断一行是几级标题，0 表示不是标题。
 *
 * README：输入 # 号**并空格**后该行才立刻变成标题，
 * 所以标记后面必须紧跟一个空格，否则 `#话题` 这类写法会被误判。
 */
fun headingLevelOf(line: String): Int {
    var hashes = 0
    while (hashes < line.length && hashes < HEADING_MAX_LEVEL && line[hashes] == '#') hashes++
    if (hashes == 0) return 0
    return if (hashes < line.length && line[hashes] == ' ') hashes else 0
}

/** 标题的字号与字重。 */
private fun headingSpan(level: Int): SpanStyle = when (level) {
    1 -> SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
    2 -> SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold)
    3 -> SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium)
    else -> SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium)
}

/** 标题的行高。字号变大后不抬高行高会切到字的上下缘。 */
private fun headingLineHeight(level: Int) = when (level) {
    1 -> 34.sp
    2 -> 30.sp
    else -> 27.sp
}

/**
 * 解析结果。
 *
 * [annotated] 是**去掉标记后**的展示文本（带标题样式），
 * [hidden] 是被隐藏的标记在**原始文本**里的区间，供光标位置映射使用。
 */
internal class MarkdownResult(
    val annotated: AnnotatedString,
    val hidden: List<IntRange>,
)

/**
 * 解析 markdown：目前只处理行首标题（# / ## / ### …）。
 *
 * 逐行重建字符串：把 `#` 与紧随的空格丢掉，只保留可见内容，
 * 同时给该行套上对应层级的字号与行高。
 */
internal fun parseMarkdown(text: String): MarkdownResult {
    val hidden = mutableListOf<IntRange>()
    val spans = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    val paragraphs = mutableListOf<AnnotatedString.Range<ParagraphStyle>>()
    val out = StringBuilder()

    var lineStart = 0
    while (true) {
        val newline = text.indexOf('\n', lineStart)
        val lineEnd = if (newline < 0) text.length else newline
        val line = text.substring(lineStart, lineEnd)

        val level = headingLevelOf(line)
        // 标记 = level 个 # 再跟一个空格
        val markerLength = if (level > 0) level + 1 else 0
        if (markerLength > 0) {
            hidden += lineStart until (lineStart + markerLength)
        }

        val visibleStart = out.length
        out.append(line, markerLength, line.length)
        val visibleEnd = out.length

        // 行尾换行符算在这一行里：段落样式必须覆盖到它，
        // 否则空标题行（只有 "# "、可见内容为空）拿不到行高，
        // 光标会被画到按默认行高计算的位置 —— 看起来就是「跑到下一行」。
        val hasNewline = newline >= 0
        val paraEnd = if (hasNewline) visibleEnd + 1 else visibleEnd

        if (level > 0) {
            // 字号只作用于可见字符，避免影响行尾
            if (visibleEnd > visibleStart) {
                spans += AnnotatedString.Range(headingSpan(level), visibleStart, visibleEnd)
            }
            // 行高作用于整行（含换行符），空标题行也生效
            paragraphs += AnnotatedString.Range(
                ParagraphStyle(lineHeight = headingLineHeight(level), textAlign = TextAlign.Start),
                visibleStart,
                paraEnd,
            )
        }

        if (!hasNewline) break
        out.append('\n')
        lineStart = newline + 1
    }

    return MarkdownResult(
        annotated = AnnotatedString(out.toString(), spans, paragraphs),
        hidden = hidden,
    )
}

/** 只读展示用：把 markdown 渲染成带样式的文本。 */
fun renderMarkdown(text: String): AnnotatedString = parseMarkdown(text).annotated

/**
 * 把「原始文本下标」与「隐藏标记后的展示下标」互相换算。
 *
 * 落进隐藏区的位置统一折叠到该区起点对应的展示位置；
 * 反向换算时则取隐藏区**之后**的第一个字符，
 * 这样点在标题行最左侧输入文字不会插到 `#` 前面把标题破坏掉。
 */
private class MarkerOffsetMapping(
    private val hidden: List<IntRange>,
    private val originalLength: Int,
    private val transformedLength: Int,
) : OffsetMapping {

    override fun originalToTransformed(offset: Int): Int {
        val original = offset.coerceIn(0, originalLength)
        var removed = 0
        for (range in hidden) {
            if (range.first >= original) break
            removed += minOf(range.last + 1, original) - range.first
        }
        return (original - removed).coerceIn(0, transformedLength)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val transformed = offset.coerceIn(0, transformedLength)
        var removed = 0
        for (range in hidden) {
            val length = range.last - range.first + 1
            val collapsedAt = range.first - removed
            when {
                transformed < collapsedAt -> return (transformed + removed).coerceIn(0, originalLength)
                // 折叠点：映射到标记之后（行首可见内容的起点）。
                // 这里必须钳在 originalLength 内，否则空标题行会落到换行符之后，
                // 光标就画到下一行去了。
                transformed == collapsedAt -> return (range.last + 1).coerceIn(0, originalLength)
                else -> removed += length
            }
        }
        return (transformed + removed).coerceIn(0, originalLength)
    }
}

/**
 * 随笔正文的实时预览变形。
 *
 * README：输入 markdown 时实时切换为预览格式，隐藏前面的格式代码。
 * 因此在输入框上套一层 [VisualTransformation]：字形与光标都基于隐藏后的文本布局，
 * 但底层保存的仍是原始 markdown。
 */
class MarkdownTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val parsed = parseMarkdown(text.text)
        // 没有标题时原文即展示文本，直接复用可省掉一次光标映射
        if (parsed.hidden.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        return TransformedText(
            parsed.annotated,
            MarkerOffsetMapping(parsed.hidden, text.text.length, parsed.annotated.length),
        )
    }
}
