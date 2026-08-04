package dev.jyotiraditya.lyrics

/**
 * A chunk of text with its own optional word timing, separate from whatever
 * line it's attached to. Used for a [LyricLine.transliteration] or one entry
 * in [LyricLine.translation].
 *
 * @property lang a BCP-47 tag like `en` or `zh-Hant` when the source bothered
 *   to tag one, so multiple languages can be told apart in the UI. Null if
 *   the source didn't tag it.
 */
data class TimedText(val text: String, val words: List<LyricWord> = emptyList(), val lang: String? = null)
