package dev.jyotiraditya.dmt.domain.model

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val COLLAB_SEPARATORS = Regex("""\s*[;/]\s*""")

val Track.primaryArtist: String
    get() = albumArtist.ifBlank { artist }

fun String.asCredit(): String = replace(COLLAB_SEPARATORS, " + ")

private val DISC_FOLDER = Regex("""/(cd|disc|disk)\s*\d+$""", RegexOption.IGNORE_CASE)

/**
 * The album this track belongs to: its name with its album artist, or with its folder when the tags
 * name no album artist, so albums that share a name stay apart.
 */
val Track.albumKey: String
    get() {
        val owner = albumArtist.ifBlank {
            path.substringBeforeLast('/', "").replace(DISC_FOLDER, "")
        }
        return "${album.lowercase()}\u001F${owner.lowercase()}"
    }

/** These tracks grouped into albums, artists, folders and genres at once. */
suspend fun List<Track>.toLibrary(): LibrarySnapshot = coroutineScope {
    val albums = async { toAlbums() }
    val artists = async { toArtists() }
    val folders = async { toFolders() }
    val genres = async { toGenres() }
    LibrarySnapshot(
        this@toLibrary,
        albums.await(),
        artists.await(),
        folders.await(),
        genres.await(),
    )
}

fun List<Track>.toFolders(): List<Folder> = asSequence()
    .filter { it.path.isNotEmpty() }
    .groupBy { it.path.substringBeforeLast('/') }
    .map { (dir, tracks) -> Folder(path = dir, tracks = tracks) }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.relativePath })

fun List<Track>.toArtists(): List<Artist> = groupBy {
    it.primaryArtist.lowercase()
}
    .map { (_, tracks) ->
        Artist(
            name = tracks.groupingBy { it.primaryArtist }.eachCount().maxBy { it.value }.key,
            albums = tracks.mapTo(HashSet()) { it.album }.size,
            tracks =
                tracks.sortedWith(
                    compareBy<Track, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                        .thenBy { it.discNumber }
                        .thenBy { it.trackNumber },
                ),
        )
    }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<Track>.toGenres(): List<Genre> = groupBy {
    it.genre?.takeIf(String::isNotBlank)?.lowercase() ?: UNKNOWN_GENRE
}
    .map { (_, tracks) ->
        Genre(
            name = tracks.first().genre?.takeIf(String::isNotBlank) ?: UNKNOWN_GENRE,
            tracks = tracks,
        )
    }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

fun List<Track>.toAlbums(): List<Album> = groupBy {
    it.albumKey
}
    .map { (id, tracks) ->
        val artists = tracks.mapTo(HashSet()) { it.primaryArtist.lowercase() }
        Album(
            id = id,
            name = tracks.first().album,
            artist = if (artists.size == 1) tracks.first().primaryArtist else "various artists",
            tracks = tracks.sortedWith(compareBy({ it.discNumber }, { it.trackNumber })),
        )
    }
    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
