package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.Voice
import org.junit.Assert.assertEquals
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

class TtmlLyricsParserTest {

    @Test
    fun `single voice ttml with background vocals parses cleanly`() {
        val lyrics = TtmlParser.parse(fixture("ttml_single.ttml"))
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
        val lyrics = TtmlParser.parse(fixture("ttml_multivoice.ttml"))
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
    fun `a named group agent keeps its own color when it overlaps a soloist's duplicate line`() {
        val lyrics = TtmlParser.parse(fixture("ttml_multivoice.ttml"))
        assertNotNull(lyrics)

        // ensemble line, no overlap, keeps its own singer
        val firstEnsembleLine = lyrics!!.lines.first { it.startMs == 85_063L }
        assertEquals(Voice.GROUP, firstEnsembleLine.voice)
        assertTrue(firstEnsembleLine.singer >= 0)

        // same ensemble, but Dolores echoes it right after, triggering a merge,
        // singer should stay the same, not drop to -1
        val mergedEnsembleLine = lyrics.lines.first { it.startMs == 89_713L }
        assertEquals(Voice.GROUP, mergedEnsembleLine.voice)
        assertEquals(firstEnsembleLine.singer, mergedEnsembleLine.singer)
    }

    @Test
    fun `translations block is attached to its matching line by itunes key`() {
        val lyrics = TtmlParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)

        val first = lyrics!!.lines.first { it.startMs == 2_344L }
        assertEquals(listOf("This song is all, it's about you, baby"), first.translation.map { it.text })
        assertNull(first.transliteration)
    }

    @Test
    fun `translation with an x-bg clause splits into separate segments`() {
        val lyrics = TtmlParser.parse(fixture("ttml_single.ttml"))
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { it.startMs == 93_775L }
        assertEquals(
            listOf(
                "(They keep on asking me, \"Who is he?\")",
                "You show up, no matter how busy you are",
            ),
            line.translation.map { it.text },
        )
    }

    @Test
    fun `transliterations block is attached with its own word timing`() {
        val lyrics = TtmlParser.parse(fixture("ttml_transliteration.ttml"))
        assertNotNull(lyrics)

        val first = lyrics!!.lines.first { it.startMs == 1_594L }
        val transliteration = first.transliteration
        assertNotNull(transliteration)
        assertEquals("shizumu you ni tokete yuku you ni", transliteration!!.text)
        assertEquals(7, transliteration.words.size)
        assertTrue(first.translation.isEmpty())
    }

    @Test
    fun `inline x-translation and x-roman spans stay out of the sung text`() {
        val lyrics = TtmlParser.parse(fixture("ttml_inline_translations.ttml"))
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { it.startMs == 29_990L }
        assertEquals("永遠と見紛う闇と", line.text)
        assertEquals("Eien to mimagau yami to", line.transliteration?.text)
        assertTrue(
            line.translation.any { it.lang == "en" && it.text == "With darkness mistaken for eternity" },
        )
        assertEquals(10, line.translation.size)
    }

    @Test
    fun `multiple translation blocks in different languages all get captured`() {
        val lyrics = TtmlParser.parse(fixture("ttml_multi_translation_blocks.ttml"))
        assertNotNull(lyrics)

        val line = lyrics!!.lines.first { it.startMs == 120_185L }
        assertEquals(setOf("mn-Mong-CN", "zh-Hans"), line.translation.map { it.lang }.toSet())
        assertTrue(line.translation.any { it.text == "十五的月亮升上了天空哟" })
    }

    @Test
    fun `garbage input returns null instead of throwing`() {
        assertNull(TtmlParser.parse("<tt><this is not valid xml"))
    }

    @Test
    fun `distinct named group agents each get their own singer index`() {
        val lyrics = TtmlParser.parse(fixture("ttml_named_groups.ttml"))
        assertNotNull(lyrics)
        assertTrue(lyrics!!.synced)

        val groupLines = lyrics.lines.filter { it.voice == Voice.GROUP && !it.interlude }
        assertTrue(groupLines.isNotEmpty())

        // ten different group agents (v1000..v1009) are declared and used in this
        // song, they shouldn't all collapse into the same generic singer
        val distinctGroupSingers = groupLines.map { it.singer }.distinct()
        assertTrue(distinctGroupSingers.size > 1)
        assertTrue(distinctGroupSingers.none { it < 0 })

        val firstGroupLine = lyrics.lines.first { it.startMs == 60_971L }
        assertEquals(Voice.GROUP, firstGroupLine.voice)
        assertTrue(firstGroupLine.singer >= 0)
    }

    @Test
    fun `spans split across source lines keep their word spacing`() {
        val lyrics = TtmlParser.parse(fixture("ttml_pretty_printed.ttml"))
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
