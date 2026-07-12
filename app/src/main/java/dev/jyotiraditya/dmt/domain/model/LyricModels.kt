package dev.jyotiraditya.dmt.domain.model

enum class Voice { PRIMARY, SECONDARY, GROUP }

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val start: Int,
    val end: Int,
    val background: Boolean,
)

data class Transliteration(val text: String, val words: List<LyricWord> = emptyList())

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val voice: Voice = Voice.PRIMARY,
    val singer: Int = 0,
    val sectionStart: Boolean = false,
    val interlude: Boolean = false,
    val translation: List<String> = emptyList(),
    val transliteration: Transliteration? = null,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
)
