package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.TimedText
import dev.jyotiraditya.lyrics.alternateVoices
import dev.jyotiraditya.lyrics.fillLineEnds
import dev.jyotiraditya.lyrics.markInstrumentalLines
import dev.jyotiraditya.lyrics.mergeSimultaneousDuplicates
import dev.jyotiraditya.lyrics.withInterludes

/**
 * Parses the LRC family: plain line-synced LRC, the enhanced/A2 extension with
 * per-word `<mm:ss.xxx>` timing, and the community voice/background conventions
 * that got layered on top over the years.
 *
 * - `[mm:ss.xx]vN: text` is a line sung by voice `N`. `N` isn't capped at 2, we've
 *   seen `v1`/`v2`/`v3` in real duet and trio releases, so each distinct `N`
 *   becomes a stable [LyricLine.singer] index in the order it first shows up.
 * - `[bg: ...]` is a backing vocal line. If it has its own leading timestamp and
 *   reads in a different script than the line right before it, we treat it as
 *   that line's [LyricLine.transliteration] instead of a separate background line.
 * - When two or three lines share the same timestamp, they get folded into one:
 *   the first pair with different scripts becomes text plus
 *   [LyricLine.transliteration], anything after that becomes a
 *   [LyricLine.translation] entry.
 */
object LrcParser {


    /**
     * Whether [raw] is written in this format, which a line timestamp of its own tells.
     *
     * @param raw The text to look at.
     * @return Whether it holds at least one `[mm:ss]` timestamp.
     */
    fun matches(raw: String): Boolean = LINE_TIME.containsMatchIn(raw)

    /**
     * Returns the lyrics [raw] holds.
     *
     * @param raw The text to read.
     * @return The lyrics, or null if no line of [raw] carries a timestamp.
     */
    fun parse(raw: String): Lyrics? {
        val lines = mutableListOf<LyricLine>()
        val singers = mutableMapOf<Int, Int>()

        raw.lines().forEach { line ->
            val trimmedLine = line.trim()

            val bg = BG_LINE.matchEntire(trimmedLine)
            if (bg != null) {
                parseBackgroundLine(bg.groupValues[1], lines)
                return@forEach
            }

            val stamps = LINE_TIME.findAll(line).toList()
            if (stamps.isEmpty()) return@forEach

            val (rawText, voiceTag) = stripLinePrefix(line.substring(stamps.last().range.last + 1))
            if (rawText.isEmpty()) return@forEach

            val (text, words) = parseWordTags(rawText)
            if (text.isBlank()) return@forEach

            val singer = voiceTag?.let { tag -> singers.getOrPut(tag) { singers.size } } ?: 0

            stamps.forEach { match ->
                val startMs = match.toMs()
                if (startMs < 0) return@forEach

                lines += LyricLine(
                    startMs = startMs,
                    endMs = words.lastOrNull()?.endMs ?: -1L,
                    text = text,
                    words = words,
                    singer = singer,
                )
            }
        }

        if (lines.isEmpty()) return null

        return Lyrics(
            lines = lines.sortedBy { it.startMs }
                .markInstrumentalLines()
                .pairTransliterations()
                .fillLineEnds()
                .mergeSimultaneousDuplicates()
                .alternateVoices()
                .withInterludes(),
            synced = true,
        )
    }
}

/** The `vN:` a line opens with when a file says who of several singers sings it. */
private val VOICE_PREFIX = Regex("""^v(\d+):""")

/**
 * Returns [text] without what it opens with, and the singer it named if it named one.
 *
 * @param text The line to read.
 * @return The line as it reads, and the singer it opened with, or null if it named none.
 */
internal fun stripLinePrefix(text: String): Pair<String, Int?> {
    val withoutLeadingTime = LEADING_TIME.replaceFirst(text.trim(), "")
    val voice = VOICE_PREFIX.find(withoutLeadingTime)
    val stripped = withoutLeadingTime
        .substring(voice?.range?.let { it.last + 1 } ?: 0)
        .trimStart()

    return stripped to voice?.groupValues?.get(1)?.toIntOrNull()
}
