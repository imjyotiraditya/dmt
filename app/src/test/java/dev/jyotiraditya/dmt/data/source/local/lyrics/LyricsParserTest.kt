package dev.jyotiraditya.dmt.data.source.local.lyrics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/lyrics/$name")) {
        "missing fixture $name"
    }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyricsParserTest {

    @Test
    fun `routes ttml content to the ttml parser`() {
        val lyrics = LyricsParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
    }

    @Test
    fun `routes lrc content to the lrc parser`() {
        val lyrics = LyricsParser.parse(fixture("plain.lrc"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
    }

    @Test
    fun `falls back to unsynced plain text`() {
        val lyrics = LyricsParser.parse("just some words\nacross two lines")
        assertNotNull(lyrics)
        assertFalse(lyrics!!.synced)
        assertTrue(lyrics.lines.all { it.words.isEmpty() })
    }

    @Test
    fun `blank input yields no lyrics`() {
        assertNull(LyricsParser.parse("   "))
    }
}
