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

    /** 初始快照，用于判定「有没有改过」。 */
    private val initial = Snapshot(title, source, body)

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

    private companion object {
        const val MAX_HISTORY = 100
    }
}
