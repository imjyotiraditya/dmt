package dev.jyotiraditya.dmt.library.cue

/** A parsed cue sheet, which may reference several files, such as one per disc. */
data class CueSheet(val title: String?, val performer: String?, val files: List<CueFile>)
