package com.lacrimosa.staria.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * 无涟漪点击。
 *
 * 项目整体是「白纸 + 墨线」的克制风格，Material 的涟漪会在纸面上糊出一片灰色水波，
 * 与基调冲突。所以全项目统一用 `indication = null`（原本就是这样），
 * 这里只把它抽成一个 modifier，省掉每处都写五行的样板。
 */
fun Modifier.clickableNoIndication(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}
