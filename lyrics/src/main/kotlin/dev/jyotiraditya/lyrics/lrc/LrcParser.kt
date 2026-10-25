package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.Lyrics
import dev.jyotiraditya.lyrics.TimedText
import dev.jyotiraditya.lyrics.assignSides
import dev.jyotiraditya.lyrics.fillLineEnds
import dev.jyotiraditya.lyrics.isReadingOf
import dev.jyotiraditya.lyrics.markInstrumentalLines
import dev.jyotiraditya.lyrics.mergeSimultaneousDuplicates
import dev.jyotiraditya.lyrics.scriptOf
import dev.jyotiraditya.lyrics.stripBackgroundBrackets
import dev.jyotiraditya.lyrics.withInterludes
import java.lang.Character.UnicodeScript
import kotlin.math.abs

/**
 * Parses line-timed and enhanced LRC. `vN:` marks the singer of a line, `[bg: ...]` a backing
 * vocal, and lines starting together become one line with its transliteration and translations.
 */
object LrcParser {

    /** Whether [raw] holds at least one LRC line time. */
    fun matches(raw: String): Boolean = raw.containsTimeTag(TagStyle.SQUARE)

    /** The lyrics in [raw], or null if no line in it is timed. */
    fun parse(raw: String): Lyrics? {
        val reader = LrcLineReader()
        raw.lineSequence().forEach(reader::read)
        if (reader.lines.isEmpty()) return null
        return Lyrics(
            lines =
                reader.lines
                    .sortedBy { it.startMs }
                    .markInstrumentalLines()
                    .stripBackgroundBrackets()
                    .pairReadings()
                    .fillLineEnds()
                    .mergeSimultaneousDuplicates()
                    .assignSides()
                    .withInterludes(),
            synced = true,
        )
    }
}

private val BACKGROUND_LINE = Regex("""^\[bg:(.*)]$""", RegexOption.IGNORE_CASE)

private val VOICE_PREFIX = Regex("""^v(\d+):""")

private class LrcLineReader {
    val lines = mutableListOf<LyricLine>()

    /** Singer index by voice number, in the order the voices first appear. */
    private val singers = HashMap<Int, Int>()

    fun read(source: String) {
        val background = BACKGROUND_LINE.matchEntire(source.trim())
        if (background != null) {
            readBackground(background.groupValues[1])
            return
        }

        var stamps = source.leadingTimeTags()
        if (stamps.isEmpty()) return
        // Square tags after the text are word times, so only the first leading tag is the line's.
        val rest = source.substring(stamps.last().end)
        if (rest.containsTimeTag(TagStyle.SQUARE)) stamps = stamps.take(1)

        val (body, voice) = source.substring(stamps.last().end).withoutVoice()
        val timed = readWordTimes(body)
        if (timed.text.isBlank()) return

        val singer = voice?.let { singers.getOrPut(it) { singers.size } } ?: 0
        val firstStartMs = stamps.first().ms
        stamps.forEach { stamp ->
            val words = timed.words.shiftedTo(stamp.ms, firstStartMs)
            lines +=
                LyricLine(
                    startMs = stamp.ms,
                    endMs = words.lastOrNull()?.endMs ?: LyricLine.UNTIMED,
                    text = timed.text,
                    words = words,
                    singer = singer,
                )
        }
    }

    /** Reads a backing line, or the transliteration of the line before it. */
    private fun readBackground(content: String) {
        val trimmed = content.trim()
        val stamp = trimmed.timeTagAt(0, TagStyle.SQUARE)
        val (body, _) = trimmed.substring(stamp?.end ?: 0).withoutVoice()
        val timed = readWordTimes(body)
        if (timed.text.isBlank() || timed.words.isEmpty()) return

        val previous = lines.lastOrNull()
        if (stamp != null && previous != null && isReadingOf(previous.text, timed.text)) {
            lines[lines.lastIndex] = previous.copy(transliteration = timed)
            return
        }
        val words = timed.words.map { it.copy(background = true) }
        lines +=
            LyricLine(
                startMs = words.first().startMs,
                endMs = words.last().endMs,
                text = timed.text,
                words = words,
                singer = previous?.singer ?: 0,
            )
    }

    private fun String.withoutVoice(): Pair<String, Int?> {
        val trimmed = trim()
        val match = VOICE_PREFIX.find(trimmed) ?: return trimmed to null
        return trimmed.substring(match.range.last + 1).trimStart() to
            match.groupValues[1].toIntOrNull()
    }
}

/** The largest gap between start times of lines that are merged into one. */
private const val TOGETHER_MS = 150L

/**
 * These lines with lines starting together merged: the Latin one becomes the transliteration and
 * any further ones translations. Expects them sorted.
 */
private fun List<LyricLine>.pairReadings(): List<LyricLine> {
    val result = ArrayList<LyricLine>(size)
    for (line in this) {
        val last = result.lastOrNull()
        val together =
            last != null &&
                line.transliteration == null &&
                abs(last.startMs - line.startMs) <= TOGETHER_MS
        when {
            !together -> result += line

            last.transliteration == null && isReadingOf(last.text, line.text) -> {
                val lineIsLatin = scriptOf(line.text) == UnicodeScript.LATIN
                val (original, reading) = if (lineIsLatin) last to line else line to last
                result[result.lastIndex] =
                    original.copy(
                        endMs = maxOf(last.endMs, line.endMs),
                        transliteration = TimedText(reading.text, reading.words),
                    )
            }

            last.transliteration != null ->
                result[result.lastIndex] =
                    last.copy(
                        endMs = maxOf(last.endMs, line.endMs),
                        translation = last.translation + TimedText(line.text, line.words),
                    )

            else -> result += line
        }
    }
    return result
}
