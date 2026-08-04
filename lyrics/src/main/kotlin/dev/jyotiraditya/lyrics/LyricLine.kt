package dev.jyotiraditya.lyrics

/**
 * One line of lyrics, synced or not.
 *
 * @property singer stable 0-based id for whoever's singing, assigned in the
 *   order each voice tag first shows up (`vN` in LRC, `ttm:agent` in TTML). A
 *   named [Voice.GROUP] agent (TTML `type="group"`) gets its own index too,
 *   same as a solo singer. `-1` means the line has no declared agent at all:
 *   it's either an [interlude] or a group line synthesized by merging
 *   duplicate simultaneous lines from different singers. This is the field
 *   to key a color palette off of, since unlike [voice] it isn't capped at
 *   two.
 * @property translation zero or more full-line translations, can be more
 *   than one language, see [TimedText.lang].
 * @property transliteration a same-line reading, when the source gave one.
 */
data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
    val voice: Voice = Voice.PRIMARY,
    val singer: Int = 0,
    val sectionStart: Boolean = false,
    val interlude: Boolean = false,
    val translation: List<TimedText> = emptyList(),
    val transliteration: TimedText? = null,
)
