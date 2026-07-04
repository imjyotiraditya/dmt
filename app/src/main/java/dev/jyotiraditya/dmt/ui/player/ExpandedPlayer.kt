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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg)
            .tuiClickable {}
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
    ) {
        PlayerHeader(dispatch, onInfo)

        Spacer(modifier = Modifier.weight(1f))

        CoverPanel(state)
        TrackMeta(state)
        SeekRow(state, dispatch)
        TransportRow(state, dispatch)
        StatusRow(state, dispatch)

        Spacer(modifier = Modifier.weight(1f))

        QueueFooter(state, onQueue)
    }
}

@Composable
private fun PlayerHeader(dispatch: (DmtAction) -> Unit, onInfo: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        TuiKey(stringResource(R.string.close)) { dispatch(DmtAction.Expand(false)) }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.now_playing),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim
        )
        Spacer(modifier = Modifier.weight(1f))
        TuiKey(stringResource(R.string.info), onClick = onInfo)
    }
}

@Composable
private fun CoverPanel(state: DmtState) {
    val rawArt = state.artRaw

    TuiPanel(modifier = Modifier.padding(top = 14.dp)) {
        when {
            state.settings.rawArt && rawArt != null -> {
                val image = remember(rawArt) { rawArt.asImageBitmap() }
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(rawArt.width.toFloat() / rawArt.height)
                )
            }

            state.cover != null -> {
                AsciiCover(
                    cover = state.cover,
                    playing = state.isPlaying,
                    wave = state.settings.wave
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
private fun StatusRow(state: DmtState, dispatch: (DmtAction) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        TuiStatus("shf", if (state.shuffle) "on" else "off", state.shuffle) {
            dispatch(DmtAction.ToggleShuffle)
        }
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
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
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
