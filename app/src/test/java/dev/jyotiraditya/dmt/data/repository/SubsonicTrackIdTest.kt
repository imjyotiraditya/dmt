package dev.jyotiraditya.dmt.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SubsonicTrackIdTest {

    @Test
    fun `same song id gives the same track id`() {
        assertEquals("7nZDCj4EKBPNp8u6TXdX4C".toTrackId(), "7nZDCj4EKBPNp8u6TXdX4C".toTrackId())
    }

    @Test
    fun `different song ids give different track ids`() {
        assertNotEquals("7nZDCj4EKBPNp8u6TXdX4C".toTrackId(), "7nZDCj4EKBPNp8u6TXdX4D".toTrackId())
    }

    @Test
    fun `hash is FNV-1a`() {
        assertEquals(-0x340d631b7bdddcdbL, "".toTrackId())
        assertEquals(-0x509c23b379fe1374L, "a".toTrackId())
    }
}
