package dev.jyotiraditya.dmt.library

/** The metadata of a track, as read from the file itself rather than from an index. */
data class TrackMetadata(
    val durationMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
)
