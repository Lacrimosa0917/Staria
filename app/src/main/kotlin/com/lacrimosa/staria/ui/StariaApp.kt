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
import com.lacrimosa.staria.data.EditDraft
import com.lacrimosa.staria.data.EditKind
import com.lacrimosa.staria.data.Essay
import com.lacrimosa.staria.data.NicknameStore
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.SampleData
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import com.lacrimosa.staria.ui.component.AvatarButton
import com.lacrimosa.staria.ui.component.AvatarSize
import com.lacrimosa.staria.ui.component.AvatarTopPadding
import com.lacrimosa.staria.ui.component.BarBottomPadding
import com.lacrimosa.staria.ui.component.BarHeight
import com.lacrimosa.staria.ui.component.BarSidePadding
import com.lacrimosa.staria.ui.component.ContentInsets
import com.lacrimosa.staria.ui.component.CreateKind
import com.lacrimosa.staria.ui.component.LocalContentInsets
import com.lacrimosa.staria.ui.component.ScrollEdgeGap
import com.lacrimosa.staria.ui.component.StariaBottomBar
import com.lacrimosa.staria.ui.component.StariaTab
import com.lacrimosa.staria.ui.editor.EditorScreen
import com.lacrimosa.staria.ui.home.HomeScreen
import com.lacrimosa.staria.ui.paper.ExpandedNoteOverlay
import com.lacrimosa.staria.ui.paper.PaperScreen
import com.lacrimosa.staria.ui.profile.NicknameDialog
import com.lacrimosa.staria.ui.profile.ProfileDrawer
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.think.EssayReaderPage
import com.lacrimosa.staria.ui.think.ThinkScreen
import com.lacrimosa.staria.ui.time.TimeScreen
import java.time.LocalDate

/**
 * 生成新条目的 id：取现有最大值 + 1，避免与已有的示例数据撞号。
 */
private fun nextId(existing: Collection<Long>): Long =
    (existing.maxOrNull() ?: 0L) + 1L

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

    // 随笔查看页（二级全屏页面）。非空时整页覆盖，底栏与头像不出现
    var readerEssay by remember { mutableStateOf<Essay?>(null) }

    // 编辑页（二级全屏）。非空时覆盖一切，底栏、头像、抽屉都不出现。
    // 新建与编辑共用：草稿里的 id 为空即新建。
    var editDraft by remember { mutableStateOf<EditDraft?>(null) }

    // 日记数据：删除后从列表移除（时间页不再展示）
    var diaryEntries by remember { mutableStateOf(SampleData.diaryEntries) }
    // 纸条数据：首页卡片堆叠与纸条页共用
    var paperNotes by remember { mutableStateOf(SampleData.notes) }
    // 随笔数据：思索页
    var essays by remember { mutableStateOf(SampleData.essays) }

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

    // 系统返回键的优先级：关闭编辑页 → 关闭随笔查看页 → 关展开卡片 → 退多选 → 交给系统
    BackHandler(
        enabled = editDraft != null || readerEssay != null ||
            expandedNote != null || selection.isActive,
    ) {
        when {
            // 编辑页有自己的未保存拦截，这里只做兜底：直接丢弃草稿
            editDraft != null -> editDraft = null
            readerEssay != null -> readerEssay = null
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
            StariaTab.Think -> {
                essays = essays.filterNot { it.id in ids }
            }
            else -> Unit
        }
        selection.exit()
    }

    // ---------- 编辑页：新建 ----------
    // 三种类型都从今天开始，日期此后不再变动
    val onCreate: (CreateKind) -> Unit = { kind ->
        collapseBar()
        val today = LocalDate.now()
        editDraft = EditDraft(
            kind = when (kind) {
                CreateKind.Diary -> EditKind.Diary
                CreateKind.Excerpt -> EditKind.Excerpt
                CreateKind.Essay -> EditKind.Essay
            },
            // id 为空即新建
            id = null,
            date = today,
        )
    }

    // ---------- 编辑页：保存 ----------
    // 日期一律沿用草稿里的创建日期，保存不刷新（用户确认的口径）
    val onSaveDraft: (EditDraft) -> Unit = { draft ->
        when (draft.kind) {
            EditKind.Diary -> {
                val existing = draft.id?.let { id -> diaryEntries.firstOrNull { it.id == id } }
                if (existing == null) {
                    diaryEntries = diaryEntries + DiaryEntry(
                        id = nextId(diaryEntries.map { it.id }),
                        date = draft.date,
                        title = draft.title.ifBlank { "无题" },
                        body = draft.body,
                    )
                } else {
                    diaryEntries = diaryEntries.map {
                        if (it.id == existing.id) {
                            it.copy(
                                title = draft.title.ifBlank { "无题" },
                                body = draft.body,
                            )
                        } else {
                            it
                        }
                    }
                }
            }

            EditKind.Excerpt -> {
                val existing = draft.id?.let { id -> paperNotes.firstOrNull { it.id == id } }
                if (existing == null) {
                    paperNotes = paperNotes + PaperNote(
                        id = nextId(paperNotes.map { it.id }),
                        text = draft.body,
                        source = draft.source.ifBlank { null },
                        editedOn = draft.date,
                    )
                } else {
                    paperNotes = paperNotes.map {
                        if (it.id == existing.id) {
                            it.copy(
                                text = draft.body,
                                source = draft.source.ifBlank { null },
                            )
                        } else {
                            it
                        }
                    }
                }
            }

            EditKind.Essay -> {
                val existing = draft.id?.let { id -> essays.firstOrNull { it.id == id } }
                if (existing == null) {
                    essays = essays + Essay(
                        id = nextId(essays.map { it.id }),
                        date = draft.date,
                        title = draft.title.ifBlank { "无题" },
                        body = draft.body,
                    )
                } else {
                    essays = essays.map {
                        if (it.id == existing.id) {
                            it.copy(
                                title = draft.title.ifBlank { "无题" },
                                body = draft.body,
                            )
                        } else {
                            it
                        }
                    }
                }
            }
        }
        editDraft = null
    }

    // 悬浮层的真实占位高度：必须把系统栏算进去。
    // 头像 / 底栏自身带 statusBarsPadding、systemBarsPadding，
    // 只按控件高度算会短一截，内容仍会压到它们身上。
    val systemBars = WindowInsets.systemBars
    val density = LocalDensity.current
    // getTop/getBottom 返回像素值，需在 Density 作用域内转成 dp
    val statusBarTopPx = systemBars.getTop(density)
    val navBarBottomPx = systemBars.getBottom(density)
    val statusBarTop = with(density) { statusBarTopPx.toDp() }
    val navBarBottom = with(density) { navBarBottomPx.toDp() }

    val contentInsets = ContentInsets(
        // 状态栏 + 头像上间距 + 头像直径 + 一点缝隙
        top = statusBarTop + AvatarTopPadding + AvatarSize + ScrollEdgeGap,
        // 底栏高度 + 下间距 + 导航栏 + 一点缝隙
        bottom = BarHeight + BarBottomPadding + navBarBottom + ScrollEdgeGap,
    )

    CompositionLocalProvider(LocalContentInsets provides contentInsets) {
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
                            onEdit = { entry: DiaryEntry ->
                                // 进入编辑页时沿用该条的创建日期
                                editDraft = EditDraft(
                                    kind = EditKind.Diary,
                                    id = entry.id,
                                    date = entry.date,
                                    title = entry.title,
                                    body = entry.body,
                                )
                            },
                            onDelete = { entry: DiaryEntry ->
                                diaryEntries = diaryEntries.filterNot { it.id == entry.id }
                            },
                        )

                        StariaTab.Paper -> PaperScreen(
                            notes = paperNotes,
                            selection = selection,
                            onUserInteract = collapseBar,
                            onEdit = { note: PaperNote ->
                                editDraft = EditDraft(
                                    kind = EditKind.Excerpt,
                                    id = note.id,
                                    date = note.editedOn,
                                    source = note.source.orEmpty(),
                                    body = note.text,
                                )
                            },
                            onDelete = { note: PaperNote ->
                                paperNotes = paperNotes.filterNot { it.id == note.id }
                            },
                            onExpand = { note ->
                                expandedNote = note
                                selection.setOverlay(true)
                            },
                        )

                        StariaTab.Think -> ThinkScreen(
                            essays = essays,
                            selection = selection,
                            onUserInteract = collapseBar,
                            onEdit = { essay: Essay ->
                                editDraft = EditDraft(
                                    kind = EditKind.Essay,
                                    id = essay.id,
                                    date = essay.date,
                                    title = essay.title,
                                    body = essay.body,
                                )
                            },
                            onDelete = { essay: Essay ->
                                essays = essays.filterNot { it.id == essay.id }
                            },
                            onOpen = { essay -> readerEssay = essay },
                        )
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
            onCreate = onCreate,
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

        // ---------- 随笔查看页（二级全屏） ----------
        // 覆盖在最上层，底栏与头像被完全遮住
        readerEssay?.let { essay ->
            EssayReaderPage(
                essay = essay,
                onBack = { readerEssay = null },
                onEdit = {
                    // 从查看页进入编辑：先收起查看页，编辑保存后回到列表
                    readerEssay = null
                    editDraft = EditDraft(
                        kind = EditKind.Essay,
                        id = essay.id,
                        date = essay.date,
                        title = essay.title,
                        body = essay.body,
                    )
                },
                onDelete = {
                    essays = essays.filterNot { it.id == essay.id }
                    readerEssay = null
                },
            )
        }

        // ---------- 编辑页（二级全屏） ----------
        // 声明在最后，压在查看页之上：从随笔查看页点编辑能直接进入
        editDraft?.let { draft ->
            EditorScreen(
                initial = draft,
                onExit = { editDraft = null },
                onSave = onSaveDraft,
            )
        }
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
