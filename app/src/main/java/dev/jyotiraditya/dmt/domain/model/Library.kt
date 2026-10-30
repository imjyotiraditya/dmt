package dev.jyotiraditya.dmt.domain.model

import androidx.compose.runtime.Immutable
import dev.jyotiraditya.dmt.core.model.Track
import java.util.concurrent.ConcurrentHashMap

/** An album; [id] tells apart albums that share a name. */
@Immutable
data class Album(val id: String, val name: String, val artist: String, val tracks: List<Track>)

@Immutable
data class Artist(val name: String, val albums: Int, val tracks: List<Track>) {
    /** This artist's own tracks grouped into albums. */
    val discography: List<Album> by lazy { tracks.toAlbums() }
}

@Immutable data class Genre(val name: String, val tracks: List<Track>)

@Immutable
data class Folder(val path: String, val tracks: List<Track>) {
    /** The path below shared storage, such as `Music/Albums`, or empty for its root. */
    val relativePath: String = path.removePrefix(SHARED_STORAGE_ROOT).trim('/')

    val name: String
        get() = relativePath.substringAfterLast('/')

    /** The folders above this one, such as `Music`, or empty at the top. */
    val location: String
        get() = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
}

const val SHARED_STORAGE_ROOT = "/storage/emulated/0"

@Immutable data class Playlist(val name: String, val tracks: List<Track>)

/** The tracks of a library and their groupings, with lookups built on first use. */
@Immutable
data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val genres: List<Genre> = emptyList(),
) {
    private val byId by lazy { tracks.associateBy { it.id } }
    private val albumsById by lazy { albums.associateBy { it.id } }
    private val artistsByKey by lazy { artists.associateBy { it.name.lowercase() } }
    private val foldersByPath by lazy { folders.associateBy { it.path } }
    private val genresByName by lazy { genres.associateBy { it.name } }
    private val sorted = ConcurrentHashMap<LibrarySort, List<Track>>()

    /** The tracks in [sort] order, sorted once per order. */
    fun tracksSortedBy(sort: LibrarySort): List<Track> =
        sorted.getOrPut(sort) { tracks.sortedWith(sort.comparator) }

    fun track(id: Long): Track? = byId[id]

    /** The track a player media id names, or null when it names none in this library. */
    fun track(mediaId: String?): Track? = mediaId?.toLongOrNull()?.let(byId::get)

    fun album(id: String): Album? = albumsById[id]

    fun artist(name: String): Artist? = artistsByKey[name.lowercase()]

    fun folder(path: String): Folder? = foldersByPath[path]

    fun genre(name: String): Genre? = genresByName[name]

    /** The album [track] is grouped under. */
    fun albumOf(track: Track): Album? = album(track.albumKey)

    /** The artist [track] is grouped under. */
    fun artistOf(track: Track): Artist? = artist(track.primaryArtist)
}
