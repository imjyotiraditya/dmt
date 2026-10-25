package dev.jyotiraditya.lyrics

import dev.jyotiraditya.lyrics.lrc.LrcParser
import dev.jyotiraditya.lyrics.lrc.TagStyle
import dev.jyotiraditya.lyrics.lrc.containsTimeTag

/** The formats lyrics come in, from the richest to the plainest. */
internal enum class LyricsFormat {
    TTML,

    /** LRC with word times and voice or backing-vocal tags. */
    ENRICHED_LRC,
    WORD_LRC,
    LINE_LRC,
    PLAIN,
    ;

    companion object {
        private val ENRICHED_TAG = Regex("""\[bg:|]\s*v\d+:""", RegexOption.IGNORE_CASE)

        /** The format [text] is written in. Expects [text] trimmed. */
        fun of(text: String): LyricsFormat {
            if (text.startsWith('<') && "<tt" in text) return TTML
            val wordTimed = text.containsTimeTag(TagStyle.ANGLE)
            return when {
                wordTimed && ENRICHED_TAG.containsMatchIn(text) -> ENRICHED_LRC
                wordTimed -> WORD_LRC
                LrcParser.matches(text) -> LINE_LRC
                else -> PLAIN
            }
        }
    }
}
