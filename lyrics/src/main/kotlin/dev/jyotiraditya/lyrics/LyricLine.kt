package dev.jyotiraditya.lyrics

/**
 * One line of lyrics.
 *
 * @property startMs when the line starts, or [UNTIMED] in lyrics that are not synced.
 * @property endMs when the line ends, or [UNTIMED] in lyrics that are not synced.
 * @property words the timed words of [text]; empty when only the line is timed.
 * @property voice the side of a duet the line is drawn on.
 * @property singer who sings the line, numbered in order of appearance, or [NO_SINGER].
 * @property interlude whether the line stands for a stretch nobody sings.
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
) {
    companion object {
        /** The time of a line in lyrics that are not synced. */
        const val UNTIMED = -1L

        /** The singer of an interlude, or of a line several soloists share. */
        const val NO_SINGER = -1
    }
}
