package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.SectionGap
import dev.jyotiraditya.dmt.ui.theme.Surface

private const val PULSE_MS = 900
private const val PULSE_LOW = 0.45f

private const val SHELF_COUNT = 2
private const val TILES_PER_SHELF = 4
private const val ROW_COUNT = 8

private val TileGap = 12.dp
private val CaptionHeight = 14.dp
private val RowArt = 56.dp

/** An opacity easing between dim and full, for placeholders while content loads. */
@Composable
private fun rememberPulse(): State<Float> = rememberInfiniteTransition(label = "placeholderPulse")
    .animateFloat(
        initialValue = 1f,
        targetValue = PULSE_LOW,
        animationSpec =
            infiniteRepeatable(
                tween(PULSE_MS, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
        label = "pulse",
    )

private fun Modifier.pulsing(pulse: State<Float>): Modifier = graphicsLayer { alpha = pulse.value }

@Composable
private fun Block(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width, height).background(Surface))
}

/** A quiet status line above placeholders, announced to screen readers. */
@Composable
private fun LoadingLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = Muted,
        modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/** Shelves of cover placeholders, shown on the home page while the library is read. */
@Composable
fun ShelfPlaceholders(text: String, cover: Dp, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Column(modifier = modifier.padding(top = SectionGap / 2)) {
        LoadingLabel(text = text, modifier = Modifier.padding(horizontal = Margin))
        Column(modifier = Modifier.pulsing(pulse)) {
            repeat(SHELF_COUNT) {
                Block(
                    width = cover,
                    height = 24.dp,
                    modifier = Modifier.padding(start = Margin, top = SectionGap, bottom = 16.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TileGap),
                    modifier =
                        Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState(), enabled = false)
                            .padding(horizontal = Margin),
                ) {
                    repeat(TILES_PER_SHELF) {
                        Column {
                            Block(width = cover, height = cover)
                            Block(
                                width = cover * 0.7f,
                                height = CaptionHeight,
                                modifier = Modifier.padding(top = 10.dp),
                            )
                            Block(
                                width = cover * 0.45f,
                                height = CaptionHeight,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Track row placeholders, shown in the library while it is read. */
@Composable
fun RowPlaceholders(text: String, modifier: Modifier = Modifier) {
    val pulse = rememberPulse()
    Column(modifier = modifier.padding(top = 16.dp)) {
        LoadingLabel(text = text, modifier = Modifier.padding(horizontal = Margin))
        Column(modifier = Modifier.padding(top = 12.dp).pulsing(pulse)) {
            repeat(ROW_COUNT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Margin, vertical = 8.dp),
                ) {
                    Block(width = RowArt, height = RowArt)
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Block(width = 180.dp, height = CaptionHeight)
                        Block(
                            width = 110.dp,
                            height = CaptionHeight,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
