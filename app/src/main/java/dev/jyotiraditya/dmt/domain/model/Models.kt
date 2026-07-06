package dev.jyotiraditya.dmt.domain.model

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
    val trackNumber: Int,
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

data class Spec(
    val label: String,
    val value: String,
    val hot: Boolean = false,
)

data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val folders: List<Folder> = emptyList(),
)
