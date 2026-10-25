package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.lyrics.Lyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

/** How long auto-scroll stays paused after the user scrolls by hand. */
private const val HAND_SCROLL_HOLD_MS = 3500L

/** Where the active line sits, as a fraction of the view height from the top. */
private const val ACTIVE_LINE_AT = 0.3f

/** Lyrics that scroll with playback and light each word as it is sung; tap a line to seek. */
@Composable
internal fun LyricsView(
    lyrics: Lyrics,
    trackId: String?,
    position: State<Long>,
    durationMs: Long,
    romanized: Boolean,
    colors: LyricColors,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val activeIndex by
        remember(lyrics) {
            derivedStateOf { if (lyrics.synced) lyrics.lines.indexAt(position.value) else -1 }
        }

    var handHeld by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions
            .filterIsInstance<DragInteraction.Start>()
            .collectLatest {
                handHeld = true
                delay(HAND_SCROLL_HOLD_MS)
                handHeld = false
            }
    }
    LaunchedEffect(trackId) { listState.scrollToItem(0) }
    LaunchedEffect(lyrics, listState) {
        snapshotFlow { if (handHeld) -1 else activeIndex }
            .collect { index ->
                if (index < 0) return@collect
                val viewport = listState.layoutInfo.viewportSize.height
                listState.animateScrollToItem(
                    index,
                    scrollOffset = -(viewport * ACTIVE_LINE_AT).toInt(),
                )
            }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = 32.dp, bottom = 360.dp),
        modifier = modifier.fadingEdges(listState),
    ) {
        items(lyrics.lines) { line ->
            val seekable = lyrics.synced && durationMs > 0 && line.startMs >= 0
            LyricLineView(
                line = line,
                position = position,
                synced = lyrics.synced,
                romanized = romanized,
                colors = colors,
                onSeek =
                    if (seekable) {
                        { onSeek((line.startMs.toFloat() / durationMs).coerceIn(0f, 1f)) }
                    } else {
                        null
                    },
            )
        }
    }
}

private const val TOP_FADE = 0.08f
private const val BOTTOM_FADE = 0.16f

/** Fades the list out towards an edge while there is more to scroll to beyond it. */
@Composable
private fun Modifier.fadingEdges(state: LazyListState): Modifier {
    val top = animateFloatAsState(if (state.canScrollBackward) 1f else 0f, label = "topFade")
    val bottom = animateFloatAsState(if (state.canScrollForward) 1f else 0f, label = "bottomFade")
    return graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
        drawContent()
        drawRect(
            brush =
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 1f - top.value),
                    TOP_FADE to Color.Black,
                    1f - BOTTOM_FADE to Color.Black,
                    1f to Color.Black.copy(alpha = 1f - bottom.value),
                ),
            blendMode = BlendMode.DstIn,
        )
    }
}
