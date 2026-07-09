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
    val coverUri: Uri? = null,
    val source: TrackSource = TrackSource.LOCAL,
    val remoteId: String? = null,
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

/**
 * The playback signal flow shown by the Audio Inspector: track -> decoder -> processing ->
 * output -> output device. Every section is a list of [Spec] so the UI can render them
 * uniformly; a section is simply empty (and hidden) when nothing accurate can be reported.
 */
data class AudioJourney(
    val track: List<Spec> = emptyList(),
    val decoder: List<Spec> = emptyList(),
    val processing: List<Spec> = emptyList(),
    val output: List<Spec> = emptyList(),
    val outputDevice: List<Spec> = emptyList(),
    val advanced: List<Spec> = emptyList(),
)

data class LibrarySnapshot(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val folders: List<Folder> = emptyList(),
)
