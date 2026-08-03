package dev.jyotiraditya.dmt.library.cue

/** A file that a cue sheet names, along with the tracks it says the file holds. */
data class CueFile(
    val name: String,
    val tracks: List<CueTrack>,
)
