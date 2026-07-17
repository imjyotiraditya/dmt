package dev.jyotiraditya.dmt.domain.model

import android.net.Uri

enum class TrackSource { LOCAL, JELLYFIN }

data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val path: String,
    val durationMs: Long,
    val mime: String,
    val bitrate: Int,
    val size: Long,
    val trackNumber: Int,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val coverUri: Uri? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val remoteId: String? = null,
    val clipStartMs: Long? = null,
    val clipEndMs: Long? = null,
)

data class Album(
    val name: String,
    val artist: String,
    val tracks: List<Track>,
)

data class Artist(
    val name: String,
    val albums: Int,
    val tracks: List<Track>,
)

data class Folder(
    val name: String,
    val path: String,
    val tracks: List<Track>,
)

data class Playlist(
    val name: String,
    val tracks: List<Track>,
)

data class Spec(
    val label: String,
    val value: String,
    val hot: Boolean = false,
)

data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val folders: List<Folder> = emptyList(),
)
