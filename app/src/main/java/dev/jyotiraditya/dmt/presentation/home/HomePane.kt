package dev.jyotiraditya.dmt.presentation.home

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.AsciiCover
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.presentation.library.artistLine2
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine


@Composable
fun HomePane(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    art: suspend (Track) -> Bitmap,
    onOpenAlbum: (String) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenTracks: () -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenArtists: () -> Unit,
) {
    if (state.scanning && state.tracks.isEmpty()) {
        Caption(stringResource(R.string.scanning))
        return
    }
    if (state.tracks.isEmpty()) {
        Caption(stringResource(R.string.no_audio, state.settings.sourceMode.label))
        return
    }

    val home = state.home

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (home.hasFavourites) {
            Text(
                text = stringResource(R.string.home_favourites),
                style = MaterialTheme.typography.titleLarge,
                color = TuiBright,
                modifier = Modifier.padding(top = 14.dp, bottom = 6.dp),
            )
        }

        if (home.albums.isNotEmpty()) {
            ShelfHeader(label = stringResource(R.string.home_albums), onMore = onOpenAlbums)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(home.albums, key = { it.name }) { album ->
                    AlbumCard(album = album, art = art, artKey = state.settings.rawArt) {
                        onOpenAlbum(album.name)
                    }
                }
            }
        }

        if (home.artists.isNotEmpty()) {
            ShelfHeader(label = stringResource(R.string.home_artists), onMore = onOpenArtists)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(home.artists, key = { it.name }) { artist ->
                    ArtistCard(artist = artist, art = art, artKey = state.settings.rawArt) {
                        onOpenArtist(artist.name)
                    }
                }
            }
        }

        if (home.tracks.isNotEmpty()) {
            ShelfHeader(label = stringResource(R.string.home_tracks), onMore = onOpenTracks)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(home.tracks, key = { _, track -> track.id }) { index, track ->
                    TrackCard(track = track, art = art, artKey = state.settings.rawArt) {
                        dispatch(DmtAction.PlayAt(home.tracks, index))
                    }
                }
            }
        }

        if (home.fresh.isNotEmpty()) {
            if (home.hasFavourites) {
                HorizontalDivider(
                    color = TuiLine,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_try_new),
                style = MaterialTheme.typography.titleLarge,
                color = TuiBright,
                modifier = Modifier.padding(top = 14.dp),
            )
            Caption(stringResource(R.string.home_try_new_hint))
            ShelfHeader(label = stringResource(R.string.home_tracks), onMore = onOpenTracks)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(home.fresh, key = { _, track -> track.id }) { index, track ->
                    TrackCard(track = track, art = art, artKey = state.settings.rawArt) {
                        dispatch(DmtAction.PlayAt(home.fresh, index))
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ShelfHeader(label: String, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = TuiFg,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = ">",
            style = MaterialTheme.typography.titleMedium,
            color = TuiDim,
            modifier = Modifier
                .tuiClickable(onMore)
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun AlbumCard(
    album: Album,
    art: suspend (Track) -> Bitmap,
    artKey: Any,
    onClick: () -> Unit,
) {
    ShelfCard(
        seed = album.tracks.first(),
        title = album.name,
        meta = album.artist,
        art = art,
        artKey = artKey,
        onClick = onClick,
    )
}

@Composable
private fun ArtistCard(
    artist: Artist,
    art: suspend (Track) -> Bitmap,
    artKey: Any,
    onClick: () -> Unit,
) {
    ShelfCard(
        seed = artist.tracks.first(),
        title = artist.name,
        meta = artistLine2(artist),
        art = art,
        artKey = artKey,
        onClick = onClick,
    )
}

@Composable
private fun TrackCard(
    track: Track,
    art: suspend (Track) -> Bitmap,
    artKey: Any,
    onClick: () -> Unit,
) {
    ShelfCard(
        seed = track,
        title = track.title,
        meta = track.artist.asCredit(),
        art = art,
        artKey = artKey,
        onClick = onClick,
    )
}

@Composable
private fun ShelfCard(
    seed: Track,
    title: String,
    meta: String,
    art: suspend (Track) -> Bitmap,
    artKey: Any,
    onClick: () -> Unit,
) {
    val cover by produceState<Bitmap?>(initialValue = null, seed.id, artKey) {
        value = art(seed)
    }
    Column(
        modifier = Modifier
            .width(148.dp)
            .border(1.dp, TuiLine)
            .tuiClickable(onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            cover?.let {
                AsciiCover(
                    cover = it,
                    playing = false,
                    wave = false,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = TuiBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
        )
        Text(
            text = meta.lowercase().ifBlank { " " },
            style = MaterialTheme.typography.labelSmall,
            color = TuiDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(top = 2.dp, bottom = 8.dp),
        )
    }
}
