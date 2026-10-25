package dev.jyotiraditya.dmt.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NeutralFieldTest {

    @Test
    fun `a cover with no colour gets a grey, never a tint`() {
        val field = neutralField(0.5f)
        assertEquals(field.red, field.green, 0f)
        assertEquals(field.green, field.blue, 0f)
    }

    @Test
    fun `the grey follows the cover's brightness`() {
        assertEquals(0.5f, neutralField(0.5f).red, 0.01f)
    }

    @Test
    fun `black and white covers stay clear of pure black and pure white`() {
        assertEquals(0.14f, neutralField(0f).red, 0.01f)
        assertEquals(0.82f, neutralField(1f).red, 0.01f)
    }
}
