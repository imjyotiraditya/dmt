package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Ink

private val SeekTarget = 40.dp
private val SeekBarHeight = 6.dp
private val ThumbRest = 14.dp
private val ThumbHeld = 22.dp
private const val UNPLAYED_ALPHA = 0.18f

/** A thin bar filled to [fraction], read while drawing. */
@Composable
fun FillBar(
    fraction: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = Ink,
    track: Color = Ink.copy(alpha = 0.14f),
    height: Dp = 4.dp,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawRect(track)
        drawRect(color, size = Size(size.width * fraction().coerceIn(0f, 1f), size.height))
    }
}

/**
 * A seek bar with a square thumb that grows while held; a drag scrubs and seeks on release.
 * [fraction] is followed without recomposing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeekBar(
    fraction: () -> Float,
    onScrub: (Float?) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    scrubbing: Boolean = false,
    ink: Color = Ink,
) {
    val thumb by animateDpAsState(if (scrubbing) ThumbHeld else ThumbRest, label = "seekThumb")
    val scrub by rememberUpdatedState(onScrub)
    val seek by rememberUpdatedState(onSeek)
    val slider = remember { SliderState() }
    slider.onValueChange = { target ->
        slider.value = target
        scrub(target)
    }
    slider.onValueChangeFinished = { seek(slider.value) }
    LaunchedEffect(slider) {
        snapshotFlow { fraction().coerceIn(0f, 1f) }.collect { slider.value = it }
    }
    Slider(
        state = slider,
        modifier = modifier.fillMaxWidth().height(SeekTarget),
        thumb = {
            Canvas(modifier = Modifier.size(width = ThumbRest, height = ThumbHeld)) {
                val side = thumb.toPx()
                drawRect(
                    ink,
                    topLeft = Offset((size.width - side) / 2, (size.height - side) / 2),
                    size = Size(side, side),
                )
            }
        },
        track = {
            Canvas(modifier = Modifier.fillMaxWidth().height(SeekBarHeight)) {
                // Runs under the thumb's half at each end, so the bar spans the full width.
                val inset = ThumbRest.toPx() / 2
                val played = inset + size.width * slider.coercedValueAsFraction
                drawRect(
                    ink.copy(alpha = UNPLAYED_ALPHA),
                    topLeft = Offset(-inset, 0f),
                    size = Size(size.width + 2 * inset, size.height),
                )
                drawRect(ink, topLeft = Offset(-inset, 0f), size = Size(played, size.height))
            }
        },
    )
}
