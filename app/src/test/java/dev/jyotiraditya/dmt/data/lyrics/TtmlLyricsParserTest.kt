package dev.jyotiraditya.dmt.data.lyrics

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/lyrics/$name")) { "missing fixture $name" }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TtmlLyricsParserTest {

    @Test
    fun `single voice ttml with background vocals parses cleanly`() {
        val lyrics = parseTtml(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertTrue(lyrics.lines.isNotEmpty())
        assertTrue(lyrics.lines.all { it.voice == VOICE_PRIMARY })
        assertTrue(lyrics.lines.any { line -> line.words.any { it.background } })

        for (line in lyrics.lines) {
            for (word in line.words) {
                assertTrue(word.start in 0..line.text.length)
                assertTrue(word.end in word.start..line.text.length)
            }
        }
    }

    @Test
    fun `multi-voice ensemble ttml assigns distinct voices and a group voice`() {
        val lyrics = parseTtml(fixture("ttml_multivoice.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val voicesUsed = lyrics.lines.map { it.voice }.toSet()
        assertTrue(voicesUsed.contains(VOICE_PRIMARY))
        assertTrue(voicesUsed.contains(VOICE_SECONDARY))
        assertTrue(voicesUsed.contains(VOICE_GROUP))

        val distinctSingers = lyrics.lines.map { it.singer }.filter { it >= 0 }.toSet()
        assertTrue(distinctSingers.size > 1)
    }
}
