package dev.jyotiraditya.dmt.data.lyrics

const val VOICE_PRIMARY = 0
const val VOICE_SECONDARY = 1
const val VOICE_GROUP = 2

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val start: Int,
    val end: Int,
    val background: Boolean,
)

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val voice: Int = VOICE_PRIMARY,
    val singer: Int = 0,
    val sectionStart: Boolean = false,
    val interlude: Boolean = false,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
)
