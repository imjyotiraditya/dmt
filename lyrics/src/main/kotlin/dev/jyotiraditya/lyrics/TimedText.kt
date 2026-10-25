package dev.jyotiraditya.lyrics

/**
 * A line's translation or transliteration, with optional word timing.
 *
 * @property lang the BCP-47 language tag, such as `zh-Hant`, or null if not given.
 */
data class TimedText(
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val lang: String? = null,
)
