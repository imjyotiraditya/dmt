package dev.jyotiraditya.dmt.data.remote.lrcred

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LrcRedMatchTest {

    private val idol = LrcRedHit("JPP302300335", "Idol", "YOASOBI", duration = 213.234)
    private val cover =
        LrcRedHit(
            "QZK6J2340145",
            "Idol (From \"Oshi no Ko\") [English Cover]",
            "JubyPhonic",
            duration = 213.592,
        )

    @Test
    fun `picks the hit with the same title, artist and length`() {
        assertEquals(idol, bestMatch(listOf(cover, idol), "Idol", "YOASOBI", 213_000))
    }

    @Test
    fun `ignores case, accents, punctuation and version notes`() {
        val hit = LrcRedHit("X", "Beyoncé - Halo (Live)", "Beyoncé", duration = 261.0)
        assertEquals(hit, bestMatch(listOf(hit), "beyonce halo", "BEYONCE", 261_500))
    }

    @Test
    fun `rejects a different artist`() {
        assertNull(bestMatch(listOf(cover), "Idol", "YOASOBI", 213_000))
    }

    @Test
    fun `rejects a recording of a different length`() {
        assertNull(bestMatch(listOf(idol), "Idol", "YOASOBI", 240_000))
    }

    @Test
    fun `prefers the closest length among matching hits`() {
        val edit = LrcRedHit("EDIT", "Idol", "YOASOBI", duration = 211.0)
        assertEquals(idol, bestMatch(listOf(edit, idol), "Idol", "YOASOBI", 213_100))
    }

    @Test
    fun `accepts a featured artist on either side`() {
        val hit = LrcRedHit("FEAT", "Done for Me", "Charlie Puth & Kehlani", duration = 180.0)
        assertEquals(
            hit,
            bestMatch(listOf(hit), "Done for Me (feat. Kehlani)", "Charlie Puth", 180_200),
        )
    }

    @Test
    fun `prefers the hit from the same album when versions share a title and length`() {
        val english = LrcRedHit("EN", "Idol", "YOASOBI", "E-SIDE 3", 213.2)
        val original = LrcRedHit("JA", "Idol", "YOASOBI", "Idol - Single", 213.4)
        val hits = listOf(english, original)
        assertEquals(original, bestMatch(hits, "Idol", "YOASOBI", 213_000, "Idol - Single"))
    }
}
