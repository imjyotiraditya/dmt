package dev.jyotiraditya.dmt.domain.model

import dev.jyotiraditya.lyrics.Lyrics

/** How finely lyrics are timed. */
enum class LyricsTiming {
    WORD,
    LINE,
    PLAIN,
}

val Lyrics.timing: LyricsTiming
    get() =
        when {
            !synced -> LyricsTiming.PLAIN
            lines.any { it.words.isNotEmpty() } -> LyricsTiming.WORD
            else -> LyricsTiming.LINE
        }
