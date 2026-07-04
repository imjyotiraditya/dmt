package dev.jyotiraditya.dmt.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface

fun Modifier.tuiClickable(onClick: () -> Unit): Modifier = this.clickable(
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
        content = content
    )
}

@Composable
fun TuiKey(
    label: String,
    bright: Boolean = false,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val inverted = bright != pressed
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (inverted) TuiBg else TuiFg,
        modifier = Modifier
            .border(1.dp, if (inverted) TuiFg else TuiLine)
            .background(if (inverted) TuiFg else TuiSurface.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (big) 22.dp else 14.dp,
                vertical = if (big) 15.dp else 11.dp
            )
    )
}

@Composable
fun TuiTab(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (active) TuiBg else TuiDim,
        modifier = Modifier
            .border(1.dp, if (active) TuiFg else TuiLine)
            .background(if (active) TuiFg else TuiSurface.copy(alpha = 0.4f))
            .tuiClickable(onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
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
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}

@Composable
fun TuiStatus(label: String, value: String, on: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(1.dp, if (pressed) TuiFg else TuiLine)
            .background(if (pressed) TuiFg else TuiSurface.copy(alpha = 0.4f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (on) LocalAccent.current else TuiFaint)
        )
        Text(
            text = " $label:$value",
            style = MaterialTheme.typography.labelMedium,
            color = when {
                pressed -> TuiBg
                on -> TuiBright
                else -> TuiDim
            }
        )
    }
}

@Composable
fun Hairline(fraction: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawLine(TuiFaint, Offset(0f, 1.dp.toPx()), Offset(size.width, 1.dp.toPx()), 2.dp.toPx())
        drawLine(
            TuiFg,
            Offset(0f, 1.dp.toPx()),
            Offset(size.width * fraction.coerceIn(0f, 1f), 1.dp.toPx()),
            2.dp.toPx()
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
                    onDragCancel = { onScrub(null) }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height / 2
            val x = size.width * fraction.coerceIn(0f, 1f)
            drawLine(
                color = TuiFaint,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 9f))
            )
            drawLine(
                color = TuiFg,
                start = Offset(0f, y),
                end = Offset(x, y),
                strokeWidth = 2.dp.toPx()
            )
            val knob = 8.dp.toPx()
            drawRect(
                color = accent,
                topLeft = Offset((x - knob / 2).coerceIn(0f, size.width - knob), y - knob / 2),
                size = Size(knob, knob)
            )
        }
    }
}

