package dev.jyotiraditya.lyrics.lrc

import dev.jyotiraditya.lyrics.LyricsParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeTagTest {

    @Test
    fun `reads tenths, hundredths and thousandths`() {
        assertEquals(61_500L, "[01:01.5]".timeTagAt(0, TagStyle.SQUARE)?.ms)
        assertEquals(61_050L, "[01:01.05]".timeTagAt(0, TagStyle.SQUARE)?.ms)
        assertEquals(61_005L, "[01:01.005]".timeTagAt(0, TagStyle.SQUARE)?.ms)
        assertEquals(61_000L, "<1:01>".timeTagAt(0, TagStyle.ANGLE)?.ms)
    }

    @Test
    fun `rejects header tags and malformed times`() {
        assertNull("[ar:Someone]".timeTagAt(0, TagStyle.SQUARE))
        assertNull("[01:234]".timeTagAt(0, TagStyle.SQUARE))
        assertNull("[01:23.4567]".timeTagAt(0, TagStyle.SQUARE))
        assertNull("[01:23>".timeTagAt(0, TagStyle.SQUARE))
    }

    @Test
    fun `finds every tag in one pass and skips brackets that are not tags`() {
        val tags = "[x] <00:01.00>a <bad> <00:02.00>b".timeTags(TagStyle.ANGLE)
        assertEquals(listOf(1_000L, 2_000L), tags.map { it.ms })
    }

    @Test
    fun `a devanagari line and its romanization at the same time pair up`() {
        val lyrics =
            LyricsParser.parse("[00:01.00]तेरे बिना\n[00:01.00]tere bina\n[00:05.00]next")!!
        val line = lyrics.lines.first()
        assertEquals("तेरे बिना", line.text)
        assertEquals("tere bina", line.transliteration?.text)
    }
}
