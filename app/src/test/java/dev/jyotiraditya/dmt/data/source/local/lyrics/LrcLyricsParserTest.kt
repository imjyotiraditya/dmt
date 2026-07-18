package dev.jyotiraditya.dmt.data.source.local.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/lyrics/$name")) {
        "missing fixture $name"
    }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

class LrcLyricsParserTest {

    @Test
    fun `plain line-synced lrc has no word timing`() {
        val lyrics = LrcLyricsParser.parse(fixture("plain.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertTrue(lyrics.lines.isNotEmpty())
        assertTrue(lyrics.lines.all { it.words.isEmpty() })

        val first = lyrics.lines.first { !it.interlude }
        assertEquals(21_769L, first.startMs)
        assertEquals("I'm tired of being what you want me to be", first.text)
    }

    @Test
    fun `enhanced word-timed lrc extracts per-word timing`() {
        val lyrics = LrcLyricsParser.parse(fixture("enhanced.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val first = lyrics.lines.first { it.words.isNotEmpty() }
        assertTrue(first.words.isNotEmpty())
        for (word in first.words) {
            assertTrue(word.start in 0..first.text.length)
            assertTrue(word.end in word.start..first.text.length)
            assertTrue(word.endMs >= word.startMs)
        }
        // no leftover <mm:ss.xxx> word tags should remain in any displayed text
        assertTrue(lyrics.lines.none { it.text.contains("<") })
    }

    @Test
    fun `voice prefix and background lines are handled`() {
        val lyrics = LrcLyricsParser.parse(fixture("voice_bg.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // the "v1:" voice marker must never leak into displayed text
        assertTrue(lyrics.lines.none { it.text.startsWith("v1:") || it.text.contains("v1:") })

        // [bg: ... ] lines must survive as their own lines, fully word-timed and flagged background
        val bgLines = lyrics.lines.filter {
            it.words.isNotEmpty() &&
                    it.words.all { w -> w.background }
        }
        assertTrue(bgLines.isNotEmpty())
        val sampleBg = bgLines.first { it.text.startsWith("I'm") }
        assertEquals("I'm just tired of lookin' the other way", sampleBg.text)
        assertEquals(77_254L, sampleBg.startMs)
    }

    @Test
    fun `bg lines with a duplicated nested timestamp and voice prefix are cleaned`() {
        val lyrics = LrcLyricsParser.parse(fixture("voice_bg_nested.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // neither the outer nor the nested "[mm:ss.xxx]v1:" prefix should leak into any text
        assertTrue(lyrics.lines.none { it.text.contains("v1:") || it.text.contains("[") })

        // the bg line here is romaji of the same line (different script), so it's a
        // transliteration attached to the main line, not a separate background line
        val main = lyrics.lines.first { it.startMs == 373L }
        assertEquals("まる で 御伽 の 話", main.text)
        val transliteration = main.transliteration
        assertNotNull(transliteration)
        assertEquals("Maru de otogi no hanashi", transliteration!!.text)
        assertTrue(lyrics.lines.none { it.words.any { w -> w.background } })
    }

    @Test
    fun `duet lrc keeps voice sides, own line ends, and bg singer`() {
        val lyrics = LrcLyricsParser.parse(fixture("duet.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        // a line's end comes from its own final word stamp, not the start of the
        // next line that overlaps it
        val v3 = lines.first { it.text.startsWith("だから") }
        assertEquals(68_820L, v3.endMs)

        // the two singers land on different sides
        val v1 = lines.first { it.text.startsWith("限り有る") }
        assertNotEquals(v3.voice, v1.voice)

        // a standalone bg line keeps background words and inherits the singer it backs
        val bg = lines.first { it.startMs == 194_156L }
        assertTrue(bg.words.isNotEmpty())
        assertTrue(bg.words.all { it.background })
        assertEquals(lines.first { it.startMs == 193_265L }.singer, bg.singer)
    }

    @Test
    fun `same-timestamp bilingual lines merge into one line with transliteration`() {
        val lyrics = LrcLyricsParser.parse(fixture("bilingual.lrc"))
        assertNotNull(lyrics)

        val lines = lyrics!!.lines.filter { !it.interlude }

        // romaji + original script sharing a timestamp collapse into one line:
        // the original script stays as text, romaji attaches as transliteration
        val first = lines.first { it.startMs == 16_240L }
        assertEquals("単純なステージ", first.text)
        assertEquals("Tanjun na stage", first.transliteration?.text)

        // the merged line spans to the next line's start instead of the romaji
        // half getting a zero-length duration
        assertEquals(18_570L, first.endMs)

        // english-only lines have no partner and stay plain single lines
        val english = lines.first { it.text == "BLACK ROVER" }
        assertNull(english.transliteration)

        // every pair collapsed: no two remaining lines share a start time
        assertEquals(lines.size, lines.map { it.startMs }.distinct().size)
    }

    @Test
    fun `matches detects bracket timestamps only`() {
        assertTrue(LrcLyricsParser.matches(fixture("plain.lrc")))
        assertTrue(LrcLyricsParser.matches(fixture("enhanced.lrc")))
        assertFalse(LrcLyricsParser.matches("just some plain unsynced text\nwith multiple lines"))
    }

    @Test
    fun `parse returns null when there is nothing synced`() {
        assertNull(LrcLyricsParser.parse("no timestamps here at all"))
    }
}
