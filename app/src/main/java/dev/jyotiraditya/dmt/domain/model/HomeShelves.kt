package dev.jyotiraditya.dmt.domain.model

import androidx.compose.runtime.Immutable

private const val SHELF_SIZE = 12

@Immutable
data class HomeShelves(
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val fresh: List<Track> = emptyList(),
) {
    val hasFavourites: Boolean
        get() = albums.isNotEmpty() || artists.isNotEmpty() || tracks.isNotEmpty()
}

fun homeShelves(
    tracks: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    counts: Map<Long, Int>,
): HomeShelves =
    HomeShelves(
        albums = albums.mostPlayed { it.tracks.plays(counts) },
        artists = artists.mostPlayed { it.tracks.plays(counts) },
        tracks = tracks
            .filter { it.plays(counts) > 0 }
            .sortedWith(
                compareByDescending<Track> { it.plays(counts) }
                    .thenByDescending { it.dateAdded },
            )
            .take(SHELF_SIZE),
        fresh = tracks
            .filter { it.plays(counts) == 0 }
            .sortedByDescending { it.dateAdded }
            .take(SHELF_SIZE),
    )

private fun Track.plays(counts: Map<Long, Int>): Int = counts[id] ?: 0

private fun List<Track>.plays(counts: Map<Long, Int>): Int = sumOf { it.plays(counts) }

private fun <T> List<T>.mostPlayed(plays: (T) -> Int): List<T> =
    map { it to plays(it) }
        .filter { (_, played) -> played > 0 }
        .sortedByDescending { (_, played) -> played }
        .take(SHELF_SIZE)
        .map { (item, _) -> item }
