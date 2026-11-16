package com.lacrimosa.staria.ui.component

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * 一次 tooltip 请求。
 *
 * @param anchor 长按点在窗口坐标系里的位置
 * @param actions 菜单项
 * @param onDismiss 关闭时回调（各页面用它清理自己的 tooltip 状态）
 * @param keepLit 被长按卡片的窗口矩形。压暗遮罩会在它位置挖洞，
 *   让这张卡保持原亮度（视觉上像「从纸面里被拎起来」）。null 表示整屏压暗。
 */
data class TooltipRequest(
    val anchor: Offset,
    val actions: List<TooltipAction>,
    val preferredCorner: AnchorCorner,
    val onDismiss: () -> Unit,
    val keepLit: Rect? = null,
)

/**
 * 全局 tooltip 宿主状态。
 *
 * ## 为什么必须提升到根布局
 *
 * 液态玻璃的 `drawBackdrop` 会把「backdrop 录到的内容」重画一遍。
 * 如果 tooltip 自己就渲染在**被录制的那棵子树里**（即页面内部），
 * 它一边被录、一边又去画录制结果，就构成渲染递归 ——
 * 实机会直接栈溢出崩溃（实测 500 多层 `RenderNode::prepareTreeImpl`）。
 *
 * 所以 tooltip 必须渲染在 `layerBackdrop` 子树**之外**：
 * 页面只负责「请求显示」，由根布局在最外层真正绘制。
 * 这与仓库 Glass Bottom Sheet 一节强调的「玻璃层不能进入被录制的子树」一致。
 */
@Stable
class TooltipHostState {

    var current: TooltipRequest? by mutableStateOf(null)
        private set

    fun show(
        anchor: Offset,
        actions: List<TooltipAction>,
        preferredCorner: AnchorCorner = AnchorCorner.BottomLeft,
        onDismiss: () -> Unit,
        keepLit: Rect? = null,
    ) {
        if (actions.isEmpty()) return
        current = TooltipRequest(anchor, actions, preferredCorner, onDismiss, keepLit)
    }

    /** 关闭并回调，保证页面侧状态一起清掉。 */
    fun dismiss() {
        val req = current ?: return
        current = null
        req.onDismiss()
    }

    /** 只清状态，不回调（页面自己已经在清理时用，避免重复回调）。 */
    fun clear() {
        current = null
    }
}

/**
 * 下发到各页面的 host。
 *
 * 页面调用 [ActionTooltip] 时若拿得到 host，就把请求转给它、自己什么都不画；
 * 拿不到（例如脱离根布局单独预览）才退化成就地渲染。
 */
val LocalTooltipHost = compositionLocalOf<TooltipHostState?> { null }
