package dev.jyotiraditya.dmt.data.source.local.lyrics

import dev.jyotiraditya.dmt.domain.model.Voice
import org.junit.Assert.assertEquals
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
class TtmlLyricsParserTest {

    @Test
    fun `single voice ttml with background vocals parses cleanly`() {
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)
        assertTrue(lyrics.lines.isNotEmpty())
        assertTrue(lyrics.lines.all { it.voice == Voice.PRIMARY })
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
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_multivoice.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val voicesUsed = lyrics.lines.map { it.voice }.toSet()
        assertTrue(voicesUsed.contains(Voice.PRIMARY))
        assertTrue(voicesUsed.contains(Voice.SECONDARY))
        assertTrue(voicesUsed.contains(Voice.GROUP))

        val distinctSingers = lyrics.lines.map { it.singer }.filter { it >= 0 }.toSet()
        assertTrue(distinctSingers.size > 1)
    }

    @Test
    fun `translations block is attached to its matching line by itunes key`() {
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)

        val first = lyrics!!.lines.first { it.startMs == 2_344L }
        assertEquals(listOf("This song is all, it's about you, baby"), first.translation)
        assertNull(first.transliteration)
    }

    @Test
    fun `translation with an x-bg clause splits into separate segments`() {
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { it.startMs == 93_775L }
        assertEquals(
            listOf(
                "(They keep on asking me, \"Who is he?\")",
                "You show up, no matter how busy you are",
            ),
            line.translation,
        )
    }

    @Test
    fun `transliterations block is attached with its own word timing`() {
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_transliteration.ttml"))
        assertNotNull(lyrics)

        val first = lyrics!!.lines.first { it.startMs == 1_594L }
        val transliteration = first.transliteration
        assertNotNull(transliteration)
        assertEquals("shizumu you ni tokete yuku you ni", transliteration!!.text)
        assertEquals(7, transliteration.words.size)
        assertTrue(first.translation.isEmpty())
    }

    @Test
    fun `spans split across source lines keep their word spacing`() {
        val lyrics = TtmlLyricsParser.parse(fixture("ttml_pretty_printed.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val sung = lyrics.lines.filter { !it.interlude }
        assertEquals(48, sung.size)
        assertEquals("Pour pint of that dirty", sung.first().text)

        val second = sung[1]
        assertEquals("Double me cup, I sip 'til I'm blurry", second.text)
        val wordTexts = second.words.map { second.text.substring(it.start, it.end) }
        assertEquals(
            listOf("Double", "me", "cup", ",", "I", "sip", "'til", "I'm", "blurry"),
            wordTexts,
        )
    }
}
