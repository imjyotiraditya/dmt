package dev.jyotiraditya.dmt.library.cue

/**
 * A cue sheet, which says what a rip of a disc holds.
 *
 * A sheet usually names one file and lists the tracks within it, but it may name several, as a rip
 * of a disc per file also has a sheet describing the release as a whole.
 */
data class CueSheet(
    val title: String?,
    val performer: String?,
    val files: List<CueFile>,
)
