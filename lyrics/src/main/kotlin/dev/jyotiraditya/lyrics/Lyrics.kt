package dev.jyotiraditya.lyrics

/** Parse result. Either synced lines with real timestamps, or plain unsynced text. */
data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
)
