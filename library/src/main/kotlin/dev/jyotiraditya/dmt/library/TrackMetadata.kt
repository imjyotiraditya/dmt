package dev.jyotiraditya.dmt.library

data class TrackMetadata(
    val durationMs: Long = 0L,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val trackNumber: Int = 0,
    val discNumber: Int = 0,
    val genre: String? = null,
)
