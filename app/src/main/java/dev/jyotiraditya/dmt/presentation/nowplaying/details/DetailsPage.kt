package dev.jyotiraditya.dmt.presentation.nowplaying.details

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlaybackSpeedState
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.AudioQuality
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.nowPlayingTrack
import dev.jyotiraditya.dmt.ui.components.FactRow
import dev.jyotiraditya.dmt.ui.components.QualityBadge
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.util.asTime

/** The playing track's format, the path its sound takes to the speaker, and its tags. */
@OptIn(UnstableApi::class)
@Composable
internal fun DetailsPage(state: DmtState, player: Player) {
    val speed = rememberPlaybackSpeedState(player).playbackSpeed
    val stages =
        remember(state.tech, state.route, speed) { signalChain(state.tech, state.route, speed) }
    val track = state.nowPlayingTrack
    val file =
        remember(stages) { stages.firstOrNull { it.title == R.string.chain_file }?.facts.orEmpty() }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Margin),
    ) {
        Summary(state.quality, file, track)
        stages.forEach { stage ->
            Heading(stringResource(stage.title))
            stage.facts.forEach { fact ->
                FactRow(label = stringResource(fact.field.label), value = fact.value)
            }
        }
        Heading(stringResource(R.string.player_track_info))
        trackInfo(state, track).forEach { row ->
            FactRow(label = stringResource(row.label), value = row.value)
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/** The format in one line with its quality badge, then bitrate and size, then any cue sheet. */
@Composable
private fun Summary(quality: AudioQuality?, file: List<Fact>, track: Track?) {
    val headline = file.valuesOf(SUMMARY_FIELDS)
    val detail = file.valuesOf(SUMMARY_DETAIL_FIELDS)
    if (headline.isEmpty() && quality == null) return
    Column(modifier = Modifier.padding(top = 20.dp)) {
        quality?.let {
            QualityBadge(it, ink = Ink, field = Ground, modifier = Modifier.padding(bottom = 10.dp))
        }
        if (headline.isNotEmpty()) {
            Text(text = headline, style = MaterialTheme.typography.titleLarge, color = Ink)
        }
        if (detail.isNotEmpty()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (track?.cue == true) {
            Text(
                text = cueSummary(track),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** The disc image a cue track comes from and where in it the track sits. */
@Composable
private fun cueSummary(track: Track): String {
    val start = track.clipStartMs ?: 0L
    val end = track.clipEndMs ?: (start + track.durationMs)
    return listOf(
        stringResource(R.string.library_cue_from),
        track.path.substringAfterLast('/'),
        "${start.asTime()} – ${end.asTime()}",
    )
        .joinToString(" · ")
}

private val SUMMARY_FIELDS =
    listOf(SpecField.FORMAT, SpecField.BIT_DEPTH, SpecField.SAMPLE_RATE, SpecField.CHANNELS)
private val SUMMARY_DETAIL_FIELDS =
    listOf(SpecField.BITRATE, SpecField.VARIABLE_BITRATE, SpecField.SIZE)

private fun List<Fact>.valuesOf(fields: List<SpecField>): String =
    fields.mapNotNull { field -> firstOrNull { it.field == field }?.value }.joinToString(" · ")

@Composable
private fun Heading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        color = Ink,
        modifier = Modifier.padding(top = 32.dp, bottom = 4.dp),
    )
}

private data class InfoRow(@param:StringRes val label: Int, val value: String)

private fun trackInfo(state: DmtState, track: Track?): List<InfoRow> = buildList {
    add(InfoRow(R.string.track_field_title, state.title))
    add(InfoRow(R.string.track_field_artist, state.artist.asCredit()))
    if (state.album.isNotBlank()) {
        add(InfoRow(R.string.track_field_album, state.album))
    }
    track
        ?.genre
        ?.takeIf { it.isNotBlank() }
        ?.let { genre ->
            add(InfoRow(R.string.track_field_genre, genre))
        }
    add(InfoRow(R.string.track_field_length, state.durationMs.asTime()))
    track?.let {
        add(InfoRow(R.string.track_field_location, it.path.ifBlank { it.uri.toString() }))
    }
}
