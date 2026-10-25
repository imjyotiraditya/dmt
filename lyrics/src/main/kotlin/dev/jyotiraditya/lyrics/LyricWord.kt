package dev.jyotiraditya.lyrics

/**
 * The timing of one word, or one syllable in CJK text, within its line.
 *
 * @property start inclusive character offset into the line's text.
 * @property end exclusive character offset into the line's text.
 * @property background whether the word is a backing vocal.
 */
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val start: Int,
    val end: Int,
    val background: Boolean,
)
