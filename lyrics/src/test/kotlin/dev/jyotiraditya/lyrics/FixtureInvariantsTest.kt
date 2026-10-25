package dev.jyotiraditya.lyrics

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

private fun fixtures(): List<Pair<String, String>> =
    File(object {}.javaClass.getResource("/lyrics")!!.toURI())
        .listFiles()
        .orEmpty()
        .sortedBy { it.name }
        .map { it.name to it.readText(Charsets.UTF_8) }

class FixtureInvariantsTest {

    @Test
    fun `every fixture parses into lines that hold together`() {
        val broken = mutableListOf<String>()

        fixtures().forEach { (name, raw) ->
            val lyrics = LyricsParser.parse(raw) ?: return@forEach

            fun complain(line: LyricLine, what: String) {
                broken += "$name @${line.startMs} \"${line.text.take(24)}\": $what"
            }

            lyrics.lines.zipWithNext { line, next ->
                if (next.startMs < line.startMs) complain(next, "starts before the line before it")
            }

            lyrics.lines.forEach { line ->
                if (lyrics.synced && line.endMs <= line.startMs) {
                    complain(line, "ends before it starts")
                }

                line.words.zipWithNext { word, next ->
                    if (next.start < word.end) complain(line, "words overlap in the text")

                    if (next.background == word.background && next.startMs < word.startMs) {
                        complain(line, "words are out of order in time")
                    }
                }

                line.words.forEach { word ->
                    if (word.start < 0 || word.end > line.text.length) {
                        complain(line, "a word falls outside the text")
                    }
                    if (word.endMs < word.startMs) complain(line, "a word ends before it starts")
                    if (word.startMs < line.startMs) complain(line, "a word starts before the line")
                    if (word.endMs > line.endMs) {
                        complain(line, "a word is sung past the line's end")
                    }
                }

                (line.translation + listOfNotNull(line.transliteration)).forEach { timed ->
                    timed.words.forEach { word ->
                        if (word.start < 0 || word.end > timed.text.length) {
                            complain(line, "a word of \"${timed.text.take(16)}\" falls outside it")
                        }
                    }
                }
            }
        }

        assertEquals(emptyList<String>(), broken)
    }
}
