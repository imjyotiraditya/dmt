package dev.jyotiraditya.dmt.data.source.local.cue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

private fun fixture(name: String): String =
    checkNotNull(object {}.javaClass.getResourceAsStream("/cue/$name")) {
        "missing fixture $name"
    }
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }

class CueParserTest {

    @Test
    fun `single file sheet parses album metadata and track starts`() {
        val sheet = CueParser.parse(fixture("brothers_in_arms.cue"))
        assertNotNull(sheet)
        assertEquals("Brothers In Arms (Full Length Version)", sheet!!.title)
        assertEquals("Dire Straits", sheet.performer)
        assertEquals(1, sheet.files.size)

        val file = sheet.files.first()
        assertEquals("Dire Straits - Brothers In Arms (Full Length Version).flac", file.name)
        assertEquals(listOf(1, 2, 3), file.tracks.map { it.number })
        // 06:59:00 -> 6 * 60_000 + 59 * 1_000, frames are 1/75s
        assertEquals(listOf(0L, 419_000L, 720_000L), file.tracks.map { it.startMs })
        assertEquals(
            "Going Home - Theme From 'Local Hero' (Live Version)",
            file.tracks[1].title,
        )
        // no per-track PERFORMER lines, so tracks inherit nothing
        assertNull(file.tracks[0].performer)
    }

    @Test
    fun `pregap uses index 01 and multiple files keep their own tracks`() {
        val sheet = CueParser.parse(fixture("pregap_multi_file.cue"))
        assertNotNull(sheet)
        assertEquals("Split Album", sheet!!.title)
        assertEquals(2, sheet.files.size)

        val one = sheet.files[0]
        assertEquals("disc one.wav", one.name)
        assertEquals("First Artist", one.tracks[0].performer)
        // INDEX 01 04:00:33 wins over INDEX 00; 33 frames = 440ms
        assertEquals(240_440L, one.tracks[1].startMs)

        // track with only INDEX 00 falls back to it
        val two = sheet.files[1]
        assertEquals(listOf(0L), two.tracks.map { it.startMs })
    }

    @Test
    fun `sheets without files or tracks are rejected`() {
        assertNull(CueParser.parse("REM GENRE Rock\nTITLE \"Nothing\""))
        assertNull(CueParser.parse("FILE \"a.flac\" WAVE\n  TRACK 01 AUDIO\n    TITLE \"x\""))
        assertNull(CueParser.parse(""))
    }

    @Test
    fun `unquoted file names with spaces drop the trailing type token`() {
        val sheet = CueParser.parse(
            """
            FILE disc one.flac WAVE
              TRACK 01 AUDIO
                INDEX 01 00:00:00
              TRACK 02 AUDIO
                INDEX 01 00:10:00
            """.trimIndent(),
        )
        assertNotNull(sheet)
        assertEquals("disc one.flac", sheet!!.files.first().name)
    }

    @Test
    fun `decode handles utf8 bom shift jis and utf16`() {
        val plain = "TITLE \"Test\""
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
                plain.toByteArray(Charsets.UTF_8)
        assertEquals(plain, CueParser.decode(bom))

        val japanese = "TITLE \"明滅\""
        assertEquals(japanese, CueParser.decode(japanese.toByteArray(charset("Shift_JIS"))))

        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) +
                plain.toByteArray(Charsets.UTF_16LE)
        assertEquals(plain, CueParser.decode(utf16))

        assertEquals(plain, CueParser.decode(plain.toByteArray(Charsets.UTF_8)))
    }
}
