package com.lacrimosa.staria.data

import android.content.Context

/**
 * 主题色。
 *
 * README：暂定八种，初始默认为简约白。
 * [argb] 是预览色块与后续着色使用的实际颜色；
 * 简约白没有给色值，取纯白（与全局背景一致）。
 */
enum class ThemeColor(val label: String, val argb: Long) {
    Plain("简约白", 0xFFFFFFFF),
    MistBlue("迷雾蓝", 0xFF7BA7D9),
    MoonCyan("月白青", 0xFF8FB8C9),
    CamelliaRed("山茶红", 0xFFED656A),
    ApricotYellow("熟杏黄", 0xFFF8BC31),
    ScallionGreen("浅葱绿", 0xFF40A070),
    SakuraPink("樱花粉", 0xFFF596AA),
    VinePurple("红藤紫", 0xFFB481BB),
}

/** 深色模式的三档。 */
enum class DarkMode(val label: String) {
    FollowSystem("跟随系统"),
    On("打开"),
    Off("关闭"),
}

/**
 * 设置项存储。
 *
 * README 说明设置页目前只展示交互按键、不接实际功能，
 * 但选择结果仍然落盘，这样重进页面能看到自己选过的值，交互才是闭环的。
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadThemeColor(): ThemeColor {
        val name = prefs.getString(KEY_THEME_COLOR, null) ?: return ThemeColor.Plain
        return ThemeColor.entries.firstOrNull { it.name == name } ?: ThemeColor.Plain
    }

    fun saveThemeColor(color: ThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
    }

    fun loadDarkMode(): DarkMode {
        val name = prefs.getString(KEY_DARK_MODE, null) ?: return DarkMode.FollowSystem
        return DarkMode.entries.firstOrNull { it.name == name } ?: DarkMode.FollowSystem
    }

    fun saveDarkMode(mode: DarkMode) {
        prefs.edit().putString(KEY_DARK_MODE, mode.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "staria_settings"
        const val KEY_THEME_COLOR = "theme_color"
        const val KEY_DARK_MODE = "dark_mode"
    }
}
