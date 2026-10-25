package dev.jyotiraditya.dmt.data.remote.subsonic

import dev.jyotiraditya.dmt.domain.model.UNKNOWN_ARTIST
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubsonicTest {

    @Test
    fun `token matches the example in the Subsonic API docs`() {
        assertEquals("26719a1196d2a940705a59634eb18eab", subsonicToken("sesame", "c19b2d"))
    }

    @Test
    fun `new accounts get a fresh salt and its token`() {
        val first = SubsonicAccount.create("https://music.example.com", "dmt", "sesame")
        val second = SubsonicAccount.create("https://music.example.com", "dmt", "sesame")
        assertNotEquals(first.salt, second.salt)
        assertEquals(subsonicToken("sesame", first.salt), first.token)
    }

    @Test
    fun `server address loses the web app and API paths`() {
        assertEquals(
            "https://music.example.com",
            normalizeSubsonicUrl("music.example.com/app/#/album"),
        )
        assertEquals("http://10.0.0.2:4533", normalizeSubsonicUrl(" http://10.0.0.2:4533/rest/ "))
        assertEquals(
            "https://example.com/navidrome",
            normalizeSubsonicUrl("example.com/navidrome/"),
        )
    }

    @Test
    fun `milliseconds become an LRC time`() {
        assertEquals("00:00.00", 0L.lrcTime())
        assertEquals("01:02.34", 62_345L.lrcTime())
        assertEquals("12:00.99", 720_999L.lrcTime())
    }

    @Test
    fun `synced lines carry their times and unsynced lines stay plain`() {
        val lines = listOf(LyricLineDto(start = 1_500, value = "one"), LyricLineDto(value = "two"))
        assertEquals("[00:01.50]one\ntwo", StructuredLyrics(synced = true, line = lines).asLrc())
        assertEquals("one\ntwo", StructuredLyrics(synced = false, line = lines).asLrc())
    }

    @Test
    fun `m4a with a bit depth is ALAC`() {
        assertEquals(
            "audio/alac",
            song(suffix = "m4a", contentType = "audio/mp4", bitDepth = 24).mime(),
        )
        assertEquals("audio/mp4", song(suffix = "m4a", contentType = "audio/mp4").mime())
        assertEquals("audio/flac", song(suffix = "flac").mime())
        assertEquals("audio/?", song().mime())
    }

    @Test
    fun `songs map lengths, rates and missing artists`() {
        val mapped =
            song(
                duration = 213,
                bitRate = 2304,
                bitDepth = 0,
                samplingRate = 48_000,
            )
                .toSong()
        assertEquals(213_000L, mapped.durationMs)
        assertEquals(2_304_000, mapped.bitrate)
        assertNull(mapped.bitDepth)
        assertEquals(48_000, mapped.sampleRateHz)
        assertEquals(UNKNOWN_ARTIST, mapped.artist)
    }

    @Test
    fun `display artist wins over the plain artist`() {
        assertEquals("A & B", song(artist = "A", displayArtist = "A & B").toSong().artist)
    }

    private fun song(
        suffix: String? = null,
        contentType: String? = null,
        bitDepth: Int? = null,
        duration: Long? = null,
        bitRate: Int? = null,
        samplingRate: Int? = null,
        artist: String? = null,
        displayArtist: String? = null,
    ) = SongDto(
        id = "id",
        title = "Idol",
        artist = artist,
        displayArtist = displayArtist,
        duration = duration,
        bitRate = bitRate,
        suffix = suffix,
        contentType = contentType,
        bitDepth = bitDepth,
        samplingRate = samplingRate,
    )
}
