package dev.jyotiraditya.dmt.domain.model

import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.LyricWord
import dev.jyotiraditya.lyrics.Lyrics
import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsTimingTest {

    private val line = LyricLine(4_000, 8_000, "Streetlights hum the long way home")

    @Test
    fun wordStampsMakeItWordTimed() {
        val word = LyricWord(4_000, 5_000, 0, 12, background = false)
        val lyrics = Lyrics(listOf(line.copy(words = listOf(word))), synced = true)
        assertEquals(LyricsTiming.WORD, lyrics.timing)
    }

    @Test
    fun lineStampsOnlyMakeItLineTimed() {
        assertEquals(LyricsTiming.LINE, Lyrics(listOf(line), synced = true).timing)
    }

    @Test
    fun unsyncedIsPlain() {
        assertEquals(LyricsTiming.PLAIN, Lyrics(listOf(line), synced = false).timing)
    }
}
