package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.util.asHoursMinutes
import dev.jyotiraditya.dmt.util.asTime

private const val SEPARATOR = " · "

/** A track's artist, album and length, leaving out what is blank or not asked for. */
fun trackSummary(track: Track, artist: Boolean = true, album: Boolean = true): String =
    listOfNotNull(
        track.artist.asCredit().takeIf { artist },
        track.album.takeIf { album },
        track.durationMs.asTime(),
    )
        .filter(String::isNotBlank)
        .joinToString(SEPARATOR)

/** How many albums and tracks an artist has, such as `2 albums · 14 tracks`. */
@Composable
fun artistSummary(artist: Artist): String = listOf(
    pluralStringResource(R.plurals.count_albums, artist.albums, artist.albums),
    pluralStringResource(R.plurals.count_tracks, artist.tracks.size, artist.tracks.size),
)
    .joinToString(SEPARATOR)

/** How many tracks there are and how long they play, such as `9 tracks · 54m`. */
@Composable
fun tracksSummary(tracks: List<Track>): String {
    if (tracks.isEmpty()) return stringResource(R.string.count_tracks_none)
    val total = remember(tracks) { tracks.sumOf { it.durationMs }.asHoursMinutes() }
    return pluralStringResource(R.plurals.count_tracks, tracks.size, tracks.size) +
        SEPARATOR +
        total
}

/** A folder's name, calling the shared storage root by name rather than by its path. */
@Composable
fun folderTitle(folder: Folder): String =
    folder.name.ifEmpty { stringResource(R.string.folders_storage_root) }

/** Where a folder sits and what it holds, such as `Music · 9 tracks · 54m`. */
@Composable
fun folderSummary(folder: Folder): String = listOf(folder.location, tracksSummary(folder.tracks))
    .filter(String::isNotEmpty)
    .joinToString(SEPARATOR)
