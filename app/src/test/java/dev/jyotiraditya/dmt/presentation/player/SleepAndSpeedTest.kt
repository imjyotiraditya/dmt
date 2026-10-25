package dev.jyotiraditya.dmt.presentation.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepAndSpeedTest {

    @Test
    fun speedRoundsToTheNearestStep() {
        assertEquals(1.15f, steppedSpeed(1.149f))
        assertEquals(1.25f, steppedSpeed(1.26f))
        assertEquals(1f, steppedSpeed(0.999f))
    }

    @Test
    fun speedStepsByFiveHundredths() {
        assertEquals(1.05f, steppedSpeed(1f, steps = 1))
        assertEquals(0.95f, steppedSpeed(1f, steps = -1))
        assertEquals(1.5f, steppedSpeed(1.25f, steps = 5))
    }

    @Test
    fun speedStaysBetweenHalfAndDouble() {
        assertEquals(0.5f, steppedSpeed(0.5f, steps = -1))
        assertEquals(2f, steppedSpeed(2f, steps = 1))
        assertEquals(2f, steppedSpeed(3f))
    }

    @Test
    fun sleepStepsByFiveMinutesWithinItsRange() {
        assertEquals(25, steppedSleepMinutes(20, steps = 1))
        assertEquals(15, steppedSleepMinutes(20, steps = -1))
        assertEquals(5, steppedSleepMinutes(5, steps = -1))
        assertEquals(180, steppedSleepMinutes(180, steps = 1))
    }
}
