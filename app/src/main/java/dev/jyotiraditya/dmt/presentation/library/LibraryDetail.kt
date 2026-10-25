package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.Artwork
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.TrackRow
import dev.jyotiraditya.dmt.ui.components.animateFieldAsState
import dev.jyotiraditya.dmt.ui.components.background
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.components.rememberFieldColor
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.sharedElement
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.fitShare
import dev.jyotiraditya.dmt.ui.theme.inkOn
import dev.jyotiraditya.dmt.ui.theme.poster
import dev.jyotiraditya.dmt.util.asTime

/** The header of an album, artist or playlist, tinted with its cover colour. */
@Composable
internal fun DetailHead(
    title: String,
    meta: String,
    tracks: List<Track>,
    dispatch: (DmtAction) -> Unit,
    modifier: Modifier = Modifier,
    coverKey: Any? = null,
    action: (@Composable (Color) -> Unit)? = null,
) {
    val seed = tracks.firstOrNull()
    val field = rememberFieldColor(seed)
    val paint by animateFieldAsState(field)
    val ink = field.inkOn()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background { paint }
                .padding(horizontal = Margin)
                .padding(top = 20.dp, bottom = 28.dp),
    ) {
        Artwork(
            track = seed,
            modifier =
                Modifier.fillMaxWidth(0.66f)
                    .wrapContentWidth(Alignment.Start)
                    .widthIn(max = 300.dp)
                    .aspectRatio(1f)
                    .then(if (coverKey != null) Modifier.sharedElement(coverKey) else Modifier),
        )
        Text(
            text = title,
            style = poster(fitShare(title, 0.11f)),
            color = ink,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 22.dp),
        )
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Text(
            text = tracksSummary(tracks),
            style = MaterialTheme.typography.bodySmall,
            color = ink,
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            if (tracks.isNotEmpty()) {
                Block(
                    label = stringResource(R.string.action_play),
                    icon = DmtIcons.Play,
                    fill = ink,
                    ink = field,
                    onClick = { dispatch(DmtAction.PlayAt(tracks, 0)) },
                )
                TextLink(
                    label = stringResource(R.string.player_shuffle),
                    color = ink,
                    onClick = { dispatch(DmtAction.ShufflePlay(tracks)) },
                )
                TextLink(
                    label = stringResource(R.string.action_queue),
                    color = ink,
                    onClick = { dispatch(DmtAction.Enqueue(tracks, title)) },
                )
            }
            action?.invoke(ink)
        }
    }
}

@Composable
internal fun TracksDetail(
    title: String,
    meta: String,
    tracks: List<Track>,
    numbered: Boolean,
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    coverKey: Any? = null,
) {
    val openTrackMenu = rememberTrackMenu(state, dispatch, libraryNavigation(dispatch))
    val playing = rememberWantsPlayback()
    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item { DetailHead(title, meta, tracks, dispatch, coverKey = coverKey) }
        item { Spacer(modifier = Modifier.height(12.dp)) }
        itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(
                track = track,
                meta =
                    if (numbered) {
                        "${track.artist.asCredit()} · ${track.durationMs.asTime()}"
                    } else {
                        trackSummary(track)
                    },
                current = track.id.toString() == state.nowPlayingId,
                playing = playing,
                number =
                    if (numbered) {
                        (track.trackNumber.takeIf { it > 0 } ?: (index + 1))
                    } else {
                        null
                    },
                onClick = { dispatch(DmtAction.PlayAt(tracks, index)) },
                onLongClick = { openTrackMenu(track) },
            )
        }
    }
}

@Composable
internal fun ArtistDetail(artist: Artist, state: DmtState, dispatch: (DmtAction) -> Unit) {
    val albums = artist.discography
    LazyColumn(contentPadding = PaddingValues(bottom = LocalContentBottom.current)) {
        item { DetailHead(artist.name, artistSummary(artist), artist.tracks, dispatch) }
        items(albums, key = { it.id }) { a ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.fillMaxWidth()
                        .pressable(
                            onClick = { dispatch(DmtAction.OpenAlbum(a.id)) },
                            sink = 0.985f,
                            label = a.name,
                        )
                        .padding(horizontal = Margin, vertical = 10.dp),
            ) {
                Artwork(
                    track = a.tracks.first(),
                    modifier = Modifier.size(84.dp).sharedElement(albumCoverKey(a.id)),
                )
                Column(modifier = Modifier.padding(start = 16.dp)) {
                    Text(
                        text = a.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = tracksSummary(a.tracks),
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted,
                    )
                }
            }
        }
    }
}
