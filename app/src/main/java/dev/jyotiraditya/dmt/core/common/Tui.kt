package dev.jyotiraditya.dmt.core.common

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TuiNotice(
    error: String?,
    notice: String?,
    modifier: Modifier = Modifier,
    reserveSpace: Boolean = false,
) {
    val text = error ?: notice
    if (text == null && !reserveSpace) return
    Text(
        text = text.orEmpty(),
        style = MaterialTheme.typography.labelSmall,
        color = if (error != null) MaterialTheme.colorScheme.error else TuiDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(bottom = 4.dp),
    )
}

@Composable
fun rememberCursorAlpha(periodMs: Int = 1060): Float {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = periodMs
                1f at 0
                1f at periodMs / 2
                0f at periodMs / 2 + 1
                0f at periodMs
            },
        ),
        label = "cursorAlpha",
    )
    return alpha
}

@Composable
fun FitScaled(fitScale: Float, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density * fitScale, density.fontScale),
        content = content,
    )
}

private class PressFlash(private val scope: CoroutineScope) {
    private val flash = Animatable(0f)

    val value: Float
        get() = flash.value

    fun click() {
        scope.launch {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(220))
        }
    }
}

@Composable
private fun rememberPressFlash(): PressFlash {
    val scope = rememberCoroutineScope()
    return remember { PressFlash(scope) }
}

fun Modifier.tuiClickable(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = null,
        indication = null,
        onClick = onClick,
    )

@Composable
fun TuiPanel(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TuiLine)
            .background(TuiSurface.copy(alpha = 0.85f))
            .let { if (onClick != null) it.tuiClickable(onClick) else it }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        content = content,
    )
}

@Composable
fun TuiKey(
    label: String,
    modifier: Modifier = Modifier,
    bright: Boolean = false,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val flash = rememberPressFlash()
    val press = if (pressed) 1f else flash.value
    val restText = if (bright) TuiBg else TuiFg
    val restBorder = if (bright) TuiFg else TuiLine
    val restBg = if (bright) TuiFg else TuiSurface.copy(alpha = 0.4f)
    val pressText = if (bright) TuiFg else TuiBg
    val pressBorder = if (bright) TuiLine else TuiFg
    val pressBg = if (bright) TuiSurface.copy(alpha = 0.4f) else TuiFg
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = lerp(restText, pressText, press),
        textAlign = TextAlign.Center,
        modifier = modifier
            .border(1.dp, lerp(restBorder, pressBorder, press))
            .background(lerp(restBg, pressBg, press))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                flash.click()
                onClick()
            }
            .padding(
                horizontal = if (big) 22.dp else 14.dp,
                vertical = if (big) 15.dp else 11.dp,
            ),
    )
}

@Composable
fun TuiTab(
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) TuiBg else TuiDim,
        modifier = modifier
            .border(1.dp, if (active) TuiFg else TuiLine)
            .background(if (active) TuiFg else TuiSurface.copy(alpha = 0.4f))
            .tuiClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    )
}

@Composable
fun TuiChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TuiDim,
        modifier = Modifier
            .border(1.dp, TuiLine)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
}

@Composable
fun TuiStatus(
    label: String,
    value: String,
    on: Boolean,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val flash = rememberPressFlash()
    val press = if (pressed) 1f else flash.value
    val restText = if (on) TuiBright else TuiDim
    val blink = if (busy) rememberCursorAlpha() else 1f
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, lerp(TuiLine, TuiFg, press))
            .background(lerp(TuiSurface.copy(alpha = 0.4f), TuiFg, press))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                flash.click()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(
                    when {
                        busy -> LocalAccent.current.copy(alpha = blink)
                        on -> LocalAccent.current
                        else -> TuiFaint
                    },
                ),
        )
        Text(
            text = " $label:$value",
            style = MaterialTheme.typography.labelMedium,
            color = lerp(restText, TuiBg, press),
        )
    }
}

@Composable
fun Hairline(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp),
    ) {
        drawLine(
            TuiFaint,
            Offset(0f, 1.dp.toPx()),
            Offset(size.width, 1.dp.toPx()),
            2.dp.toPx(),
        )
        drawLine(
            TuiFg,
            Offset(0f, 1.dp.toPx()),
            Offset(size.width * fraction.coerceIn(0f, 1f), 1.dp.toPx()),
            2.dp.toPx(),
        )
    }
}

@Composable
fun ThinSlider(
    fraction: Float,
    onScrub: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val filledColor = TuiFg
    val emptyColor = TuiFaint
    val paint = remember {
        Paint().apply {
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
            textSize = 28f
        }
    }
    Box(
        modifier = modifier
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        last = (offset.x / size.width).coerceIn(0f, 1f)
                        onScrub(last)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        last = (change.position.x / size.width).coerceIn(0f, 1f)
                        onScrub(last)
                    },
                    onDragEnd = { onSeek(last) },
                    onDragCancel = { onScrub(null) },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val advance = paint.measureText("█")
            val cols = (size.width / advance).toInt().coerceAtLeast(1)
            val filledCols = (cols * fraction.coerceIn(0f, 1f)).toInt()
            val baseline = size.height / 2 + paint.textSize * 0.35f
            drawContext.canvas.nativeCanvas.apply {
                for (col in 0 until cols) {
                    val filled = col < filledCols
                    val glyph = if (filled) "█" else "░"
                    paint.color = when {
                        col == filledCols - 1 -> accent.toArgb()
                        filled -> filledColor.toArgb()
                        else -> emptyColor.toArgb()
                    }
                    drawText(
                        glyph,
                        col * advance,
                        baseline,
                        paint,
                    )
                }
            }
        }
    }
}
