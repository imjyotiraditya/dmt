package dev.jyotiraditya.dmt.library.cue

/** A track that a cue sheet lists, which covers the part of a file that starts at [startMs]. */
data class CueTrack(
    val number: Int,
    val title: String?,
    val performer: String?,
    /** Where the track starts in the file that holds it, in milliseconds. */
    val startMs: Long,
)
