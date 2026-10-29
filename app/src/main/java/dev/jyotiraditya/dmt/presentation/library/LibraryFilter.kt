package dev.jyotiraditya.dmt.presentation.library

import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Genre
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.Track

/** The parts of a library that match [query], with the tracks in the chosen order. */
data class FilteredLibrary(
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val genres: List<Genre> = emptyList(),
) {
    /** Whether these are search results rather than the whole library. */
    val searching: Boolean
        get() = query.isNotBlank()
}

/** This library narrowed to what contains [query], ignoring case; a blank query keeps all. */
internal fun LibrarySnapshot.filtered(query: String, sort: LibrarySort): FilteredLibrary {
    val q = query.trim()
    fun String.hit() = contains(q, ignoreCase = true)
    return FilteredLibrary(
        query = query,
        tracks =
            tracksSortedBy(sort).matching(q) {
                it.title.hit() || it.artist.hit() || it.album.hit()
            },
        albums = albums.matching(q) { it.name.hit() || it.artist.hit() },
        artists = artists.matching(q) { it.name.hit() },
        folders = folders.matching(q) { it.relativePath.hit() },
        genres = genres.matching(q) { it.name.hit() },
    )
}

private inline fun <T> List<T>.matching(query: String, matches: (T) -> Boolean): List<T> =
    if (query.isEmpty()) this else filter(matches)
