package dev.jyotiraditya.dmt.data

import android.net.Uri

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
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
