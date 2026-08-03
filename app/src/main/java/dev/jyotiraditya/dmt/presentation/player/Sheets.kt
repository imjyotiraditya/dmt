package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiGreen
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiRed
import dev.jyotiraditya.dmt.util.asTime

private val TRACK_SPEC_LABELS = setOf("FMT", "BIT", "RATE", "CH", "KBPS", "VBR", "GAPLESS", "SIZE")
private val DECODER_SPEC_LABELS = setOf("DEC", "HW", "IMPL", "INST")
private val OUTPUT_ROUTE_LABELS = setOf("API", "BIT", "RATE", "BUF", "FLAGS")
private val DEVICE_ROUTE_LABELS = setOf("VIA", "NAME", "RATES", "ENC", "CH")
private const val CHAIN_LABEL_WIDTH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuiSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TuiBg,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        Column {
            HorizontalDivider(color = TuiLine)
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                content()
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    meta: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(TuiAccent),
            )
            Text(
                text = " $title",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
            )
        }
        actions()
    }
}

@Composable
fun QueueList(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        itemsIndexed(state.queue) { position, entry ->
            val current = entry.index == state.queueIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .tuiClickable { dispatch(DmtAction.Jump(entry.index)) }
                    .padding(vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (current) TuiAccent else TuiFaint),
                )
                Text(
                    text = " %02d ".format(position + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiFaint,
                )
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current) TuiBright else TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = TuiFaint,
                    modifier = Modifier
                        .tuiClickable { dispatch(DmtAction.RemoveAt(entry.index)) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun InfoContent(state: DmtState) {
    val track: Track? = state.tracks.find { it.id.toString() == state.nowPlayingId }

    InfoRow(
        label = stringResource(R.string.info_title),
        value = state.title,
    )
    InfoRow(
        label = stringResource(R.string.info_artist),
        value = state.artist.asCredit().lowercase(),
    )
    if (state.album.isNotBlank()) {
        InfoRow(
            label = stringResource(R.string.info_album),
            value = state.album.lowercase(),
        )
    }
    InfoRow(
        label = stringResource(R.string.info_duration),
        value = state.durationMs.asTime(),
    )
    state.tech.forEach { spec ->
        InfoRow(
            label = spec.label.lowercase(),
            value = spec.value.lowercase(),
        )
    }
    track?.let {
        InfoRow(
            label = stringResource(R.string.info_uri),
            value = it.uri.toString(),
        )
    }
}

@Composable
fun ChainContent(state: DmtState) {
    val stages = remember(state.tech, state.speed, state.route) { chainStages(state) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
    ) {
        stages.forEachIndexed { index, stage ->
            ChainStageBlock(
                stage = stage,
                last = index == stages.lastIndex,
            )
        }
    }
}

@Composable
private fun ChainStageBlock(stage: ChainStage, last: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "█ ",
            style = MaterialTheme.typography.bodyMedium,
            color = stage.color,
        )
        Text(
            text = stage.name,
            style = MaterialTheme.typography.labelMedium,
            color = TuiBright,
        )
    }
    stage.specs.forEach { spec ->
        ChainSpecRow(spec)
    }
    if (!last) {
        Text(
            text = "v",
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFaint,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ChainSpecRow(spec: Spec) {
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = "┊ ",
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFaint,
        )
        Text(
            text = spec.label.lowercase().padEnd(CHAIN_LABEL_WIDTH),
            style = MaterialTheme.typography.bodyMedium,
            color = TuiDim,
        )
        Text(
            text = spec.value.lowercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (spec.hot) TuiAccent else TuiFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class ChainStage(
    val name: String,
    val color: Color,
    val specs: List<Spec>,
)

private fun chainStages(state: DmtState): List<ChainStage> {
    val trackRate = state.tech.firstOrNull { it.label == "RATE" }?.value
    val trackBits = state.tech.firstOrNull { it.label == "BIT" }?.value
    val output = state.route.filter { it.label in OUTPUT_ROUTE_LABELS }.map { spec ->
        when {
            spec.label == "RATE" && trackRate != null && trackRate != spec.value ->
                spec.copy(value = "$trackRate -> ${spec.value}", hot = true)

            spec.label == "BIT" && trackBits != null && trackBits != spec.value ->
                spec.copy(value = "$trackBits -> ${spec.value}", hot = true)

            else -> spec
        }
    }
    val device = state.route.filter { it.label in DEVICE_ROUTE_LABELS }

    return buildList {
        state.tech.filter { it.label in TRACK_SPEC_LABELS }
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(name = "track", color = TuiAccent, specs = it)) }
        state.tech.filter { it.label in DECODER_SPEC_LABELS }
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(name = "decoder", color = TuiGreen, specs = it)) }
        add(
            ChainStage(
                name = "dsp",
                color = TuiFg,
                specs = listOf(Spec(label = "SPEED", value = "%.2fx".format(state.speed))),
            ),
        )
        if (output.isNotEmpty()) {
            add(ChainStage(name = "output", color = TuiRed, specs = output))
        }
        if (device.isNotEmpty()) {
            add(ChainStage(name = "device", color = TuiDim, specs = device))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                modifier = Modifier.padding(end = 16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TuiFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = TuiLine)
    }
}

/**
 * Lets the listener pick where the lyrics of a track come from.
 *
 * Each source says what it holds for the track at hand, and one that holds nothing cannot be picked,
 * other than the one that has to be fetched, which only says once it has been asked.
 */
@Composable
fun LyricsSourceSheet(
    selected: LyricsSource,
    showingFrom: LyricsSource?,
    available: Set<LyricsSource>,
    tried: Set<LyricsSource>,
    fetching: Boolean,
    onPick: (LyricsSource) -> Unit,
    onDismiss: () -> Unit,
) {
    TuiSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.lyrics_sources_title))
        LyricsSource.entries.forEach { source ->
            // Lrclib can always be picked, as having nothing may have been the network rather
            // than the track, and asking again is the only way to tell.
            val pickable = source in available || source == LyricsSource.LRCLIB

            LyricsSourceRow(
                source = source,
                selected = source == selected,
                pickable = pickable,
                status = source.status(showingFrom, available, tried, fetching),
                onPick = { onPick(source) }.takeIf { pickable },
            )
        }
    }
}

@Composable
private fun LyricsSourceRow(
    source: LyricsSource,
    selected: Boolean,
    pickable: Boolean,
    status: LyricsSourceStatus,
    onPick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onPick != null) it.tuiClickable(onPick) else it }
            .padding(vertical = 10.dp),
    ) {
        Text(
            text = if (selected) "[x] " else "[ ] ",
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) TuiAccent else TuiFaint,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(source.label()),
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !pickable -> TuiFaint
                    selected -> TuiFg
                    else -> TuiBright
                },
            )
            Text(
                text = stringResource(source.hint()),
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
            )
        }
        Text(
            text = stringResource(status.label),
            style = MaterialTheme.typography.labelMedium,
            color = status.color,
        )
    }
}

/** What a source holds for the track being played. */
private data class LyricsSourceStatus(val label: Int, val color: Color)

private fun LyricsSource.status(
    showingFrom: LyricsSource?,
    available: Set<LyricsSource>,
    tried: Set<LyricsSource>,
    fetching: Boolean,
): LyricsSourceStatus = when {
    this == showingFrom -> LyricsSourceStatus(R.string.lyrics_source_showing, TuiAccent)

    this == LyricsSource.LRCLIB && fetching ->
        LyricsSourceStatus(R.string.lyrics_key_busy, TuiDim)

    this in available -> LyricsSourceStatus(R.string.lyrics_source_found, TuiDim)

    // Lrclib is the one source that has to be asked before it can say.
    this == LyricsSource.LRCLIB -> LyricsSourceStatus(
        label = if (this in tried) {
            R.string.lyrics_source_none_online
        } else {
            R.string.lyrics_source_tap
        },
        color = TuiFaint,
    )

    else -> LyricsSourceStatus(R.string.lyrics_source_missing, TuiFaint)
}

private fun LyricsSource.label(): Int = when (this) {
    LyricsSource.EMBEDDED -> R.string.lyrics_source_embedded
    LyricsSource.LOCAL -> R.string.lyrics_source_local
    LyricsSource.LRCLIB -> R.string.lyrics_source_lrclib
}

private fun LyricsSource.hint(): Int = when (this) {
    LyricsSource.EMBEDDED -> R.string.lyrics_source_embedded_hint
    LyricsSource.LOCAL -> R.string.lyrics_source_local_hint
    LyricsSource.LRCLIB -> R.string.lyrics_source_lrclib_hint
}
