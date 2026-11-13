package com.lacrimosa.staria.ui.selection

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 多选状态。
 *
 * 由根布局持有而不是放在页面内部，原因：
 * - 多选时右下角要显示红色删除按钮，它必须与新建按钮**同位置**，
 *   所以只能由底栏渲染；页面拿不到底栏的坐标。
 * - 多选需要在系统返回时退出，返回键的处理也在根布局。
 *
 * 页面通过 [enter]、[toggle]、[exit] 修改状态，根布局通过
 * [isActive] 与 [selectedIds] 决定底栏形态。
 */
@Stable
class SelectionState {

    /** 是否处于多选模式。 */
    var isActive by mutableStateOf(false)
        private set

    /** 已选中的条目 id。 */
    val selectedIds = mutableStateListOf<Long>()

    /**
     * 当前是否还有需要拦截系统返回的浮层状态。
     *
     * 由各页面登记（如纸条页的展开卡片），根布局据此统一处理返回键：
     * 先关浮层，再退多选，最后才交给系统。
     */
    var overlayActive by mutableStateOf(false)
        private set

    fun setOverlay(active: Boolean) {
        overlayActive = active
    }

    /** 进入多选，可带入首个选中项。 */
    fun enter(firstId: Long? = null) {
        isActive = true
        selectedIds.clear()
        if (firstId != null) selectedIds.add(firstId)
    }

    /** 切换某项的选中状态；取消最后一个时自动退出多选。 */
    fun toggle(id: Long) {
        if (!isActive) return
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
            if (selectedIds.isEmpty()) exit()
        } else {
            selectedIds.add(id)
        }
    }

    /** 退出多选并清空选中。 */
    fun exit() {
        isActive = false
        selectedIds.clear()
    }

    fun contains(id: Long): Boolean = selectedIds.contains(id)
}
