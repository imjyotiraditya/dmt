package dev.jyotiraditya.dmt.domain.model

import android.net.Uri
import androidx.compose.runtime.Immutable
import java.util.concurrent.ConcurrentHashMap

// The library writes these for missing tags, so both must agree.
const val UNKNOWN_TITLE = dev.jyotiraditya.dmt.library.UNKNOWN_TITLE
const val UNKNOWN_ARTIST = dev.jyotiraditya.dmt.library.UNKNOWN_ARTIST
const val UNKNOWN_ALBUM = dev.jyotiraditya.dmt.library.UNKNOWN_ALBUM
const val UNKNOWN_GENRE = "unknown genre"

enum class TrackSource {
    LOCAL,
    JELLYFIN,
    SUBSONIC,
}

@Immutable
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val albumArtist: String = "",
    val album: String,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val discNumber: Int = 0,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val coverUri: Uri? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val remoteId: String? = null,
    val clipStartMs: Long? = null,
    val clipEndMs: Long? = null,
    val cue: Boolean = false,
    val genre: String? = null,
    val bitDepth: Int? = null,
    val sampleRateHz: Int? = null,
)

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

@Immutable data class Spec(val label: String, val value: String, val hot: Boolean = false)

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
