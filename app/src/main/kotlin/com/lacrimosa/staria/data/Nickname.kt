package com.lacrimosa.staria.data

import android.content.Context

/**
 * 昵称长度约束。
 *
 * README：「昵称的字数不大于 8 个汉字，3 个英文或数字字符视为一个汉字。」
 *
 * 实现上把「一个汉字」作为 3 个权重单位：
 * - 汉字 / 全角字符 / 表情等：3 单位
 * - 英文、数字（ASCII 字母与数字）：1 单位
 * 于是 8 个汉字 = 24 单位 = 24 个英文字母，恰好等价。
 */
const val NICKNAME_MAX_WEIGHT = 24

/** 单个码点的权重。 */
private fun weightOf(codePoint: Int): Int {
    val isAsciiAlnum = codePoint < 128 &&
        (Character.isLetterOrDigit(codePoint))
    return if (isAsciiAlnum) 1 else 3
}

/** 计算昵称占用的权重，用于判断是否超出上限。 */
fun nicknameWeight(text: String): Int {
    var sum = 0
    var i = 0
    while (i < text.length) {
        val cp = text.codePointAt(i)
        sum += weightOf(cp)
        i += Character.charCount(cp)
    }
    return sum
}

/**
 * 昵称存储。
 *
 * 只有一项数据，用 SharedPreferences 即可；等接入 Room 后再统一迁移。
 */
class NicknameStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): String = prefs.getString(KEY_NICKNAME, null)
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_NICKNAME

    fun save(nickname: String) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
    }

    private companion object {
        const val PREFS_NAME = "staria_profile"
        const val KEY_NICKNAME = "nickname"
        const val DEFAULT_NICKNAME = "沈星回"
    }
}
