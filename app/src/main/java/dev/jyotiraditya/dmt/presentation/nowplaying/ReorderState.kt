package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Drags a lazy list row by its key, swapping it past other rows and scrolling at the edges. */
@Stable
internal class ReorderState(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val haptics: HapticFeedback,
    private val edge: Float,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDrop: (key: Any) -> Unit,
) {
    private var draggedKey by mutableStateOf<Any?>(null)
    private var settlingKey by mutableStateOf<Any?>(null)
    private var startOffset = 0f
    private var delta by mutableFloatStateOf(0f)
    private val settle = Animatable(0f)
    private var swappedAt: LazyListLayoutInfo? = null
    private var job: Job? = null

    /** The key of the row that is dragged or still settling into its place. */
    val raisedKey: Any?
        get() = draggedKey ?: settlingKey

    /** How far the row with [key] is drawn from its place in the list. */
    fun translation(key: Any): Float = when (key) {
        draggedKey -> startOffset + delta - (item(key)?.offset ?: return 0f)
        settlingKey -> settle.value
        else -> 0f
    }

    /** Picks up the row with [key] and scrolls the list while it is held near an edge. */
    fun start(key: Any) {
        val item = item(key) ?: return
        job?.cancel()
        settlingKey = null
        draggedKey = key
        startOffset = item.offset.toFloat()
        delta = 0f
        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        job = scope.launch {
            while (true) {
                val step = edgeStep()
                if (step != 0f) {
                    listState.scrollBy(step)
                    swapIfOver()
                }
                withFrameNanos {}
            }
        }
    }

    /** Moves the dragged row by [dy] pixels. */
    fun drag(dy: Float) {
        if (draggedKey == null) return
        delta += dy
        swapIfOver()
    }

    /** Drops the dragged row and lets it settle into its place. */
    fun end() {
        val key = draggedKey ?: return
        val offset = translation(key)
        job?.cancel()
        draggedKey = null
        settlingKey = key
        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
        job = scope.launch {
            settle.snapTo(offset)
            settle.animateTo(0f)
            settlingKey = null
        }
        onDrop(key)
    }

    private fun item(key: Any?) =
        listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** Swaps the dragged row with the row under its middle, once the last swap is laid out. */
    private fun swapIfOver() {
        val info = listState.layoutInfo
        if (info === swappedAt) return
        val dragged = item(draggedKey) ?: return
        val middle = startOffset + delta + dragged.size / 2f
        val target =
            info.visibleItemsInfo.firstOrNull {
                it.key != draggedKey && middle >= it.offset && middle < it.offset + it.size
            } ?: return
        val first = listState.firstVisibleItemIndex
        if (dragged.index == first || target.index == first) {
            listState.requestScrollToItem(first, listState.firstVisibleItemScrollOffset)
        }
        swappedAt = info
        onMove(dragged.index, target.index)
        haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    /** Pixels to scroll this frame while the dragged row is held at the top or bottom edge. */
    private fun edgeStep(): Float {
        val info = listState.layoutInfo
        val dragged = item(draggedKey) ?: return 0f
        val top = startOffset + delta
        val bottom = top + dragged.size
        val overlap =
            when {
                delta < 0 && top < info.viewportStartOffset + edge ->
                    top - info.viewportStartOffset - edge

                delta > 0 && bottom > info.viewportEndOffset - edge ->
                    bottom - info.viewportEndOffset + edge

                else -> 0f
            }
        return overlap.coerceIn(-edge, edge) / 10f
    }
}

/** A [ReorderState] for [listState] that calls [onMove] for each swap and [onDrop] on release. */
@Composable
internal fun rememberReorderState(
    listState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
    onDrop: (key: Any) -> Unit,
): ReorderState {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val edge = with(LocalDensity.current) { 48.dp.toPx() }
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnDrop by rememberUpdatedState(onDrop)
    return remember(listState, scope, haptics, edge) {
        ReorderState(
            listState = listState,
            scope = scope,
            haptics = haptics,
            edge = edge,
            onMove = { from, to -> currentOnMove(from, to) },
            onDrop = { key -> currentOnDrop(key) },
        )
    }
}

/** Lets this handle drag the row with [key] in [state]. */
internal fun Modifier.reorderHandle(state: ReorderState, key: Any): Modifier =
    pointerInput(state, key) {
        detectDragGestures(
            onDragStart = { state.start(key) },
            onDragEnd = state::end,
            onDragCancel = state::end,
        ) { change, amount ->
            change.consume()
            state.drag(amount.y)
        }
    }
