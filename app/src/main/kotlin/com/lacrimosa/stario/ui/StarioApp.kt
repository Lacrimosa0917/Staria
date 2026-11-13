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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lacrimosa.stario.data.DiaryEntry
import com.lacrimosa.stario.data.NicknameStore
import com.lacrimosa.stario.data.PaperNote
import com.lacrimosa.stario.data.SampleData
import com.lacrimosa.stario.ui.component.AvatarButton
import com.lacrimosa.stario.ui.component.BarSidePadding
import com.lacrimosa.stario.ui.component.CreateKind
import com.lacrimosa.stario.ui.component.StarioBottomBar
import com.lacrimosa.stario.ui.component.StarioTab
import com.lacrimosa.stario.ui.home.HomeScreen
import com.lacrimosa.stario.ui.placeholder.PlaceholderScreen
import com.lacrimosa.stario.ui.paper.PaperScreen
import com.lacrimosa.stario.ui.profile.NicknameDialog
import com.lacrimosa.stario.ui.profile.ProfileDrawer
import com.lacrimosa.stario.ui.theme.PaperWhite
import com.lacrimosa.stario.ui.time.TimeScreen

/**
 * 应用根布局。
 *
 * 结构（README）：
 * - 四个查看页共用同一个底部区域：胶囊底栏 + 新建按钮，切页时不消失
 * - 四个查看页也都保留左上角头像，点击进入个人主页
 * - 个人主页以抽屉形式盖在页面之上
 *
 * tab 与头像都放在这里而不是各页面内部，避免各页重复实现导致行为不一致。
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

    // 日记数据：删除后从列表移除（时间页不再展示）
    var diaryEntries by remember { mutableStateOf(SampleData.diaryEntries) }
    // 纸条数据：首页卡片堆叠与纸条页共用
    var paperNotes by remember { mutableStateOf(SampleData.notes) }

    // 多选进行中：底栏隐藏新建按钮，由页面的红色删除按钮接管右下角
    var selectionActive by remember { mutableStateOf(false) }

    val collapseBar = {
        navExpanded = false
        createExpanded = false
    }

    Box(modifier = Modifier.fillMaxSize().background(PaperWhite)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---------- 顶部：头像（四页常驻） ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = BarSidePadding, top = 14.dp),
            ) {
                AvatarButton(
                    nickname = nickname,
                    onClick = { profileVisible = true },
                )
            }

            // ---------- 页面内容 ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        // 点空白处收起底栏展开口
                        if (navExpanded || createExpanded) collapseBar()
                    },
            ) {
                Crossfade(
                    targetState = currentTab,
                    animationSpec = tween(durationMillis = 180),
                    label = "tab-content",
                ) { tab ->
                    when (tab) {
                        StarioTab.Home -> HomeScreen(
                            notes = paperNotes,
                        )

                        StarioTab.Time -> TimeScreen(
                            entries = diaryEntries,
                            // README：操作该页时若底栏展开则自动收起
                            onUserInteract = collapseBar,
                            onDelete = { entry: DiaryEntry ->
                                diaryEntries = diaryEntries.filterNot { it.id == entry.id }
                            },
                            onSelectionModeChange = { selectionActive = it },
                        )

                        StarioTab.Paper -> PaperScreen(
                            notes = paperNotes,
                            onUserInteract = collapseBar,
                            onDelete = { note: PaperNote ->
                                paperNotes = paperNotes.filterNot { it.id == note.id }
                            },
                            onSelectionModeChange = { selectionActive = it },
                        )

                        StarioTab.Think -> PlaceholderScreen(StarioTab.Think)
                    }
                }
            }

            // ---------- 全局底栏 ----------
            Column(modifier = Modifier.systemBarsPadding()) {
                StarioBottomBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = BarSidePadding,
                            end = BarSidePadding,
                            bottom = 18.dp,
                        ),
                    current = currentTab,
                    onSelect = { currentTab = it },
                    onCreate = { _: CreateKind -> /* 编辑页后续接入 */ },
                    navExpanded = navExpanded,
                    onNavExpandedChange = { navExpanded = it },
                    createExpanded = createExpanded,
                    onCreateExpandedChange = { createExpanded = it },
                    showCreateButton = !selectionActive,
                )
                Spacer(Modifier.height(4.dp))
            }
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
