package dev.jyotiraditya.dmt.presentation.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlin.math.roundToInt

private const val EXPAND_VELOCITY_THRESHOLD = 150f

@Composable
fun PlayerSheet(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    anchor: Rect?,
    hidden: Boolean,
    onInfo: () -> Unit,
    onQueue: () -> Unit,
) {
    if (state.nowPlayingId == null || anchor == null) return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()
        val miniHeightPx = anchor.height
        val collapsedY = anchor.top

        val scope = rememberCoroutineScope()
        val fraction = remember { Animatable(if (state.expanded) 1f else 0f) }
        val sheetY = remember { Animatable(if (state.expanded) 0f else collapsedY) }
        val mutex = remember { MutatorMutex() }
        val sheetSpec = remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 380f)
        }
        val velocityTracker = remember { VelocityTracker() }
        val expandedNow = rememberUpdatedState(state.expanded)

        suspend fun snapSheet(y: Float, f: Float) {
            mutex.mutate {
                sheetY.snapTo(y)
                fraction.snapTo(f)
            }
        }

        suspend fun animateSheet(
            targetExpanded: Boolean,
            spec: AnimationSpec<Float> = sheetSpec,
            initialVelocity: Float = 0f,
        ) {
            val targetY = if (targetExpanded) 0f else collapsedY
            val targetF = if (targetExpanded) 1f else 0f
            val settled = sheetY.targetValue == targetY &&
                    fraction.targetValue == targetF &&
                    (sheetY.isRunning || sheetY.value == targetY) &&
                    (fraction.isRunning || fraction.value == targetF)
            if (settled) return
            mutex.mutate {
                coroutineScope {
                    launch { sheetY.animateTo(targetY, spec, initialVelocity) }
                    launch {
                        fraction.animateTo(
                            targetF,
                            spec,
                            initialVelocity / collapsedY.coerceAtLeast(1f),
                        )
                    }
                }
            }
        }

        LaunchedEffect(collapsedY) {
            if (!sheetY.isRunning && !fraction.isRunning) {
                snapSheet(collapsedY * (1f - fraction.value), fraction.value)
            }
        }

        LaunchedEffect(state.expanded) {
            animateSheet(state.expanded)
        }

        PredictiveBackHandler(enabled = state.expanded) { progress ->
            try {
                progress.collect { event ->
                    snapSheet(collapsedY * event.progress, 1f - event.progress)
                }
                dispatch(DmtAction.Expand(false))
                animateSheet(false)
            } catch (_: CancellationException) {
                animateSheet(true)
            }
        }

        if (hidden) return@BoxWithConstraints

        val renderFull by remember { derivedStateOf { fraction.value > 0.005f } }
        val miniOnTop by remember { derivedStateOf { fraction.value < 0.5f } }

        fun settleDrag(accumulated: Float, minDragPx: Float) {
            val velocity = velocityTracker.calculateVelocity().y
            val f = fraction.value
            val expand = when {
                expandedNow.value && accumulated <= 0f -> true
                abs(accumulated) > minDragPx -> accumulated < 0
                abs(velocity) > EXPAND_VELOCITY_THRESHOLD -> velocity < 0
                else -> f > 0.5f
            }
            scope.launch {
                if (expand) {
                    launch { animateSheet(true, initialVelocity = velocity) }
                    dispatch(DmtAction.Expand(true))
                } else {
                    launch { animateSheet(false, initialVelocity = velocity) }
                    dispatch(DmtAction.Expand(false))
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(0, sheetY.value.roundToInt()) }
                .layout { measurable, constraints ->
                    val f = fraction.value
                    val ty = sheetY.value
                    val padStart = lerp(anchor.left, 0f, f).roundToInt().coerceAtLeast(0)
                    val padEnd = lerp(containerWidthPx - anchor.right, 0f, f)
                        .roundToInt()
                        .coerceAtLeast(0)
                    val bottomEdge = lerp(collapsedY + miniHeightPx, containerHeightPx, f)
                    val cardHeight =
                        (if (ty <= collapsedY) bottomEdge - ty else bottomEdge - collapsedY)
                            .roundToInt()
                            .coerceAtLeast(0)
                    val cardWidth = (constraints.maxWidth - padStart - padEnd).coerceAtLeast(0)
                    val placeable = measurable.measure(Constraints.fixed(cardWidth, cardHeight))
                    layout(constraints.maxWidth, cardHeight) {
                        placeable.placeRelative(padStart, 0)
                    }
                }
                .drawBehind {
                    val f = fraction.value
                    drawRect(lerp(TuiSurface.copy(alpha = 0.85f), TuiBg, f))
                    if (f < 1f) {
                        val inset = 0.5.dp.toPx()
                        drawRect(
                            color = TuiLine.copy(alpha = TuiLine.alpha * (1f - f)),
                            topLeft = Offset(inset, inset),
                            size = Size(
                                size.width - inset * 2,
                                size.height - inset * 2,
                            ),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                    }
                }
                .clipToBounds()
                .sheetDrag(
                    scope = scope,
                    sheetY = sheetY,
                    fraction = fraction,
                    collapsedY = collapsedY,
                    velocityTracker = velocityTracker,
                    onSnap = ::snapSheet,
                    onSettle = ::settleDrag,
                )
                .combinedClickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { if (fraction.value < 0.5f) dispatch(DmtAction.Expand(true)) },
                    onLongClick = { if (fraction.value < 0.5f) onQueue() },
                ),
        ) {
            Box(
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        Constraints.fixed(
                            constraints.maxWidth, containerHeightPx.roundToInt()
                        ),
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.placeRelative(0, 0)
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(with(LocalDensity.current) { miniHeightPx.toDp() })
                        .zIndex(if (miniOnTop) 1f else 0f)
                        .graphicsLayer {
                            alpha = (1f - fraction.value * 2f).coerceIn(0f, 1f)
                        },
                ) {
                    MiniPlayer(state = state, dispatch = dispatch)
                }
                if (renderFull) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (miniOnTop) 0f else 1f)
                            .graphicsLayer {
                                val entrance = (fraction.value - 0.25f).coerceIn(0f, 0.75f) / 0.75f
                                alpha = entrance
                                translationY = lerp(24.dp.toPx(), 0f, entrance)
                            },
                    ) {
                        ExpandedPlayer(
                            state = state,
                            dispatch = dispatch,
                            onInfo = onInfo,
                            onQueue = onQueue,
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.sheetDrag(
    scope: CoroutineScope,
    sheetY: Animatable<Float, AnimationVector1D>,
    fraction: Animatable<Float, AnimationVector1D>,
    collapsedY: Float,
    velocityTracker: VelocityTracker,
    onSnap: suspend (Float, Float) -> Unit,
    onSettle: (Float, Float) -> Unit,
): Modifier = pointerInput(collapsedY) {
    var initialF = 0f
    var initialY = 0f
    var accumulated = 0f
    var snapJob: Job? = null

    fun release() {
        snapJob?.cancel()
        snapJob = null
        onSettle(accumulated, 5.dp.toPx())
        accumulated = 0f
    }

    detectVerticalDragGestures(
        onDragStart = {
            snapJob?.cancel()
            snapJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                sheetY.stop()
                fraction.stop()
            }
            velocityTracker.resetTracking()
            initialF = fraction.value
            initialY = sheetY.value
            accumulated = 0f
        },
        onVerticalDrag = { change, dragAmount ->
            change.consume()
            accumulated += dragAmount
            val newY = (sheetY.value + dragAmount).coerceIn(0f, collapsedY)
            val ratio = (initialY - newY) / collapsedY.coerceAtLeast(1f)
            val newF = (initialF + ratio).coerceIn(0f, 1f)
            snapJob?.cancel()
            snapJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                onSnap(newY, newF)
            }
            velocityTracker.addPosition(change.uptimeMillis, change.position)
        },
        onDragEnd = { release() },
        onDragCancel = { release() },
    )
}
