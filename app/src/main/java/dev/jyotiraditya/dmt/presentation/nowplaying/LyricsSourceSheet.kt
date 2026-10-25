package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.LyricsTiming
import dev.jyotiraditya.dmt.presentation.nowplaying.lyrics.label
import dev.jyotiraditya.dmt.presentation.player.LyricsSourceStatus
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.SelectionMark
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.pressableSelect
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.rowFill

/** Picks where lyrics come from, tagging each source with what it has for the track. */
@Composable
fun LyricsSourceSheet(
    trackTitle: String,
    current: LyricsSource,
    sources: Map<LyricsSource, LyricsSourceStatus>,
    onPick: (LyricsSource) -> Unit,
    onDismiss: () -> Unit,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.lyrics_source_title), meta = trackTitle)
        Column(modifier = Modifier.selectableGroup()) {
            LyricsSource.entries.forEachIndexed { index, source ->
                val status = sources[source]
                SourceRow(
                    label = stringResource(source.label),
                    detail = stringResource(source.hint),
                    tag = stringResource(status.tag(source)),
                    chosen = source == current,
                    fill = index.rowFill,
                    mark = (index + 1).rowFill,
                    // Online sources stay pickable; a miss may have been a network error.
                    enabled = source.online || status is LyricsSourceStatus.Found,
                    onClick = { onPick(source) },
                )
            }
        }
    }
}

@Composable
private fun SourceRow(
    label: String,
    detail: String,
    tag: String,
    chosen: Boolean,
    fill: Color,
    mark: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ink = if (enabled) Ink else Muted
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.fillMaxWidth()
                .pressableSelect(
                    selected = chosen,
                    onClick = onClick,
                    enabled = enabled,
                    sink = 0.985f,
                )
                .background(fill)
                .padding(horizontal = Margin, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = label, style = MaterialTheme.typography.titleMedium, color = ink)
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = ink,
                    maxLines = 1,
                    modifier =
                        Modifier.padding(start = 8.dp)
                            .background(ink.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (enabled) {
            SelectionMark(
                selected = chosen,
                empty = mark,
                modifier = Modifier.padding(start = 16.dp),
            )
        }
    }
}

/** The tag beside [source]'s name; a null status is an online source not asked yet. */
@StringRes
private fun LyricsSourceStatus?.tag(source: LyricsSource): Int = when (this) {
    is LyricsSourceStatus.Found -> timing.label

    LyricsSourceStatus.Fetching -> R.string.lyrics_fetching

    LyricsSourceStatus.Unreachable -> R.string.lyrics_source_unreachable

    LyricsSourceStatus.Missing ->
        if (source.online) {
            R.string.lyrics_source_none_online
        } else {
            R.string.lyrics_source_missing
        }

    null -> R.string.lyrics_source_tap
}

@get:StringRes
private val LyricsSource.hint: Int
    get() =
        when (this) {
            LyricsSource.EMBEDDED -> R.string.lyrics_source_embedded_hint
            LyricsSource.LOCAL -> R.string.lyrics_source_local_hint
            LyricsSource.LRCLIB -> R.string.lyrics_source_lrclib_hint
            LyricsSource.LRC_RED -> R.string.lyrics_source_lrc_red_hint
        }

@get:StringRes
private val LyricsTiming.label: Int
    get() =
        when (this) {
            LyricsTiming.WORD -> R.string.lyrics_timing_word
            LyricsTiming.LINE -> R.string.lyrics_timing_line
            LyricsTiming.PLAIN -> R.string.lyrics_timing_plain
        }
