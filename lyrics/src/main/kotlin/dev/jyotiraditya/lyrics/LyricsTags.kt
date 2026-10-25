package dev.jyotiraditya.lyrics

object LyricsTags {

    private val LYRICS_KEYS = setOf("LYRICS", "UNSYNCEDLYRICS", "UNSYNCED LYRICS", "ELRC", "LRC")

    private const val LYRICS_KEY_PREFIX = "LYRICS-"

    /** The richest lyrics among [tags], keyed by upper-case name, or null if none hold any. */
    fun bestOf(tags: Map<String, List<String>>): String? = tags
        .asSequence()
        .filter { (key, _) -> key in LYRICS_KEYS || key.startsWith(LYRICS_KEY_PREFIX) }
        .flatMap { (_, values) -> values }
        .map(String::trim)
        .filter(String::isNotEmpty)
        // Formats are declared richest first.
        .minByOrNull { LyricsFormat.of(it).ordinal }
}
