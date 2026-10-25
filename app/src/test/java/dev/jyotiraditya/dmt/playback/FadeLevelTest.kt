package dev.jyotiraditya.dmt.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FadeLevelTest {

    @Test
    fun startsFullAndEndsSilent() {
        assertEquals(1f, fadeLevel(0f))
        assertEquals(0f, fadeLevel(1f))
    }

    @Test
    fun fallsAllTheWay() {
        val levels = (0..10).map { fadeLevel(it / 10f) }
        assertTrue(levels.zipWithNext().all { (a, b) -> b < a })
    }

    @Test
    fun staysWithinItsRangeOutsideTheFade() {
        assertEquals(1f, fadeLevel(-0.5f))
        assertEquals(0f, fadeLevel(1.5f))
    }
}
