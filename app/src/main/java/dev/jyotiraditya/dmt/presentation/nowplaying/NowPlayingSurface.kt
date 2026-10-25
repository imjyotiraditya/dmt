package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.adaptive.windowShape
import dev.jyotiraditya.dmt.ui.components.Artwork
import dev.jyotiraditya.dmt.ui.components.animateFieldAsState
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.theme.Surface
import kotlin.math.roundToInt

/**
 * The bottom sheet's content: the mini player while [sheet] rests at its [peek] and the full player
 * once it is expanded, with the cover carried across between them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSurface(
    state: DmtState,
    player: Player,
    sheet: SheetState,
    peek: Dp,
    field: Color,
    dispatch: (DmtAction) -> Unit,
    onExpand: (Boolean) -> Unit,
    onOpenArtist: () -> Unit,
) {
    val density = LocalDensity.current
    val pager = rememberPagerState(pageCount = { PlayerPage.entries.size })
    val wide = windowShape().wide
    val peekPx = with(density) { peek.toPx() }
    var height by remember { mutableIntStateOf(0) }
    // How far the sheet has risen from its peek, from 0 to 1, once it has been laid out.
    val progress = {
        val travel = height - peekPx
        if (travel <= 0f) 0f else (1f - sheet.requireOffset() / travel).coerceIn(0f, 1f)
    }

    // Returns to the playing page once collapsed.
    LaunchedEffect(pager) {
        snapshotFlow { progress() == 0f }
            .collect { collapsed ->
                if (collapsed) pager.scrollToPage(PlayerPage.PLAYING.ordinal)
            }
    }
    LaunchedEffect(state.lyrics, state.lyricsFetching) {
        val expanded = sheet.currentValue == SheetValue.Expanded
        if (state.lyrics != null && state.lyricsShowingFrom?.online == true && expanded) {
            pager.animateScrollToPage(PlayerPage.LYRICS.ordinal)
        }
    }

    val paint by animateFieldAsState(field)
    var content by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var coverInContent by remember { mutableStateOf<Rect?>(null) }
    val showFull by remember { derivedStateOf { progress() > 0.001f } }
    val showMini by remember { derivedStateOf { progress() < 0.4f } }
    val ownsSystemBars by remember { derivedStateOf { progress() > 0.5f } }
    val moving by remember { derivedStateOf { progress() > 0.001f && progress() < 0.999f } }
    val coverShown = wide || pager.currentPage == PlayerPage.PLAYING.ordinal
    val flightTarget = coverInContent?.takeIf { moving && coverShown }

    Box(
        modifier =
            Modifier.fillMaxSize()
                .onSizeChanged { height = it.height }
                .onPlaced { content = it }
                .drawBehind {
                    drawRect(lerp(Surface, paint, (progress() * 2.2f).coerceIn(0f, 1f)))
                },
    ) {
        if (showFull) {
            Box(
                modifier =
                    Modifier.fillMaxSize().graphicsLayer {
                        alpha = ((progress() - 0.25f) / 0.6f).coerceIn(0f, 1f)
                    },
            ) {
                NowPlayingScreen(
                    state = state,
                    player = player,
                    field = field,
                    dispatch = dispatch,
                    pager = pager,
                    ownsSystemBars = ownsSystemBars,
                    coverHidden = flightTarget != null,
                    onCoverPlaced = { cover ->
                        content
                            ?.takeIf { it.isAttached }
                            ?.let {
                                coverInContent = it.localBoundingBoxOf(cover, clipBounds = false)
                            }
                    },
                    onCollapse = { onExpand(false) },
                    onOpenArtist = onOpenArtist,
                )
            }
        }
        if (showMini) {
            MiniPlayer(
                state = state,
                player = player,
                field = field,
                coverHidden = flightTarget != null,
                modifier =
                    Modifier.graphicsLayer { alpha = (1f - progress() / 0.3f).coerceIn(0f, 1f) }
                        .pressable(
                            onClick = { onExpand(true) },
                            sink = 0.995f,
                            label = state.title,
                        ),
            )
        }
        if (flightTarget != null) {
            val from = MiniPlayerCover.bounds(density)
            // The cover in flight between the mini player and the full player.
            Artwork(
                cover = state.cover,
                modifier =
                    Modifier.layout { measurable, _ ->
                        val box = lerp(from, flightTarget, progress())
                        val side = box.width.roundToInt().coerceAtLeast(1)
                        val placeable = measurable.measure(Constraints.fixed(side, side))
                        layout(side, side) {
                            placeable.place(box.left.roundToInt(), box.top.roundToInt())
                        }
                    },
            )
        }
    }
}
