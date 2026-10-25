package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A gap that keeps [min], takes [share] of the spare height, and grows to at most [max]. */
internal data class Gap(val min: Dp, val share: Float, val max: Dp)

/** The playing page's gaps, from the top edge to the bottom edge. */
internal object PlayingGaps {
    val Top = Gap(min = 8.dp, share = 1f, max = 40.dp)
    val CoverToTitle = Gap(min = 20.dp, share = 1f, max = 36.dp)
    val TitleToSeek = Gap(min = 12.dp, share = 0.5f, max = 20.dp)
    val SeekToTransport = Gap(min = 16.dp, share = 1f, max = 36.dp)
    val TransportToExtras = Gap(min = 16.dp, share = 1f, max = 32.dp)
    val Bottom = Gap(min = 12.dp, share = 0.5f, max = 24.dp)

    val all = listOf(Top, CoverToTitle, TitleToSeek, SeekToTransport, TransportToExtras, Bottom)
}

/**
 * Cover, title, seek bar, transport and extras in a column: the cover takes the largest square left
 * after the controls, the gaps share what remains, and any excess centres the page.
 */
@Composable
internal fun PlayingLayout(
    cover: (@Composable () -> Unit)?,
    title: @Composable () -> Unit,
    seek: @Composable () -> Unit,
    transport: @Composable () -> Unit,
    extras: @Composable () -> Unit,
    maxCover: Dp,
    modifier: Modifier = Modifier,
) {
    val slots = listOfNotNull(cover, title, seek, transport, extras)
    Layout(contents = slots, modifier = modifier) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = Constraints(maxWidth = width)
        val controls = measurables.takeLast(4).map { it.single().measure(loose) }
        val controlsHeight = controls.sumOf { it.height }

        val gaps =
            if (cover != null) PlayingGaps.all else PlayingGaps.all - PlayingGaps.CoverToTitle
        val minGaps = gaps.sumOf { it.min.roundToPx() }

        val coverPlaceable =
            if (cover != null) {
                val room = height - controlsHeight - minGaps
                val side = minOf(width, room, maxCover.roundToPx()).coerceAtLeast(0)
                measurables.first().single().measure(Constraints.fixed(side, side))
            } else {
                null
            }

        val blocks = listOfNotNull(coverPlaceable) + controls
        val spare = (height - blocks.sumOf { it.height } - minGaps).coerceAtLeast(0)
        val sizes =
            distribute(
                spare,
                gaps.map { it.min.roundToPx() to it.max.roundToPx() },
                gaps.map { it.share },
            )
        val centring = (height - blocks.sumOf { it.height } - sizes.sum()).coerceAtLeast(0) / 2

        layout(width, height) {
            var y = centring + sizes.first()
            blocks.forEachIndexed { index, placeable ->
                placeable.place((width - placeable.width) / 2, y)
                y += placeable.height + sizes[index + 1]
            }
        }
    }
}

/** Shares [spare] among gaps by [shares]; a gap at its maximum passes its portion to the rest. */
private fun distribute(spare: Int, bounds: List<Pair<Int, Int>>, shares: List<Float>): List<Int> {
    val sizes = bounds.map { it.first.toFloat() }.toMutableList()
    var left = spare.toFloat()
    val open = bounds.indices.toMutableSet()
    while (left > 0.5f && open.isNotEmpty()) {
        val total = open.sumOf { shares[it].toDouble() }.toFloat()
        var used = 0f
        for (index in open.toList()) {
            val room = bounds[index].second - sizes[index]
            val portion = left * shares[index] / total
            val taken = minOf(portion, room)
            sizes[index] += taken
            used += taken
            if (taken >= room) open.remove(index)
        }
        left -= used
        if (used < 0.5f) break
    }
    return sizes.map { it.roundToInt() }
}
