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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lacrimosa.staria.data.DiaryEntry
import com.lacrimosa.staria.data.DarkMode
import com.lacrimosa.staria.data.EditDraft
import com.lacrimosa.staria.data.EditKind
import com.lacrimosa.staria.data.Essay
import com.lacrimosa.staria.data.LibrarySnapshot
import com.lacrimosa.staria.data.LibraryStore
import com.lacrimosa.staria.data.NicknameStore
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.data.RecordCounts
import com.lacrimosa.staria.data.SampleData
import com.lacrimosa.staria.data.ThemeColor
import com.lacrimosa.staria.data.TrashItem
import com.lacrimosa.staria.data.TrashKind
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.lacrimosa.staria.ui.component.ActionTooltip
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
import com.lacrimosa.staria.ui.component.SlideDirection
import com.lacrimosa.staria.ui.component.SlideScreen
import com.lacrimosa.staria.ui.component.StariaBottomBar
import com.lacrimosa.staria.ui.component.StariaTab
import com.lacrimosa.staria.ui.component.LocalTooltipHost
import com.lacrimosa.staria.ui.component.TooltipHostState
import com.lacrimosa.staria.ui.component.TooltipSurface
import com.lacrimosa.staria.ui.editor.EditorScreen
import com.lacrimosa.staria.ui.home.HomeScreen
import com.lacrimosa.staria.ui.paper.ExpandedNoteOverlay
import com.lacrimosa.staria.ui.paper.PaperScreen
import com.lacrimosa.staria.ui.profile.NicknameDialog
import com.lacrimosa.staria.ui.profile.ProfileDrawer
import com.lacrimosa.staria.ui.selection.SelectionState
import com.lacrimosa.staria.ui.settings.SettingsScreen
import com.lacrimosa.staria.ui.theme.LocalGlassBackdrop
import com.lacrimosa.staria.ui.theme.LocalThemeColor
import com.lacrimosa.staria.ui.theme.PageBackground
import com.lacrimosa.staria.ui.theme.PaperWhite
import com.lacrimosa.staria.ui.think.EssayReaderPage
import com.lacrimosa.staria.ui.think.ThinkScreen
import com.lacrimosa.staria.ui.time.CalendarSheet
import com.lacrimosa.staria.ui.time.TimeScreen
import com.lacrimosa.staria.ui.trash.TrashScreen
import java.time.LocalDate

/**
 * 生成新条目的 id：取现有最大值 + 1，避免与已有的示例数据撞号。
 */
private fun nextId(existing: Collection<Long>): Long =
    (existing.maxOrNull() ?: 0L) + 1L

/**
 * 记住「最近一次非 null 值」的可变容器。
 *
 * 用于 SlideScreen 的退出动画：当 `visible` 因状态置 null 而变 false 时，
 * 退场动画期间的 content 仍需要一份有效内容来画。普通 `remember {}` 缓存的是
 * 快照，会随着 key 变化重建；这里用普通类字段直接覆盖，读取时始终拿到
 * 「最后一次有效值」，不参与 Compose 快照系统，所以退出动画期间不会闪空。
 */
private class LastNonNull<T : Any>(initial: T?) {
    var value: T? = initial

    /** 写入当前值；为 null 时保持旧值不变。返回更新后的值。 */
    fun update(current: T?): T? {
        if (current != null) value = current
        return value
    }
}

@Composable
private fun <T : Any> rememberLastNonNull(initial: T?): LastNonNull<T> =
    remember { LastNonNull(initial) }

/**
 * 回收站条目的内部 id。
 *
 * 不能复用原条目 id：三类内容各自从 1 开始编号，直接混在一起会撞号。
 * 用「类型 + 原 id」拼一个稳定的键，同一类型内天然唯一。
 */
private fun trashIdOf(kind: TrashKind, originalId: Long): Long =
    kind.ordinal.toLong() * 1_000_000_000L + originalId

/** 日记 → 星尘条目。 */
private fun DiaryEntry.toTrash(): TrashItem = TrashItem(
    trashId = trashIdOf(TrashKind.Diary, id),
    kind = TrashKind.Diary,
    originalId = id,
    date = date,
    title = title,
    body = body,
    source = null,
    deletedAt = System.nanoTime(),
)

/** 纸条 → 星尘条目。 */
private fun PaperNote.toTrash(): TrashItem = TrashItem(
    trashId = trashIdOf(TrashKind.Excerpt, id),
    kind = TrashKind.Excerpt,
    originalId = id,
    date = editedOn,
    title = "",
    body = text,
    source = source,
    deletedAt = System.nanoTime(),
)

/** 随笔 → 星尘条目。 */
private fun Essay.toTrash(): TrashItem = TrashItem(
    trashId = trashIdOf(TrashKind.Essay, id),
    kind = TrashKind.Essay,
    originalId = id,
    date = date,
    title = title,
    body = body,
    source = null,
    deletedAt = System.nanoTime(),
)

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
fun StariaApp(
    themeColor: ThemeColor,
    darkMode: DarkMode,
    onThemeColorChange: (ThemeColor) -> Unit,
    onDarkModeChange: (DarkMode) -> Unit,
) {
    val context = LocalContext.current
    val nicknameStore = remember { NicknameStore(context) }
    // 内容库持久化：日记 / 纸条 / 随笔 / 星尘
    val libraryStore = remember { LibraryStore(context) }

    var currentTab by remember { mutableStateOf(StariaTab.Home) }
    var createExpanded by remember { mutableStateOf(false) }

    var nickname by remember { mutableStateOf(nicknameStore.load()) }
    var profileVisible by remember { mutableStateOf(false) }
    var editingNickname by remember { mutableStateOf(false) }

    // 纸条展开浮层：在根布局渲染，才能压暗并覆盖顶栏与底栏。
    //
    // 与随笔查看页同理：只存 id，不存 PaperNote 对象。
    // 存对象会拿住打开那一刻的快照，编辑保存后浮层仍显示旧内容。
    var expandedNoteId by remember { mutableStateOf<Long?>(null) }

    // 日历浮层：同样放根布局最外层渲染，遮罩才能覆盖顶栏与底栏。
    // calendarPick 用于把选中的日期回传给时间页展开对应日记组。
    var showCalendar by remember { mutableStateOf(false) }
    var calendarPick by remember { mutableStateOf<LocalDate?>(null) }

    // 随笔查看页（二级全屏页面）。非空时整页覆盖，底栏与头像不出现。
    //
    // 只存 **id**，不存 Essay 对象：存对象等于存下当时的快照，
    // 保存修改后列表已更新，查看页却还拿着旧对象，要切页面才刷新。
    // 存 id 则每次重组都从 essays 里现取，自动跟随最新内容。
    var readerEssayId by remember { mutableStateOf<Long?>(null) }

    // 编辑页（二级全屏）。非空时覆盖一切，底栏、头像、抽屉都不出现。
    // 新建与编辑共用：草稿里的 id 为空即新建。
    var editDraft by remember { mutableStateOf<EditDraft?>(null) }

    // 日记数据：删除后从列表移除（时间页不再展示）
    // 首次运行从持久化库读取，无存档时用示例数据初始化。
    val loaded = remember { libraryStore.load() }
    var diaryEntries by remember { mutableStateOf(loaded?.diaries ?: SampleData.diaryEntries) }
    // 纸条数据：首页卡片堆叠与纸条页共用
    var paperNotes by remember { mutableStateOf(loaded?.papers ?: SampleData.notes) }
    // 随笔数据：思索页
    var essays by remember { mutableStateOf(loaded?.essays ?: SampleData.essays) }

    // 星尘（回收站）。所有删除都先进这里，恢复时再写回对应列表。
    // 顺序即删除顺序，展示时倒序（新删除的在上方）
    var trash by remember { mutableStateOf(loaded?.trash ?: emptyList()) }

    // 每次内容变化就落盘（SharedPreferences 是异步 apply，够用）
    LaunchedEffect(diaryEntries, paperNotes, essays, trash) {
        libraryStore.save(
            LibrarySnapshot(
                diaries = diaryEntries,
                papers = paperNotes,
                essays = essays,
                trash = trash,
            ),
        )
    }

    // 二级页面：星尘 / 设置。null 表示不展示
    var showTrash by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // 多选状态。放在根布局是因为：
    // - 红色删除按钮要顶替新建按钮、与之同位置，只能由底栏渲染
    // - 系统返回键退出多选的处理也在这里
    val selection = remember { SelectionState() }

    // 切 tab 时退出多选：否则回到原页会残留选中态
    LaunchedEffect(currentTab) {
        selection.exit()
        expandedNoteId = null
        selection.setOverlay(false)
    }

    // 系统返回键的优先级：**从上层覆盖面往下收**。
    //
    // 顺序必须按「层」排，不能按「谁先被打开」排：
    // 星尘 / 设置页是从个人主页抽屉点进去的，抽屉还留在下面。
    // 如果先把 profileVisible 判掉，按返回就会先收起抽屉，
    // 而当前可见的星尘 / 设置页毫无反应——这正是曾经的 bug。
    //
    // 正确顺序：覆盖在最上面的先关，逐层往下。
    // 星尘/设置 → 编辑页 → 随笔查看页 → 关展开卡片 → 抽屉 → 退多选 → 交给系统
    BackHandler(
        enabled = showTrash || showSettings || editDraft != null ||
            readerEssayId != null || expandedNoteId != null ||
            profileVisible || selection.isActive,
    ) {
        when {
            // 二级页压在最上层，先关它
            showTrash -> showTrash = false
            showSettings -> showSettings = false
            // 编辑页有自己的未保存拦截，这里只做兜底：直接丢弃草稿
            editDraft != null -> editDraft = null
            readerEssayId != null -> readerEssayId = null
            expandedNoteId != null -> {
                expandedNoteId = null
                selection.setOverlay(false)
            }
            // 抽屉在最下层，最后收
            profileVisible -> profileVisible = false
            selection.isActive -> selection.exit()
        }
    }

    // 底栏已常驻展开，不再有「收起」这一态；这里只负责收新建菜单。
    val collapseBar = {
        createExpanded = false
    }

    // 右下角圆钮的动作：多选时删除所选，否则弹出新建菜单（由底栏内部处理）
    val onDeleteSelected: () -> Unit = {
        val ids = selection.selectedIds.toList()
        when (currentTab) {
            StariaTab.Time -> {
                val moving = diaryEntries.filter { it.id in ids }
                diaryEntries = diaryEntries.filterNot { it.id in ids }
                trash = trash + moving.map { it.toTrash() }
            }
            StariaTab.Paper -> {
                val moving = paperNotes.filter { it.id in ids }
                paperNotes = paperNotes.filterNot { it.id in ids }
                trash = trash + moving.map { it.toTrash() }
            }
            StariaTab.Think -> {
                val moving = essays.filter { it.id in ids }
                essays = essays.filterNot { it.id in ids }
                trash = trash + moving.map { it.toTrash() }
            }
            else -> Unit
        }
        selection.exit()
    }

    // ---------- 星尘：恢复 ----------
    // 按类型写回各自的列表；原 id 不变，所以恢复后的条目回到原来的位置
    val onRestoreFromTrash: (List<Long>) -> Unit = { ids ->
        val restoring = trash.filter { it.trashId in ids }
        trash = trash.filterNot { it.trashId in ids }

        val diaries = restoring.filter { it.kind == TrashKind.Diary }
        if (diaries.isNotEmpty()) {
            diaryEntries = diaryEntries + diaries.map {
                DiaryEntry(id = it.originalId, date = it.date, title = it.title, body = it.body)
            }
        }

        val papers = restoring.filter { it.kind == TrashKind.Excerpt }
        if (papers.isNotEmpty()) {
            paperNotes = paperNotes + papers.map {
                PaperNote(
                    id = it.originalId,
                    text = it.body,
                    source = it.source,
                    editedOn = it.date,
                )
            }
        }

        val restoredEssays = restoring.filter { it.kind == TrashKind.Essay }
        if (restoredEssays.isNotEmpty()) {
            essays = essays + restoredEssays.map {
                Essay(id = it.originalId, date = it.date, title = it.title, body = it.body)
            }
        }
    }

    // ---------- 星尘：永久删除 ----------
    val onPurgeFromTrash: (List<Long>) -> Unit = { ids ->
        trash = trash.filterNot { it.trashId in ids }
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
    // 日期一律沿用草稿里的创建日期，保存不刷新（用户确认的口径）。
    // 保存后**不退出**，停留在编辑页继续编辑，返回落盘后的真实 id。
    // 编辑器内部用返回值更新 currentId：新建后再次保存走「编辑」分支，
    // 不会重复新建一条。
    val onSaveDraft: (EditDraft) -> Long = { draft ->
        val finalId: Long = when (draft.kind) {
            EditKind.Diary -> {
                val existing = draft.id?.let { id -> diaryEntries.firstOrNull { it.id == id } }
                if (existing == null) {
                    val newId = nextId(diaryEntries.map { it.id })
                    diaryEntries = diaryEntries + DiaryEntry(
                        id = newId,
                        date = draft.date,
                        title = draft.title.ifBlank { "无题" },
                        body = draft.body,
                    )
                    newId
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
                    existing.id
                }
            }

            EditKind.Excerpt -> {
                val existing = draft.id?.let { id -> paperNotes.firstOrNull { it.id == id } }
                if (existing == null) {
                    val newId = nextId(paperNotes.map { it.id })
                    paperNotes = paperNotes + PaperNote(
                        id = newId,
                        text = draft.body,
                        source = draft.source.ifBlank { null },
                        editedOn = draft.date,
                    )
                    newId
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
                    existing.id
                }
            }

            EditKind.Essay -> {
                val existing = draft.id?.let { id -> essays.firstOrNull { it.id == id } }
                if (existing == null) {
                    val newId = nextId(essays.map { it.id })
                    essays = essays + Essay(
                        id = newId,
                        date = draft.date,
                        title = draft.title.ifBlank { "无题" },
                        body = draft.body,
                    )
                    newId
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
                    existing.id
                }
            }
        }

        // 保存后**不退出**，只返回落盘后的真实 id。
        // EditorScreen 内部用该 id 更新自己的 currentId，
        // 下次保存走「编辑」分支，不会重复新建。
        finalId
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

    // 全局 tooltip 宿主：页面发起请求、根布局统一绘制（见 TooltipHostState）
    val tooltipHost = remember { TooltipHostState() }

    // 液态玻璃的采样源：把「页面底色 + 全部页面内容」录进一个 GraphicsLayer。
    // 玻璃控件（底栏、头像、新建栏、二级页顶栏、tooltip）通过 drawBackdrop
    // 重新绘制这块内容并施加模糊/折射，从而「透出」它们下面真正的内容。
    // 必须先画底色：不画的话，没有内容覆盖的区域在玻璃里会露出透明像素。
    // 底色在 Composable 作用域先取出（PageBackground 现在是动态令牌），
    // 再闭包捕获进 rememberLayerBackdrop 的绘制块。
    // 用 key 包裹：切深色模式时底色变化，强制重建 backdrop，
    // 否则 rememberLayerBackdrop 只录一次，玻璃透出的底色会停在旧值。
    val backdropBaseColor = PageBackground
    val glassBackdrop = key(backdropBaseColor) {
        rememberLayerBackdrop {
            drawRect(backdropBaseColor)
            drawContent()
        }
    }

    CompositionLocalProvider(
        LocalContentInsets provides contentInsets,
        LocalGlassBackdrop provides glassBackdrop,
        LocalTooltipHost provides tooltipHost,
        LocalThemeColor provides themeColor,
    ) {
    Box(modifier = Modifier.fillMaxSize().background(PageBackground)) {
        // ---------- 内容层：铺满全屏 ----------
        // 上下栏改为悬浮，内容不再被它们挤占高度，
        // 各页面自行留出 ContentTopInset / ContentBottomInset 避开遮挡。
        // layerBackdrop 把这一层录进 backdrop，供上面的玻璃控件采样。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(glassBackdrop)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    // 点空白处收起新建菜单展开的口。
                    // 底栏本身常驻展开，不再有「收起底栏」这个动作。
                    if (createExpanded) collapseBar()
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
                            onOpenCalendar = { showCalendar = true },
                            calendarPick = calendarPick,
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
                                // 移入星尘，时间页不再展示（README）
                                trash = trash + entry.toTrash()
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
                                trash = trash + note.toTrash()
                                paperNotes = paperNotes.filterNot { it.id == note.id }
                            },
                            onExpand = { note ->
                                expandedNoteId = note.id
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
                                trash = trash + essay.toTrash()
                                essays = essays.filterNot { it.id == essay.id }
                            },
                            onOpen = { essay -> readerEssayId = essay.id },
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
            createExpanded = createExpanded,
            onCreateExpandedChange = { createExpanded = it },
            // 多选时右下角换成同位置的红色删除按钮
            deleteAction = if (selection.isActive) onDeleteSelected else null,
        )

        // ---------- 个人主页抽屉 ----------
        // 从抽屉进入星尘/设置时**不关闭抽屉**：二级页面声明在其后（层级更高），
        // 会覆盖在抽屉之上；返回时二级页面关闭，抽屉仍在后台，自然回到个人主页。
        // 始终挂载，进出动画由 ProfileDrawer 内部的 AnimatedVisibility 管理。
        ProfileDrawer(
            visible = profileVisible,
            nickname = nickname,
            // 数量必须跟着当前三个列表走：新建、删除后要立刻对上，
            // 不能再用 SampleData 里那份写死的示例值
            counts = RecordCounts(
                // 与时间页一致：标记 deleted 的不计入
                diary = diaryEntries.count { !it.deleted },
                paper = paperNotes.size,
                essay = essays.size,
            ),
            onDismiss = { profileVisible = false },
            onEditNickname = { editingNickname = true },
            onOpenRecycleBin = {
                showTrash = true
            },
            onOpenSettings = {
                showSettings = true
            },
        )

        // ---------- 纸条展开浮层 ----------
        // 放在根布局最外层，才能压暗并覆盖顶栏与底栏。
        // 按 id 从 paperNotes 现取，编辑保存后浮层内容跟着更新；
        // 该条被删除（id 查不到）时自然不再展示。
        val expandedNote = expandedNoteId?.let { id -> paperNotes.firstOrNull { it.id == id } }
        expandedNote?.let { note ->
            ExpandedNoteOverlay(
                note = note,
                onDismiss = {
                    expandedNoteId = null
                    selection.setOverlay(false)
                },
            )
        }

        // ---------- 日历浮层 ----------
        // 与纸条展开浮层同样放在最外层，遮罩统一覆盖顶栏与底栏，
        // 否则日历遮罩盖不住玻璃控件，白边会在压暗背景上异常突出。
        if (showCalendar) {
            CalendarSheet(
                initialDate = calendarPick ?: LocalDate.now(),
                diaryDates = diaryEntries
                    .filterNot { it.deleted }
                    .map { it.date }
                    .toSet(),
                onDismiss = { showCalendar = false },
                onPick = { date ->
                    showCalendar = false
                    calendarPick = date
                },
            )
        }

        // ---------- 随笔查看页（二级全屏，从右侧滑入/滑出） ----------
        // 覆盖在最上层，底栏与头像被完全遮住
        //
        // 内容按 id 从 essays 现取，而不是缓存对象：
        // 这样在编辑页保存后，查看页再进来读到的就是最新内容。
        // 打开期间被删除的话（id 查不到）视为已关闭，避免空白页卡住。
        val readerEssay = readerEssayId?.let { id -> essays.firstOrNull { it.id == id } }
        // 退出动画期间 readerEssayId 已置 null，需要保留最后一份内容来画。
        val lastReaderEssay = rememberLastNonNull(readerEssay)
        SlideScreen(visible = readerEssayId != null, direction = SlideDirection.FromRight) {
            val essay = lastReaderEssay.update(readerEssay)
            if (essay != null) {
                EssayReaderPage(
                    essay = essay,
                    onBack = { readerEssayId = null },
                    onEdit = {
                        // 从查看页进入编辑：先收起查看页，编辑保存后回到列表
                        readerEssayId = null
                        editDraft = EditDraft(
                            kind = EditKind.Essay,
                            id = essay.id,
                            date = essay.date,
                            title = essay.title,
                            body = essay.body,
                        )
                    },
                    onDelete = {
                        trash = trash + essay.toTrash()
                        essays = essays.filterNot { it.id == essay.id }
                        readerEssayId = null
                    },
                )
            }
        }

        // ---------- 星尘（二级全屏，从左侧滑入/滑出） ----------
        SlideScreen(visible = showTrash, direction = SlideDirection.FromLeft) {
            TrashScreen(
                // 新删除的在上方
                items = trash.sortedByDescending { it.deletedAt },
                onBack = { showTrash = false },
                onRestore = onRestoreFromTrash,
                onDelete = onPurgeFromTrash,
            )
        }

        // ---------- 设置（二级全屏，从左侧滑入/滑出） ----------
        SlideScreen(visible = showSettings, direction = SlideDirection.FromLeft) {
            SettingsScreen(
                themeColor = themeColor,
                darkMode = darkMode,
                onBack = { showSettings = false },
                onPickThemeColor = onThemeColorChange,
                onPickDarkMode = onDarkModeChange,
            )
        }

        // ---------- 编辑页（二级全屏，从右侧滑入/滑出） ----------
        // 声明在最后，压在查看页之上：从随笔查看页点编辑能直接进入
        //
        // 这里不能用 rememberLastNonNull 缓存草稿：
        // 草稿是「打开编辑页那一刻的快照」，缓存后即便列表更新，
        // 再次进入编辑页仍会拿到旧内容。改为正常读取 editDraft，
        // 退出动画需要的最后一份内容由 EditorScreen 内部保留。
        val draft = editDraft
        SlideScreen(visible = editDraft != null, direction = SlideDirection.FromRight) {
            if (draft != null) {
                EditorScreen(
                    initial = draft,
                    onExit = { editDraft = null },
                    onSave = onSaveDraft,
                )
            }
        }
    }

    // ---------- 全局 tooltip（压暗遮罩 + 淡灰菜单） ----------
    // 页面只负责发起请求（见 TooltipHostState），这里统一绘制。
    tooltipHost.current?.let { req ->
        TooltipSurface(
            actions = req.actions,
            anchor = req.anchor,
            onDismiss = { tooltipHost.dismiss() },
            preferredCorner = req.preferredCorner,
            keepLit = req.keepLit,
        )
    }
    }

    // ---------- 修改昵称浮窗 ----------
    if (editingNickname) {
        NicknameDialog(
            initial = nickname,
            themeColor = themeColor,
            onDismiss = { editingNickname = false },
            onConfirm = { newName ->
                nickname = newName
                nicknameStore.save(newName)
                editingNickname = false
            },
        )
    }
}
