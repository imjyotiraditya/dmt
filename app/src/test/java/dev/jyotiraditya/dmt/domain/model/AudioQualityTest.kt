package dev.jyotiraditya.dmt.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioQualityTest {

    @Test
    fun losslessUpToStandardRate() {
        assertEquals(AudioQuality.LOSSLESS, AudioQuality.of("audio/flac", 44_100))
        assertEquals(AudioQuality.LOSSLESS, AudioQuality.of("audio/flac", 48_000))
        assertEquals(AudioQuality.LOSSLESS, AudioQuality.of("audio/alac", null))
        assertEquals(AudioQuality.LOSSLESS, AudioQuality.of("audio/pcm_s24le", 48_000))
    }

    @Test
    fun hiResAboveStandardRate() {
        assertEquals(AudioQuality.HI_RES_LOSSLESS, AudioQuality.of("audio/flac", 96_000))
        assertEquals(AudioQuality.HI_RES_LOSSLESS, AudioQuality.of("audio/x-wavpack", 192_000))
        assertEquals(AudioQuality.HI_RES_LOSSLESS, AudioQuality.of("audio/dsd", null))
    }

    @Test
    fun dolby() {
        assertEquals(AudioQuality.DOLBY, AudioQuality.of("audio/ac3", 48_000))
        assertEquals(AudioQuality.DOLBY, AudioQuality.of("audio/eac3", 48_000))
        assertEquals(AudioQuality.DOLBY, AudioQuality.of("audio/ac4", 48_000))
        assertEquals(AudioQuality.DOLBY, AudioQuality.of("audio/true-hd", 48_000))
        assertEquals(AudioQuality.DOLBY_ATMOS, AudioQuality.of("audio/eac3-joc", 48_000))
    }

    @Test
    fun lossyHasNoBadge() {
        assertNull(AudioQuality.of("audio/mpeg", 44_100))
        assertNull(AudioQuality.of("audio/mp4a-latm mp4a.40.2", 44_100))
        assertNull(AudioQuality.of("audio/opus", 48_000))
    }
}
