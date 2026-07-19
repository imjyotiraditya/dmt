package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.AsciiCover
import dev.jyotiraditya.dmt.core.common.CursorTitle
import dev.jyotiraditya.dmt.core.common.FitScaled
import dev.jyotiraditya.dmt.core.common.ThinSlider
import dev.jyotiraditya.dmt.core.common.TuiChip
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.TuiNotice
import dev.jyotiraditya.dmt.core.common.TuiPanel
import dev.jyotiraditya.dmt.core.common.TuiStatus
import dev.jyotiraditya.dmt.core.common.fitScaleFor
import dev.jyotiraditya.dmt.core.common.isCompactWindow
import dev.jyotiraditya.dmt.core.common.isLandscapeWindow
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.core.common.windowDpSize
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiSurface
import dev.jyotiraditya.dmt.util.asTime
import kotlin.math.abs

private val PLAYER_CHIP_LABELS = setOf("FMT", "BIT", "RATE", "KBPS", "VBR")

@Composable
fun ExpandedPlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
    onQueue: () -> Unit,
) {
    val windowSize = windowDpSize()
    val landscape = isLandscapeWindow()
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
    ) {
        val compact = isCompactWindow()
        val fitScale =
            if (landscape) {
                fitScaleFor(designHeightDp = 480f, minScale = 0.6f)
            } else {
                fitScaleFor(designHeightDp = windowSize.width.value + 560f, minScale = 0.75f)
            }
        when {
            compact ->
                PortraitPlayer(
                    state = state,
                    dispatch = dispatch,
                    onInfo = onInfo,
                    onQueue = onQueue,
                    showLyrics = showLyrics,
                    onToggleLyrics = { showLyrics = !showLyrics },
                    compact = true,
                )

            landscape -> FitScaled(fitScale) {
                LandscapePlayer(
                    state = state,
                    dispatch = dispatch,
                    onInfo = onInfo,
                    onQueue = onQueue,
                    showLyrics = showLyrics,
                    onToggleLyrics = { showLyrics = !showLyrics },
                )
            }

            else -> FitScaled(fitScale) {
                PortraitPlayer(
                    state = state,
                    dispatch = dispatch,
                    onInfo = onInfo,
                    onQueue = onQueue,
                    showLyrics = showLyrics,
                    onToggleLyrics = { showLyrics = !showLyrics },
                )
            }
        }
    }
}

@Composable
private fun PortraitPlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
    onQueue: () -> Unit,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
    compact: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlayerHeader(
            dispatch = dispatch,
            onInfo = onInfo,
        )

        if (!compact) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(5f)
                    .padding(top = 14.dp)
                    .fillMaxWidth(),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.aspectRatio(1f),
                ) {
                    ArtSlot(state, dispatch, showLyrics)
                }
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
        ControlsBlock(
            state = state,
            dispatch = dispatch,
            showLyrics = showLyrics,
            onToggleLyrics = onToggleLyrics,
        )

        Spacer(modifier = Modifier.weight(1f))

        QueueFooter(state, onQueue)
    }
}

@Composable
private fun LandscapePlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
    onQueue: () -> Unit,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        PlayerRail(
            dispatch = dispatch,
            onInfo = onInfo,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 12.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = true),
        ) {
            ArtSlot(state, dispatch, showLyrics)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                ControlsBlock(
                    state = state,
                    dispatch = dispatch,
                    showLyrics = showLyrics,
                    onToggleLyrics = onToggleLyrics,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            QueueFooter(state, onQueue)
        }
    }
}

@Composable
private fun PlayerRail(
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(IntrinsicSize.Max)
            .padding(top = 12.dp),
    ) {
        TuiKey(
            label = stringResource(R.string.close),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            onClick = { dispatch(DmtAction.Expand(false)) },
        )

        Text(
            text = stringResource(R.string.now_playing).replace(' ', '\n'),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
        )

        TuiKey(
            label = stringResource(R.string.info),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            onClick = onInfo,
        )
    }
}

@Composable
private fun ControlsBlock(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    TrackMeta(state)
    SeekRow(state, dispatch)
    TransportRow(state, dispatch)
    StatusRow(
        state = state,
        dispatch = dispatch,
        showLyrics = showLyrics,
        onToggleLyrics = onToggleLyrics,
    )
    TuiNotice(
        error = state.error,
        notice = state.notice,
        modifier = Modifier.padding(top = 8.dp),
        reserveSpace = true,
    )
}

@Composable
private fun PlayerHeader(
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            TuiKey(
                label = stringResource(R.string.close),
                onClick = { dispatch(DmtAction.Expand(false)) },
            )
        }
        Text(
            text = stringResource(R.string.now_playing),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
            modifier = Modifier.align(Alignment.Center),
        )
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            TuiKey(
                label = stringResource(R.string.info),
                onClick = onInfo,
            )
        }
    }
}

@Composable
private fun ArtSlot(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    showLyrics: Boolean,
    modifier: Modifier = Modifier,
) {
    val lyrics = state.lyrics
    if (showLyrics && lyrics != null) {
        val aspect = state.cover?.let { it.width.toFloat() / it.height } ?: 1f
        LyricsPanel(
            lyrics = lyrics,
            trackId = state.nowPlayingId,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            romanized = state.settings.romanizedLyrics,
            contentAspect = aspect,
            onSeekFraction = {
                dispatch(DmtAction.Seek(it))
                if (!state.isPlaying) dispatch(DmtAction.TogglePlay)
            },
            modifier = modifier,
        )
    } else {
        CoverPanel(state, modifier)
    }
}

@Composable
private fun CoverPanel(state: DmtState, modifier: Modifier = Modifier) {
    val rawArt = state.artRaw

    TuiPanel(modifier = modifier) {
        when {
            state.settings.rawArt && rawArt != null -> {
                val image = remember(rawArt) { rawArt.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .aspectRatio(rawArt.width.toFloat() / rawArt.height),
                )
            }

            state.cover != null -> {
                AsciiCover(
                    cover = state.cover,
                    playing = state.isPlaying,
                    wave = state.settings.wave,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                ) {
                    Text(
                        text = stringResource(R.string.no_cover),
                        style = MaterialTheme.typography.labelMedium,
                        color = TuiFaint,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackMeta(state: DmtState) {
    CursorTitle(
        text = state.title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 18.dp),
    )
    Text(
        text = listOf(state.artist, state.album)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .lowercase(),
        style = MaterialTheme.typography.bodyMedium,
        color = TuiDim,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 3.dp),
    )
    if (state.settings.listSpecs && state.tech.isNotEmpty()) {
        val chipScroll = rememberScrollState()
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    if (chipScroll.canScrollForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.88f to Color.White,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    if (chipScroll.canScrollBackward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                0.12f to Color.White,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
                .horizontalScroll(chipScroll),
        ) {
            state.tech
                .filter { it.label in PLAYER_CHIP_LABELS }
                .forEach { spec ->
                    TuiChip("${spec.label}:${spec.value}".lowercase())
                }
        }
    }
}

@Composable
private fun SeekRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    var seekPending by remember { mutableStateOf(false) }
    LaunchedEffect(state.positionMs) {
        if (!seekPending) return@LaunchedEffect
        val held = scrub ?: return@LaunchedEffect
        val target = (held * state.durationMs).toLong()
        if (abs(state.positionMs - target) < 1200) {
            scrub = null
            seekPending = false
        }
    }

    LaunchedEffect(state.nowPlayingId) {
        scrub = null
        seekPending = false
    }

    val playFraction =
        if (state.durationMs > 0) {
            (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
    val shownPosition = scrub?.let { (it * state.durationMs).toLong() } ?: state.positionMs

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
    ) {
        Text(
            text = shownPosition.asTime(),
            style = MaterialTheme.typography.labelSmall,
            color = if (scrub != null) TuiAccent else TuiDim,
        )
        ThinSlider(
            fraction = scrub ?: playFraction,
            onScrub = {
                scrub = it
                if (it != null) seekPending = false
            },
            onSeek = {
                dispatch(DmtAction.Seek(it))
                seekPending = true
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        Text(
            text = state.durationMs.asTime(),
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim,
        )
    }
}

@Composable
private fun TransportRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
    ) {
        TuiKey(
            label = "|<<",
            big = true,
        ) {
            dispatch(DmtAction.Prev)
        }
        TuiKey(
            label = if (state.isPlaying) "  ||  " else "  |>  ",
            bright = true,
            big = true,
        ) {
            dispatch(DmtAction.TogglePlay)
        }
        TuiKey(
            label = ">>|",
            big = true,
        ) {
            dispatch(DmtAction.Next)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusRow(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
    ) {
        TuiStatus(
            label = stringResource(R.string.shuffle_key),
            value = if (state.shuffle) {
                stringResource(R.string.on)
            } else {
                stringResource(R.string.off)
            },
            on = state.shuffle,
        ) {
            dispatch(DmtAction.ToggleShuffle)
        }
        TuiStatus(
            label = stringResource(R.string.repeat_key),
            value = stringResource(
                when (state.repeat) {
                    Player.REPEAT_MODE_ALL -> R.string.repeat_all
                    Player.REPEAT_MODE_ONE -> R.string.repeat_one
                    else -> R.string.off
                },
            ),
            on = state.repeat != Player.REPEAT_MODE_OFF,
        ) {
            dispatch(DmtAction.CycleRepeat)
        }
        TuiStatus(
            label = stringResource(R.string.sleep_key),
            value = if (state.sleepMinutes == 0) {
                stringResource(R.string.off)
            } else {
                stringResource(R.string.sleep_left, (state.sleepLeftMs + 59_999) / 60_000)
            },
            on = state.sleepMinutes != 0,
        ) {
            dispatch(DmtAction.CycleSleep)
        }
        TuiStatus(
            label = stringResource(R.string.speed_key),
            value = stringResource(R.string.speed_value, state.speed.toString()),
            on = abs(state.speed - 1f) > 0.01f,
        ) {
            dispatch(DmtAction.CycleSpeed)
        }
        TuiStatus(
            label = stringResource(R.string.lyrics_key),
            value = stringResource(
                when {
                    state.lyricsFetching -> R.string.lyrics_key_busy
                    state.lyrics == null -> R.string.lyrics_key_fetch
                    showLyrics -> R.string.on
                    else -> R.string.off
                },
            ),
            on = showLyrics && state.lyrics != null,
            busy = state.lyricsFetching,
        ) {
            when {
                state.lyricsFetching -> Unit
                state.lyrics == null -> dispatch(DmtAction.FetchLyrics)
                else -> onToggleLyrics()
            }
        }
    }
}

@Composable
private fun QueueFooter(state: DmtState, onQueue: () -> Unit) {
    val next = state.queue.getOrNull(state.queueIndex + 1)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TuiLine)
            .background(TuiSurface.copy(alpha = 0.4f))
            .tuiClickable(onQueue)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(
            text = stringResource(R.string.queue_key, state.queueIndex + 1, state.queue.size),
            style = MaterialTheme.typography.labelMedium,
            color = TuiFg,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = next?.let { stringResource(R.string.next_up, it).lowercase() }
                ?: stringResource(R.string.end_of_queue),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
