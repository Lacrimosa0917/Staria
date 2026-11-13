package com.lacrimosa.stario.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lacrimosa.stario.data.NicknameStore
import com.lacrimosa.stario.data.SampleData
import com.lacrimosa.stario.ui.component.CreateKind
import com.lacrimosa.stario.ui.component.StarioBottomBar
import com.lacrimosa.stario.ui.component.StarioTab
import com.lacrimosa.stario.ui.home.HomeScreen
import com.lacrimosa.stario.ui.placeholder.PlaceholderScreen
import com.lacrimosa.stario.ui.profile.NicknameDialog
import com.lacrimosa.stario.ui.profile.ProfileDrawer
import com.lacrimosa.stario.ui.theme.PaperWhite

/**
 * 应用根布局。
 *
 * 结构（README）：
 * - 四个页面共用同一个底部区域：胶囊底栏 + 新建按钮，切页时不消失
 * - 左上角头像唤起个人主页；个人主页以抽屉形式盖在页面之上
 *
 * tab 状态放在这里而不是 HomeScreen 内部，因为底栏是全局的：
 * 切到其它页面后底栏仍需显示当前选中的是哪一个。
 */
@Composable
fun StarioApp() {
    val context = LocalContext.current
    val nicknameStore = remember { NicknameStore(context) }

    var currentTab by remember { mutableStateOf(StarioTab.Home) }
    var navExpanded by remember { mutableStateOf(false) }
    var createExpanded by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf(nicknameStore.load()) }
    var profileVisible by remember { mutableStateOf(false) }
    var editingNickname by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PaperWhite)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                // 点空白处收起展开项
                if (navExpanded || createExpanded) {
                    navExpanded = false
                    createExpanded = false
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            // ---------- 页面内容 ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(durationMillis = 180),
                    label = "tab-content",
                ) { tab ->
                    when (tab) {
                        StarioTab.Home -> HomeScreen(
                            nickname = nickname,
                            onAvatarClick = { profileVisible = true },
                        )

                        // 时间 / 纸条 / 思索：先保持空白，只验证底栏切换
                        StarioTab.Time -> PlaceholderScreen(StarioTab.Time)
                        StarioTab.Paper -> PlaceholderScreen(StarioTab.Paper)
                        StarioTab.Think -> PlaceholderScreen(StarioTab.Think)
                    }
                }
            }

            // ---------- 全局底栏 ----------
            StarioBottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 18.dp),
                current = currentTab,
                onSelect = { currentTab = it },
                onCreate = { _: CreateKind -> /* 编辑页后续接入 */ },
                navExpanded = navExpanded,
                onNavExpandedChange = { navExpanded = it },
                createExpanded = createExpanded,
                onCreateExpandedChange = { createExpanded = it },
            )

            Spacer(Modifier.height(4.dp))
        }

        // ---------- 个人主页抽屉 ----------
        if (profileVisible) {
            ProfileDrawer(
                visible = true,
                nickname = nickname,
                counts = SampleData.counts,
                onDismiss = { profileVisible = false },
                onEditNickname = { editingNickname = true },
                onOpenRecycleBin = { /* 星尘页后续接入 */ },
                onOpenSettings = { /* 设置页后续接入 */ },
            )
        }
    }

    // ---------- 修改昵称浮窗 ----------
    if (editingNickname) {
        NicknameDialog(
            initial = nickname,
            onDismiss = { editingNickname = false },
            onConfirm = { newName ->
                nickname = newName
                nicknameStore.save(newName)
                editingNickname = false
            },
        )
    }
}
