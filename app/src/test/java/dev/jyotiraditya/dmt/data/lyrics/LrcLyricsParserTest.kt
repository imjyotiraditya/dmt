package dev.jyotiraditya.dmt.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/lyrics/$name")) { "missing fixture $name" }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

class LrcLyricsParserTest {

    @Test
    fun `plain line-synced lrc has no word timing`() {
        val lyrics = parseLrc(fixture("plain.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertTrue(lyrics.lines.isNotEmpty())
        assertTrue(lyrics.lines.all { it.words.isEmpty() })

        val first = lyrics.lines.first()
        assertEquals(21_769L, first.startMs)
        assertEquals("I'm tired of being what you want me to be", first.text)
    }

    @Test
    fun `enhanced word-timed lrc extracts per-word timing`() {
        val lyrics = parseLrc(fixture("enhanced.lrc"))
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
        val lyrics = parseLrc(fixture("voice_bg.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        // the "v1:" voice marker must never leak into displayed text
        assertTrue(lyrics.lines.none { it.text.startsWith("v1:") || it.text.contains("v1:") })

        // [bg: ... ] lines must survive as their own lines, fully word-timed and flagged background
        val bgLines = lyrics.lines.filter { it.words.isNotEmpty() && it.words.all { w -> w.background } }
        assertTrue(bgLines.isNotEmpty())
        val sampleBg = bgLines.first { it.text.startsWith("I'm") }
        assertEquals("I'm just tired of lookin' the other way", sampleBg.text)
        assertEquals(77_254L, sampleBg.startMs)
    }

    @Test
    fun `isLrc detects bracket timestamps only`() {
        assertTrue(isLrc(fixture("plain.lrc")))
        assertTrue(isLrc(fixture("enhanced.lrc")))
        assertFalse(isLrc("just some plain unsynced text\nwith multiple lines"))
    }

    @Test
    fun `parseLrc returns null when there is nothing synced`() {
        assertNull(parseLrc("no timestamps here at all"))
    }
}
