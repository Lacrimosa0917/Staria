package com.lacrimosa.staria.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lacrimosa.staria.data.DiaryEntry
import com.lacrimosa.staria.data.NicknameStore
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.ui.component.AvatarButton
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.CreateKind
import com.lacrimosa.staria.ui.component.StariaBottomBar
import com.lacrimosa.staria.ui.component.StariaTab
import com.lacrimosa.staria.ui.home.HomeScreen
import com.lacrimosa.staria.ui.placeholder.PlaceholderScreen
import com.lacrimosa.staria.ui.paper.ExpandedNoteOverlay
import com.lacrimosa.staria.ui.paper.PaperScreen
import com.lacrimosa.staria.ui.profile.NicknameDialog
import com.lacrimosa.staria.ui.profile.ProfileDrawer
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.time.TimeScreen

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
fun StariaApp() {
    val context = LocalContext.current
    val nicknameStore = remember { NicknameStore(context) }

    var currentTab by remember { mutableStateOf(StariaTab.Home) }
    var navExpanded by remember { mutableStateOf(false) }
    var createExpanded by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf(nicknameStore.load()) }
    var profileVisible by remember { mutableStateOf(false) }
    var editingNickname by remember { mutableStateOf(false) }

    // 纸条展开浮层：放在根布局渲染，才能压暗并覆盖顶栏与底栏
    var expandedNote by remember { mutableStateOf<PaperNote?>(null) }

    // 日记数据：删除后从列表移除（时间页不再展示）
    var diaryEntries by remember { mutableStateOf(SampleData.diaryEntries) }
    // 纸条数据：首页卡片堆叠与纸条页共用
    var paperNotes by remember { mutableStateOf(SampleData.notes) }

    // 多选状态。放在根布局是因为：
    // - 红色删除按钮要顶替新建按钮、与之同位置，只能由底栏渲染
    // - 系统返回键退出多选的处理也在这里
    val selection = remember { SelectionState() }

    // 切 tab 时退出多选：否则回到原页会残留选中态
    LaunchedEffect(currentTab) {
        selection.exit()
        expandedNote = null
        selection.setOverlay(false)
    }

    // 系统返回键的优先级：先关展开卡片，再退多选，都没有才交给系统
    BackHandler(enabled = expandedNote != null || selection.isActive) {
        when {
            expandedNote != null -> {
                expandedNote = null
                selection.setOverlay(false)
            }
            selection.isActive -> selection.exit()
        }
    }

    val collapseBar = {
        navExpanded = false
        createExpanded = false
    }

    // 右下角圆钮的动作：多选时删除所选，否则弹出新建菜单（由底栏内部处理）
    val onDeleteSelected: () -> Unit = {
        val ids = selection.selectedIds.toList()
        when (currentTab) {
            StariaTab.Time -> {
                diaryEntries = diaryEntries.filterNot { it.id in ids }
            }
            StariaTab.Paper -> {
                paperNotes = paperNotes.filterNot { it.id in ids }
            }
            else -> Unit
        }
        selection.exit()
    }

    Box(modifier = Modifier.fillMaxSize().background(PaperWhite)) {
        // ---------- 内容层：铺满全屏 ----------
        // 上下栏改为悬浮，内容不再被它们挤占高度，
        // 各页面自行留出 ContentTopInset / ContentBottomInset 避开遮挡。
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                // key(tab) 让每个页面在切入时是全新状态：
                // 否则多选标记、展开的卡片等会跨 tab 残留
                androidx.compose.runtime.key(tab) {
                    when (tab) {
                        StariaTab.Home -> HomeScreen(
                            notes = paperNotes,
                        )

                        StariaTab.Time -> TimeScreen(
                            entries = diaryEntries,
                            selection = selection,
                            // README：操作该页时若底栏展开则自动收起
                            onUserInteract = collapseBar,
                            onDelete = { entry: DiaryEntry ->
                                diaryEntries = diaryEntries.filterNot { it.id == entry.id }
                            },
                        )

                        StariaTab.Paper -> PaperScreen(
                            notes = paperNotes,
                            selection = selection,
                            onUserInteract = collapseBar,
                            onDelete = { note: PaperNote ->
                                paperNotes = paperNotes.filterNot { it.id == note.id }
                            },
                            onExpand = { note ->
                                expandedNote = note
                                selection.setOverlay(true)
                            },
                        )

                        StariaTab.Think -> PlaceholderScreen(StariaTab.Think)
                    }
                }
            }
        }

        // ---------- 悬浮层：头像（左上） ----------
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BarSidePadding, top = 14.dp),
        ) {
            AvatarButton(
                nickname = nickname,
                onClick = { profileVisible = true },
            )
        }

        // ---------- 悬浮层：底栏 + 新建按钮（底部） ----------
        StariaBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
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
            // 多选时右下角换成同位置的红色删除按钮
            deleteAction = if (selection.isActive) onDeleteSelected else null,
        )

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

        // ---------- 纸条展开浮层 ----------
        // 放在根布局最外层，才能压暗并覆盖顶栏与底栏
        expandedNote?.let { note ->
            ExpandedNoteOverlay(
                note = note,
                onDismiss = {
                    expandedNote = null
                    selection.setOverlay(false)
                },
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
