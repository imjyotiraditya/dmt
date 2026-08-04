package dev.jyotiraditya.lyrics

import dev.jyotiraditya.lyrics.lrc.LrcParser
import dev.jyotiraditya.lyrics.ttml.TtmlParser

/** Reads lyrics of any format this holds a parser for. */
object LyricsParser {

    /**
     * Returns the lyrics [raw] holds, read by the parser of whichever format it is written in.
     *
     * Text that is no format at all is taken as lyrics that were never synced, so that a file
     * holding nothing but words still shows them.
     *
     * @param raw The text to read.
     * @return The lyrics, or null if [raw] holds none.
     */
    fun parse(raw: String): Lyrics? {
        val trimmed = raw.trim()

        return when {
            trimmed.isEmpty() -> null

            trimmed.startsWith("<") && trimmed.contains("<tt") ->
                TtmlParser.parse(trimmed)

            LrcParser.matches(trimmed) ->
                LrcParser.parse(trimmed)

            else -> parsePlain(trimmed)
        }
    }

    /** Returns [trimmed] as one unsynced line per line of text. */
    private fun parsePlain(trimmed: String): Lyrics {
        val lines = trimmed.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { text ->
                LyricLine(
                    startMs = -1L,
                    endMs = -1L,
                    text = text,
                )
            }

        return Lyrics(
            lines = lines,
            synced = false,
        )
    }
}
