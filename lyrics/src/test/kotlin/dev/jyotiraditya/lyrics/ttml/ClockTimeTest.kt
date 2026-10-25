package dev.jyotiraditya.lyrics.ttml

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricsParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTimeTest {

    @Test
    fun `reads offset and clock times`() {
        assertEquals(1_500L, parseClockTime("1.5s"))
        assertEquals(1_500L, parseClockTime("1500ms"))
        assertEquals(12_345L, parseClockTime("12.345"))
        assertEquals(62_345L, parseClockTime("1:02.345"))
        assertEquals(3_662_345L, parseClockTime("1:01:02.345"))
    }

    @Test
    fun `rounds instead of truncating binary fractions`() {
        assertEquals(1_001L, parseClockTime("1.001"))
    }

    @Test
    fun `missing or malformed times are untimed`() {
        assertEquals(LyricLine.UNTIMED, parseClockTime(null))
        assertEquals(LyricLine.UNTIMED, parseClockTime(" "))
        assertEquals(LyricLine.UNTIMED, parseClockTime("soon"))
        assertEquals(LyricLine.UNTIMED, parseClockTime("1:2:3:4"))
    }

    @Test
    fun `translations written after the body still attach to their line`() {
        val ttml =
            """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
              <body><div>
                <p begin="1s" end="2s" itunes:key="L1"><span begin="1s" end="2s">hola</span></p>
              </div></body>
              <head><metadata><iTunesMetadata><translations>
                <translation xml:lang="en"><text for="L1">hello</text></translation>
              </translations></iTunesMetadata></metadata></head>
            </tt>
            """
                .trimIndent()
        val line = LyricsParser.parse(ttml)!!.lines.single { !it.interlude }
        assertEquals("hola", line.text)
        assertEquals("hello", line.translation.single().text)
        assertEquals("en", line.translation.single().lang)
    }

    @Test
    fun `a reading that only repeats its line is dropped`() {
        val ttml =
            """
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:lrc="http://lrc.red/lyric-ttml-internal">
              <head><metadata><transliterations><transliteration xml:lang="ja-Latn">
                <text for="L1"><span begin="1s" end="2s">Hello there,</span></text>
              </transliteration></transliterations></metadata></head>
              <body><div><p begin="1s" end="2s" lrc:key="L1"><span begin="1s" end="2s">hello there</span></p></div></body>
            </tt>
            """
                .trimIndent()
        val line = LyricsParser.parse(ttml)!!.lines.single { !it.interlude }
        assertEquals(null, line.transliteration)
    }
}
