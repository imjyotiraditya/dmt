package dev.jyotiraditya.dmt.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.R
import kotlin.math.abs
import dev.jyotiraditya.dmt.player.asTime
import dev.jyotiraditya.dmt.ui.AsciiCover
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtState
import dev.jyotiraditya.dmt.ui.components.ThinSlider
import dev.jyotiraditya.dmt.ui.components.TuiChip
import dev.jyotiraditya.dmt.ui.components.TuiKey
import dev.jyotiraditya.dmt.ui.components.TuiPanel
import dev.jyotiraditya.dmt.ui.components.TuiStatus
import dev.jyotiraditya.dmt.ui.components.tuiClickable
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint

@Composable
fun ExpandedPlayer(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
    onQueue: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val landscape = configuration.screenWidthDp > configuration.screenHeightDp
    var showLyrics by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg)
            .tuiClickable {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        if (landscape) {
            LandscapePlayer(state, dispatch, onInfo, onQueue, showLyrics) {
                showLyrics = !showLyrics
            }
        } else {
            PortraitPlayer(state, dispatch, onInfo, onQueue, showLyrics) {
                showLyrics = !showLyrics
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
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PlayerHeader(
            dispatch = dispatch,
            onInfo = onInfo,
            hasLyrics = state.lyrics != null,
            showLyrics = showLyrics,
            onToggleLyrics = onToggleLyrics
        )

        Spacer(modifier = Modifier.weight(1f))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            ArtSlot(state, dispatch, showLyrics)
        }
        TrackMeta(state)
        SeekRow(state, dispatch)
        TransportRow(state, dispatch)
        StatusRow(state, dispatch)

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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp)
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
        ) {
            ArtSlot(state, dispatch, showLyrics)
        }
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
        ) {
            PlayerHeader(
                dispatch = dispatch,
                onInfo = onInfo,
                hasLyrics = state.lyrics != null,
                showLyrics = showLyrics,
                onToggleLyrics = onToggleLyrics
            )

            Spacer(modifier = Modifier.weight(1f))

            TrackMeta(state)
            SeekRow(state, dispatch)
            TransportRow(state, dispatch)
            StatusRow(state, dispatch, singleRow = true)

            Spacer(modifier = Modifier.weight(1f))

            QueueFooter(state, onQueue)
        }
    }
}

@Composable
private fun PlayerHeader(
    dispatch: (DmtAction) -> Unit,
    onInfo: () -> Unit,
    hasLyrics: Boolean,
    showLyrics: Boolean,
    onToggleLyrics: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            TuiKey(stringResource(R.string.close)) { dispatch(DmtAction.Expand(false)) }
        }
        Text(
            text = stringResource(R.string.now_playing),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
            modifier = Modifier.align(Alignment.Center)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (hasLyrics) {
                TuiKey(
                    label = stringResource(R.string.lyrics_key),
                    bright = showLyrics,
                    onClick = onToggleLyrics
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            TuiKey(stringResource(R.string.info), onClick = onInfo)
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
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            isPlaying = state.isPlaying,
            contentAspect = aspect,
            onSeekFraction = { dispatch(DmtAction.Seek(it)) },
            modifier = modifier
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
                        .aspectRatio(rawArt.width.toFloat() / rawArt.height)
                )
            }

            state.cover != null -> {
                AsciiCover(
                    cover = state.cover,
                    playing = state.isPlaying,
                    wave = state.settings.wave,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            else -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_cover),
                        style = MaterialTheme.typography.labelMedium,
                        color = TuiFaint
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackMeta(state: DmtState) {
    Text(
        text = "${state.title}_",
        style = MaterialTheme.typography.titleLarge,
        color = TuiBright,
        maxLines = 1,
        modifier = Modifier
            .padding(top = 18.dp)
            .basicMarquee(iterations = Int.MAX_VALUE)
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
        modifier = Modifier.padding(top = 3.dp)
    )
    if (state.settings.listSpecs && state.tech.isNotEmpty()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            state.tech.forEach { spec ->
                TuiChip("${spec.label}:${spec.value}".lowercase())
            }
        }
    }
}

@Composable
private fun SeekRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    var seekPending by remember { mutableStateOf(false) }
    val accent = LocalAccent.current

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
            .padding(top = 8.dp)
    ) {
        Text(
            text = shownPosition.asTime(),
            style = MaterialTheme.typography.labelSmall,
            color = if (scrub != null) accent else TuiDim
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
                .padding(horizontal = 10.dp)
        )
        Text(
            text = state.durationMs.asTime(),
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim
        )
    }
}

@Composable
private fun TransportRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        TuiKey("|<<", big = true) { dispatch(DmtAction.Prev) }
        TuiKey(
            label = if (state.isPlaying) "  ||  " else "  |>  ",
            bright = true,
            big = true
        ) {
            dispatch(DmtAction.TogglePlay)
        }
        TuiKey(">>|", big = true) { dispatch(DmtAction.Next) }
    }
}

@Composable
private fun StatusRow(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    singleRow: Boolean = false,
) {
    val shuffle: @Composable () -> Unit = {
        TuiStatus("shf", if (state.shuffle) "on" else "off", state.shuffle) {
            dispatch(DmtAction.ToggleShuffle)
        }
    }
    val repeat: @Composable () -> Unit = {
        TuiStatus(
            label = "rpt",
            value = when (state.repeat) {
                Player.REPEAT_MODE_ALL -> "all"
                Player.REPEAT_MODE_ONE -> "one"
                else -> "off"
            },
            on = state.repeat != Player.REPEAT_MODE_OFF
        ) {
            dispatch(DmtAction.CycleRepeat)
        }
    }
    val sleep: @Composable () -> Unit = {
        TuiStatus(
            label = "slp",
            value = if (state.sleepMinutes == 0) {
                "off"
            } else {
                "${(state.sleepLeftMs + 59_999) / 60_000}m"
            },
            on = state.sleepMinutes != 0
        ) {
            dispatch(DmtAction.CycleSleep)
        }
    }
    val speed: @Composable () -> Unit = {
        TuiStatus(
            label = "spd",
            value = when {
                kotlin.math.abs(state.speed - 1f) < 0.01f -> "1.0x"
                kotlin.math.abs(state.speed - 0.75f) < 0.01f -> "0.75x"
                kotlin.math.abs(state.speed - 1.25f) < 0.01f -> "1.25x"
                kotlin.math.abs(state.speed - 1.5f) < 0.01f -> "1.5x"
                else -> "2.0x"
            },
            on = kotlin.math.abs(state.speed - 1f) > 0.01f
        ) {
            dispatch(DmtAction.CycleSpeed)
        }
    }

    if (singleRow) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            shuffle()
            repeat()
            sleep()
            speed()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            shuffle()
            repeat()
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            sleep()
            speed()
        }
    }
}

@Composable
private fun QueueFooter(state: DmtState, onQueue: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        TuiKey(
            label = stringResource(R.string.queue_key, state.queueIndex + 1, state.queue.size),
            onClick = onQueue
        )
    }
    val next = state.queue.getOrNull(state.queueIndex + 1)
    Text(
        text = next?.let { stringResource(R.string.next_up, it).lowercase() }
            ?: stringResource(R.string.end_of_queue),
        style = MaterialTheme.typography.labelSmall,
        color = TuiFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
    Spacer(modifier = Modifier.height(16.dp))
}
