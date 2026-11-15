package com.lacrimosa.staria.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lacrimosa.staria.data.MAX_STACKED_NOTES
import com.lacrimosa.staria.data.PaperNote
import com.lacrimosa.staria.ui.theme.CardRadius
import com.lacrimosa.staria.ui.theme.Ink
import com.lacrimosa.staria.ui.theme.InkMuted
import com.lacrimosa.staria.ui.theme.PaperWhite
import kotlin.math.hypot
import kotlin.math.tanh
import kotlinx.coroutines.launch

/** 换卡的位移补间：先快后慢，落定时没有硬刹车感。 */
private val SwapEasing = CubicBezierEasing(0.22f, 0.77f, 0.23f, 1f)

/** 换卡补间时长。退场卡片与让位卡片共用，才会像同一只手在推。 */
private const val SwapDurationMs = 540

/** 与参考实现一致的橡皮筋：160 * tanh(delta / 160)。 */
private fun rubberBand(delta: Float): Float = 160f * tanh(delta / 160f)

/** 手势判定阈值，统一换算成像素后一次传入。 */
private data class DragThresholds(
    val tapSlop: Float,
    val flingDistance: Float,
    val fastDistance: Float,
    val flingSpeed: Float,
)

/**
 * 纸条卡片堆叠。
 *
 * 交互参考 LeiBox「拖甩照片卡片 / Card Swap」
 * （https://leibox.pantoria.cn/docs/animations/card-swap）：
 * - 前排卡片可拖拽，位移经过橡皮筋衰减；松手未达阈值则弹回
 * - 点按、甩动（位移超阈值或速度够快）都会让前排卡片退到队尾，其余顺势前移
 * - 卡片按透视关系逐层缩小、向右上方错开
 *
 * ## 退场动画为什么这么写
 *
 * 早先的实现在松手瞬间把拖拽位移清零、同时改 order，卡片会先「瞬移回原位」
 * 再补间到队尾——看起来像被弹开，而不是被收进纸堆。
 *
 * 现在分三步，中间没有一帧跳变：
 * 1. 松手时把当前拖拽位移**并入基准位置**（`base.snapTo(base + drag)`），画面不动
 * 2. 再把 `drag` 清零，此刻卡片位置仍等于松手点
 * 3. 改 order，[LaunchedEffect] 从这一点补间到队尾位置
 *
 * 另外退场卡片在整个补间期间画在**最上层**（[departingSlot] 排在绘制序末尾），
 * 否则它一改 order 就被新前排盖住，用户根本看不到「回到底层」这个过程。
 * 补间结束后才放回队尾的绘制位置，于是自然沉到纸堆底下。
 *
 * 状态能跨越这个过程，靠的是每张卡片都挂在自己的 [key] 上、且**始终在同一个
 * 调用点绘制**：如果为了压层把退场卡片挪到另一个 composable 位置，
 * 它的 [Animatable] 会重建、位置归零，动画又会跳。
 *
 * 与参考实现的差异：
 * - 参考用 `skewY(6deg)` 倾斜纸面；Compose 无等价 shear 变换，
 *   这里改为拖拽时的轻微旋转，避免字形被拉变形
 * - 参考是照片轮播，这里展示摘录文字，卡面改为纸质样式
 *
 * @param notes 全部纸条。首页传入完整列表，这里维护一个最多 [MAX_STACKED_NOTES]
 *   张的展示队列，每次换卡时把队尾换成随机抽出的另一张，从而覆盖所有纸条。
 */
@Composable
fun PaperCardStack(
    notes: List<PaperNote>,
    modifier: Modifier = Modifier,
) {
    // 展示队列：初始随机抽最多 MAX_STACKED_NOTES 张，不足则按实际数量。
    // 换卡时把队尾替换成一张未展示的随机纸条，始终覆盖全部纸条。
    val visible = remember(notes) {
        mutableStateListOf<PaperNote>().apply {
            addAll(notes.shuffled().take(MAX_STACKED_NOTES))
        }
    }
    if (visible.isEmpty()) return

    // order 存放 visible 的下标，[0] 为最前面一张
    var order by remember(visible) { mutableStateOf(visible.indices.toList()) }

    // 正在退回队尾的卡片（visible 下标）。补间期间它画在最上层，结束后清空。
    var departing by remember { mutableStateOf<Int?>(null) }

    // 换卡后把队尾换成一张随机的新纸条，保证换下去的内容不再重复、范围覆盖全部。
    // 队尾下标 = 刚被移到末尾的那张（order 循环后 index 0 移到末尾）。
    fun refreshBack(notes: List<PaperNote>) {
        if (visible.size < MAX_STACKED_NOTES) return
        val shown = visible.toSet()
        val candidates = notes.filterNot { it in shown }
        if (candidates.isNotEmpty()) {
            // 队尾是 order 最后一位对应的下标
            val tailIndex = order.last()
            visible[tailIndex] = candidates.random()
        }
    }

    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        // 卡片尺寸随可用空间自适应；同时用可用高度反推上限，
        // 防止小窗时卡片被压扁导致正文与出处重叠。
        // maxHeight 在无界约束下是 Infinity，此时只用宽度那一支。
        val aspect = 1.36f
        val widthLimit = minOf(maxWidth * 0.60f, 240.dp)
        val heightLimit = if (maxHeight.value.isFinite()) maxHeight * 0.92f else widthLimit * aspect
        val cardHeight = minOf(widthLimit * aspect, heightLimit)
        val cardWidth = cardHeight / aspect

        val count = order.size
        val last = (count - 1).coerceAtLeast(1)

        // 透视缩放：越靠后越小（保留参考实现的立体感）
        fun scaleAt(slot: Int): Float = 850f / (850f + slot * 70f)
        val scaleFront = scaleAt(0)

        // ---- 间距：在「纸堆层次感」与「视觉中心」之间取平衡 ----
        // 间距按卡片尺寸取固定比例，宽度只作为溢出保护。
        // 纸堆跨度 = stripX*(n-1)，跨度越小整体重心越靠近前排（即屏幕中心），
        // 所以这里压得比较紧，只保留刚够分辨每一层的宽度。
        val availRight = maxWidth / 2 - cardWidth / 2
        val stripX = minOf(cardWidth * 0.032f, availRight / last * 0.82f)
            .coerceAtLeast(4.dp)
        val stripY = minOf(cardHeight * 0.026f, maxHeight * 0.06f / last)
            .coerceAtLeast(3.dp)

        // ---- 中心对齐：前排为主，兼顾纸堆整体 ----
        // 后排向右上延展，纸堆包围盒中心会比前排中心右偏 stripX*(n-1)/2。
        // 完全按包围盒居中会把正文卡推向左（正文与 slogan 错位）；
        // 完全按前排居中则纸堆观感偏右。
        // 取 1/4 补偿：正文卡只左移一点点，纸堆重心也明显回正。
        val centerShiftX = with(density) { (stripX * (count - 1) / 4).toPx() }
        val centerShiftY = with(density) { (stripY * (count - 1) / 4).toPx() }

        /**
         * 第 [slot] 层卡片的平移量。坐标基准是容器**上沿**（TopCenter 对齐），
         * 整摞纸自上而下排开：最末一层贴容器顶，前排落在容器底。
         *
         * 早先用 Center 对齐，容器高 `cardHeight + stripY*last` 却只放了
         * `cardHeight` 的卡片，多出来的高度全变成卡片上方的空白——
         * 这就是「slogan 与卡片之间空得离谱」的真正来源。
         *
         * 水平：右边缘等距（right_i = stripX*i + 常数）
         *   tx_i = stripX*i + (cardW/2)(scale0 - scale_i)
         * 垂直：上边缘等距（top_i 每层上移 stripY）
         *   ty_i = stripY*(last - i) - (cardH/2)(1 - scale_i)
         *   后一项补的是「绕中心缩放」带来的上沿内缩：
         *   graphicsLayer 的 scale 以中心为原点，缩小时上沿会往下跑。
         */
        fun slotOffset(slot: Int): Offset {
            val scale = scaleAt(slot)
            val factor = slot.toFloat()
            val x = with(density) {
                (stripX * factor + (cardWidth / 2) * (scaleFront - scale)).toPx()
            } - centerShiftX
            val y = with(density) {
                (stripY * (last - factor) - (cardHeight / 2) * (1f - scale)).toPx()
            } + centerShiftY
            return Offset(x, y)
        }

        // 与参考实现同量级的阈值
        val thresholds = DragThresholds(
            tapSlop = with(density) { 6.dp.toPx() },
            flingDistance = with(density) { 70.dp.toPx() },
            fastDistance = with(density) { 24.dp.toPx() },
            flingSpeed = 0.6f,
        )

        // 绘制顺序：退场卡片排最底 → 后排 → 前排。
        //
        // 退场卡片必须**一开始就压在最底下**，这是「回到底层」这个动作的核心：
        // 它从松手点滑向队尾的整个过程中都从纸堆下方经过，
        // 于是既看得见轨迹，终点又天然在底层，不需要在结尾翻一次 z 序（那会跳一帧）。
        //
        // 副作用正好是想要的两套手感：
        // - 拖拽松手：卡片被拖离了原位，滑动全程可见 = 完整演示回到底层
        // - 轻点：卡片就在原位，立刻被新前排盖住，只剩「下一张上来」= 默认的轻快换卡
        val departingSlot = departing?.let { order.indexOf(it) } ?: -1
        val drawSlots = buildList {
            if (departingSlot >= 0) add(departingSlot)
            for (slot in (count - 1) downTo 0) {
                if (slot != departingSlot) add(slot)
            }
        }

        Box(
            // 高度正好包住整摞纸：最末一层贴顶，前排落到容器底（见 slotOffset 的推导）。
            // 早先这里是 Center 对齐，容器多出来的高度就变成了卡片上方的死白。
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + stripY * last),
            contentAlignment = Alignment.TopCenter,
        ) {
            for (slot in drawSlots) {
                val noteIndex = order[slot]
                val isDeparting = slot == departingSlot
                // key 保证每张卡的运动状态跟着「这张纸」走，而不是跟着绘制位置走
                key(noteIndex) {
                    StackCard(
                        note = visible[noteIndex],
                        target = slotOffset(slot),
                        targetScale = scaleAt(slot),
                        isFront = slot == 0 && !isDeparting,
                        isDeparting = isDeparting,
                        canSwap = count > 1 && slot == 0,
                        cardWidth = cardWidth,
                        cardHeight = cardHeight,
                        haptics = haptics,
                        thresholds = thresholds,
                        onSwap = { withDepartAnimation ->
                            // 只有甩出才进入「退场演示」；轻点让旧卡直接被盖住
                            if (withDepartAnimation) departing = noteIndex
                            order = order.drop(1) + order.first()
                            // 轻点无退场动画：旧卡立刻被盖住，此时直接把队尾换成随机新卡
                            if (!withDepartAnimation) refreshBack(notes)
                        },
                        onDepartSettled = {
                            departing = null
                            // 甩出退场补间结束后，队尾内容换成随机新卡
                            refreshBack(notes)
                        },
                    )
                }
            }
        }
    }
}

/**
 * 单张卡片。
 *
 * 位置由两个 [Animatable] 叠加：`base`（槽位位置，声明式补间）+ `drag`（手指位移）。
 * 拆开是因为退场时要把 `drag` 一次性并进 `base`，从而在不跳帧的前提下
 * 把「跟手位置」交棒给「槽位补间」。
 */
@Composable
private fun StackCard(
    note: PaperNote,
    target: Offset,
    targetScale: Float,
    isFront: Boolean,
    isDeparting: Boolean,
    canSwap: Boolean,
    cardWidth: Dp,
    cardHeight: Dp,
    haptics: HapticFeedback,
    thresholds: DragThresholds,
    /** 参数为 true 表示这次换卡要演示「退场卡片滑回队尾」。 */
    onSwap: (withDepartAnimation: Boolean) -> Unit,
    onDepartSettled: () -> Unit,
) {
    // 初值直接取 target：否则首帧会停在原点，所有卡片先叠在中心闪一下
    val base = remember { Animatable(target, Offset.VectorConverter) }
    // 手指位移用普通状态：awaitEachGesture 是受限协程作用域，
    // 在里面只能调用它自己的挂起函数，碰不了 Animatable.snapTo/animateTo。
    // 于是手势只写状态，补间一律交给 LaunchedEffect / scope。
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 换卡松手后置位：让 LaunchedEffect 先把「松手位置」并进 base 再补间，
    // 这样从「跟手」切到「滑向队尾」之间没有一帧跳变。
    var handoffPending by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = SwapDurationMs, easing = SwapEasing),
        label = "card-scale",
    )

    // 文字只在前排显示：后排只露纸边，可避免错开的边缘出现被裁断的字，
    // 同时减轻绘制量。退场卡片让文字随补间一起淡出，读起来像「内容沉进纸堆」。
    val textAlpha by animateFloatAsState(
        targetValue = if (isFront) 1f else 0f,
        animationSpec = tween(durationMillis = if (isDeparting) SwapDurationMs else 220),
        label = "card-text",
    )

    // 退场途中保持「被抬起」的投影，落回队尾时再压平
    val elevation by animateFloatAsState(
        targetValue = if (isFront || isDeparting) 10f else 3f,
        animationSpec = tween(durationMillis = SwapDurationMs, easing = SwapEasing),
        label = "card-elevation",
    )

    val currentOnSwap by rememberUpdatedState(onSwap)
    val currentOnDepartSettled by rememberUpdatedState(onDepartSettled)

    LaunchedEffect(target) {
        if (handoffPending) {
            // 两次写入在同一帧提交：画面位置 = (base+drag) + 0，与松手瞬间完全一致
            base.snapTo(base.value + dragOffset)
            dragOffset = Offset.Zero
            handoffPending = false
        }
        // 退场与让位共用同一条补间：前排前移、队尾卡片归位是同一个动作的两端
        base.animateTo(target, tween(durationMillis = SwapDurationMs, easing = SwapEasing))
        if (isDeparting) currentOnDepartSettled()
    }

    Surface(
        modifier = Modifier
            .size(cardWidth, cardHeight)
            .graphicsLayer {
                val offset = base.value + dragOffset
                translationX = offset.x
                translationY = offset.y
                scaleX = scale
                scaleY = scale
                // 拖拽时轻微倾斜：Compose 没有 skew，用旋转近似纸片被带动的感觉
                rotationZ = if (dragging) dragOffset.x * 0.02f else 0f
            }
            .then(
                if (canSwap) {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startTime = System.currentTimeMillis()
                            var total = Offset.Zero
                            dragging = true

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                val delta = change.positionChange()
                                if (delta != Offset.Zero) {
                                    total += delta
                                    dragOffset = Offset(
                                        rubberBand(total.x),
                                        rubberBand(total.y),
                                    )
                                    change.consume()
                                }
                                if (!change.pressed) break
                            }

                            val distance =
                                hypot(total.x.toDouble(), total.y.toDouble()).toFloat()
                            val elapsed =
                                (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speed = distance / elapsed

                            // 轻点：位移小于 slop。走「默认换卡」——
                            // 旧卡立刻被新前排盖住，只剩下层卡片自然上移，不做回底演示。
                            val isTap = distance < thresholds.tapSlop
                            // 甩出：位移或速度够大。走「完整演示回到底层」。
                            val isFling = !isTap && (
                                distance > thresholds.flingDistance ||
                                    (speed > thresholds.flingSpeed &&
                                        distance > thresholds.fastDistance)
                                )

                            dragging = false

                            when {
                                isTap -> {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.SegmentFrequentTick,
                                    )
                                    // 不演示退场：旧卡立刻被新前排盖住，
                                    // 由绘制顺序（前排在上）自然遮挡
                                    handoffPending = true
                                    currentOnSwap(false)
                                }

                                isFling -> {
                                    haptics.performHapticFeedback(
                                        HapticFeedbackType.SegmentFrequentTick,
                                    )
                                    // 演示退场：补间期间它排在最底层，
                                    // 从松手点完整滑回队尾，全程可见
                                    handoffPending = true
                                    currentOnSwap(true)
                                }

                                else -> {
                                    // 位移不够：平滑弹回原位，不换卡
                                    val from = dragOffset
                                    scope.launch {
                                        animate(
                                            initialValue = 1f,
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.75f,
                                                stiffness = 380f,
                                            ),
                                        ) { progress, _ ->
                                            dragOffset = from * progress
                                        }
                                        dragOffset = Offset.Zero
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            ),
        shape = RoundedCornerShape(CardRadius),
        // 纯白卡面，不描边：与 #F5F5F5 底色的层次只靠色差与投影
        color = PaperWhite,
        shadowElevation = elevation.dp,
    ) {
        if (textAlpha > 0.01f) {
            Box(modifier = Modifier.graphicsLayer { alpha = textAlpha }) {
                NoteContent(note = note)
            }
        }
    }
}

@Composable
private fun NoteContent(note: PaperNote) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 正文占据卡片中部，超出时省略
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = note.text,
                color = Ink,
                fontSize = 15.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 出处贴在正文下方；未写出处时不展示
        note.source?.let { source ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = "— $source",
                color = InkMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
