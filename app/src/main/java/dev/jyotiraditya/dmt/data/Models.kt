package dev.jyotiraditya.dmt.data

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
)

data class Album(
    val name: String,
    val artist: String,
    val tracks: List<Track>,
)

data class Folder(
    val name: String,
    val path: String,
    val tracks: List<Track>,
)

fun List<Track>.toFolders(): List<Folder> = asSequence()
    .filter { it.path.isNotEmpty() }
    .groupBy { it.path.substringBeforeLast('/') }
    .map { (dir, tracks) ->
        Folder(
            name = dir.removePrefix("/storage/emulated/0/").ifEmpty { "/" },
            path = dir,
            tracks = tracks,
        )
    }
    .sortedBy { it.name.lowercase() }

data class Spec(
    val label: String,
    val value: String,
    val hot: Boolean = false,
)

fun List<Track>.toAlbums(): List<Album> = groupBy { it.album }
    .map { (name, tracks) ->
        val artists = tracks.map { it.artist }.distinct()
        Album(name, artists.singleOrNull() ?: "various artists", tracks)
    }
    .sortedBy { it.name.lowercase() }
