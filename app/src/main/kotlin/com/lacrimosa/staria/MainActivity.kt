package com.lacrimosa.staria

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.lacrimosa.staria.data.DarkMode
import com.lacrimosa.staria.data.SettingsStore
import com.lacrimosa.staria.data.ThemeColor
import com.lacrimosa.staria.ui.StariaApp
import com.lacrimosa.staria.ui.StariaSplash
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.StariaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val settingsStore = remember { SettingsStore(context) }

            // 深色模式与主题色提升到这里：StariaTheme 需要 darkMode 决定整套配色，
            // StariaApp 需要 themeColor 下发着色位置，两者共享同一份状态。
            var themeColor by remember { mutableStateOf(settingsStore.loadThemeColor()) }
            var darkMode by remember { mutableStateOf(settingsStore.loadDarkMode()) }

            StariaTheme(darkMode = darkMode) {
                // 开屏动画盖在 StariaApp 之上，而不是「先动画、后加载」：
                // 这样首帧渲染与数据初始化在动画播放期间就已完成，
                // 动画淡出时下面已经是可交互的完整界面，不会出现二次白屏。
                var splashVisible by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // 底色与开屏、页面一致，任何一层淡出都不会露出白底
                        .background(PageBackground),
                ) {
                    StariaApp(
                        themeColor = themeColor,
                        darkMode = darkMode,
                        onThemeColorChange = { color ->
                            themeColor = color
                            settingsStore.saveThemeColor(color)
                        },
                        onDarkModeChange = { mode ->
                            darkMode = mode
                            settingsStore.saveDarkMode(mode)
                        },
                    )

                    AnimatedVisibility(
                        visible = splashVisible,
                        exit = fadeOut(tween(durationMillis = 0)),
                    ) {
                        StariaSplash(onFinished = { splashVisible = false })
                    }
                }
            }
        }
    }
}
