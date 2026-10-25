package dev.jyotiraditya.dmt.presentation.player

import dev.jyotiraditya.dmt.domain.model.LyricsTiming
import dev.jyotiraditya.lyrics.LyricLine
import dev.jyotiraditya.lyrics.Lyrics
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class LyricsSourceStatusTest {

    @Test
    fun lyricsAreFoundWithTheirTiming() {
        val lyrics = Lyrics(listOf(LyricLine(0, 1_000, "Hello")), synced = true)
        assertEquals(
            LyricsSourceStatus.Found(LyricsTiming.LINE),
            Result.success<Lyrics?>(lyrics).toSourceStatus(),
        )
    }

    @Test
    fun noLyricsIsMissing() {
        assertEquals(LyricsSourceStatus.Missing, Result.success<Lyrics?>(null).toSourceStatus())
    }

    @Test
    fun aFailureIsUnreachable() {
        assertEquals(
            LyricsSourceStatus.Unreachable,
            Result.failure<Lyrics?>(IOException()).toSourceStatus(),
        )
    }
}
