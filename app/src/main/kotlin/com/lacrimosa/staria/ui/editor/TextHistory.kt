package com.lacrimosa.staria.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 编辑页的文本与撤回重做栈。
 *
 * 标题、来源、正文共用**一条**历史，因为撤回的语义是「撤销上一次动作」，
 * 而用户在三个位置之间的输入顺序才是真正的操作顺序。
 *
 * 每次改动压入一个快照；[undo] 把当前状态挪进重做栈再回退一格。
 * 键位记录用不可变快照而不是 diff，正文体量小，够用且不会出错。
 */
class TextHistory(
    title: String,
    source: String,
    body: String,
) {
    private data class Snapshot(val title: String, val source: String, val body: String)

    /**
     * 初始快照，用于判定「有没有改过」。保存后由 [markSaved] 更新。
     *
     * 必须是 [mutableStateOf]：`isDirty()` 依赖它，保存后更新 initial
     * 需要触发重组，否则保存后 `dirty` 仍是旧值（true），
     * 点返回仍会误弹「未保存」。
     */
    private var initial by mutableStateOf(Snapshot(title, source, body))

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()

    var title by mutableStateOf(title)
        private set

    var source by mutableStateOf(source)
        private set

    var body by mutableStateOf(body)
        private set

    val canUndo: Boolean get() = undoStack.isNotEmpty()

    val canRedo: Boolean get() = redoStack.isNotEmpty()

    private fun current() = Snapshot(title, source, body)

    /** 压入改动前先记录旧值，并清空重做栈（新分支产生）。 */
    private fun push() {
        undoStack.addLast(current())
        // 栈过深没有必要，正文一次编辑也就能撤销几十步
        if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        redoStack.clear()
    }

    fun editTitle(value: String) {
        if (value == title) return
        push()
        title = value
    }

    fun editSource(value: String) {
        if (value == source) return
        push()
        source = value
    }

    fun editBody(value: String) {
        if (value == body) return
        push()
        body = value
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(current())
        apply(previous)
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(current())
        apply(next)
    }

    private fun apply(snapshot: Snapshot) {
        title = snapshot.title
        source = snapshot.source
        body = snapshot.body
    }

    /** 与初始状态相比是否有改动。 */
    fun isDirty(): Boolean = current() != initial

    /**
     * 标记为「已保存」：把当前状态当作新的初始快照，并清空撤销/重做栈。
     *
     * 编辑页现在「保存后不退出」，保存完成调用它让脏标记归零，
     * 下次点退出不再误弹「未保存」；同时清空栈避免撤销回退到保存前、
     * 造成脏标记与历史自相矛盾。
     */
    fun markSaved() {
        val saved = current()
        initial = saved
        undoStack.clear()
        redoStack.clear()
    }

    private companion object {
        const val MAX_HISTORY = 100
    }
}
