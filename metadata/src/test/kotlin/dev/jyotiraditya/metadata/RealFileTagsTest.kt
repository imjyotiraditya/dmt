package dev.jyotiraditya.metadata

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private const val LYRICS_TEXT = "[00:00.10]a test tone\n[00:00.50]still a tone"

class RealFileTagsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun fixture(name: String): File {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/audio/$name")) {
            "missing fixture $name"
        }.use { it.readBytes() }

        return temp.newFile(name).apply { writeBytes(bytes) }
    }

    private fun audioTail(file: File, length: Int): ByteArray =
        file.readBytes().let { it.copyOfRange(it.size - length, it.size) }

    private fun oggPacketsExceptComment(file: File): ByteArray {
        val bytes = file.readBytes()
        val out = java.io.ByteArrayOutputStream()
        val packet = java.io.ByteArrayOutputStream()
        var pos = 0
        var index = 0
        while (pos + 27 <= bytes.size) {
            check(String(bytes, pos, 4, Charsets.ISO_8859_1) == "OggS") { "bad page at $pos" }
            val segments = bytes[pos + 26].toInt() and 0xFF
            var payloadPos = pos + 27 + segments
            (0 until segments).forEach { segment ->
                val lacing = bytes[pos + 27 + segment].toInt() and 0xFF
                packet.write(bytes, payloadPos, lacing)
                payloadPos += lacing
                if (lacing < 255) {
                    if (index != 1) packet.writeTo(out)
                    packet.reset()
                    index++
                }
            }
            pos = payloadPos
        }
        packet.writeTo(out)
        return out.toByteArray()
    }

    private fun mdatBytes(file: File): ByteArray {
        val bytes = file.readBytes()
        var pos = 0
        while (pos + 8 <= bytes.size) {
            val size = ((bytes[pos].toInt() and 0xFF) shl 24) or
                ((bytes[pos + 1].toInt() and 0xFF) shl 16) or
                ((bytes[pos + 2].toInt() and 0xFF) shl 8) or
                (bytes[pos + 3].toInt() and 0xFF)
            val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)
            if (type == "mdat") return bytes.copyOfRange(pos, pos + size)
            pos += size
        }
        error("no mdat box")
    }

    private fun assertLyricsRoundTrip(file: File) {
        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        val tags = AudioTags.read(file.path)
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
    }

    @Test
    fun `reads the tags an encoder wrote into a real flac`() {
        val tags = AudioTags.read(fixture("tone.flac").path)

        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
        assertNull(tags[TagKey.LYRICS])
    }

    @Test
    fun `reads the tags an encoder wrote into a real mp3`() {
        val tags = AudioTags.read(fixture("tone.mp3").path)

        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
        assertNull(tags[TagKey.LYRICS])
    }

    @Test
    fun `writing lyrics into a real flac keeps the other tags and the audio`() {
        val file = fixture("tone.flac")
        val tailBefore = audioTail(file, length = 2048)

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        val tags = AudioTags.read(file.path)
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
        assertArrayEquals(tailBefore, audioTail(file, length = 2048))
    }

    @Test
    fun `writing lyrics into a real mp3 keeps the other tags and the audio`() {
        val file = fixture("tone.mp3")
        val tailBefore = audioTail(file, length = 2048)

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        val tags = AudioTags.read(file.path)
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertArrayEquals(tailBefore, audioTail(file, length = 2048))
    }

    @Test
    fun `reads the tags an encoder wrote into a real opus`() {
        val tags = AudioTags.read(fixture("tone.opus").path)

        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
        assertNull(tags[TagKey.LYRICS])
    }

    @Test
    fun `reads the tags an encoder wrote into a real m4a`() {
        val tags = AudioTags.read(fixture("tone.m4a").path)

        assertEquals(listOf("Test Tone"), tags[TagKey.TITLE])
        assertEquals(listOf("DMT"), tags[TagKey.ARTIST])
        assertEquals(listOf("Fixtures"), tags[TagKey.ALBUM])
        assertNull(tags[TagKey.LYRICS])
    }

    @Test
    fun `writing lyrics into a real opus keeps the other tags and the audio`() {
        val file = fixture("tone.opus")
        val audioBefore = oggPacketsExceptComment(file)

        assertLyricsRoundTrip(file)

        assertArrayEquals(audioBefore, oggPacketsExceptComment(file))
    }

    @Test
    fun `writing lyrics into a real ogg vorbis keeps the other tags and the audio`() {
        val file = fixture("tone-vorbis.ogg")
        val audioBefore = oggPacketsExceptComment(file)

        assertLyricsRoundTrip(file)

        assertArrayEquals(audioBefore, oggPacketsExceptComment(file))
    }

    @Test
    fun `writing lyrics into a real aac m4a keeps the other tags and the audio`() {
        val file = fixture("tone.m4a")
        val mdatBefore = mdatBytes(file)

        assertLyricsRoundTrip(file)

        assertArrayEquals(mdatBefore, mdatBytes(file))
    }

    @Test
    fun `writing lyrics into a real alac m4a keeps the other tags and the audio`() {
        val file = fixture("tone-alac.m4a")
        val mdatBefore = mdatBytes(file)

        assertLyricsRoundTrip(file)

        assertArrayEquals(mdatBefore, mdatBytes(file))
    }

    @Test
    fun `writing lyrics into a faststart m4a patches the chunk offsets`() {
        val file = fixture("tone-faststart.m4a")
        val mdatBefore = mdatBytes(file)

        assertLyricsRoundTrip(file)

        assertArrayEquals(mdatBefore, mdatBytes(file))
    }

    @Test
    fun `rewriting a real opus twice does not grow it unbounded`() {
        val file = fixture("tone.opus")

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))
        val sizeAfterFirst = file.length()

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        assertEquals(sizeAfterFirst, file.length())
        assertEquals(listOf(LYRICS_TEXT), AudioTags.read(file.path)[TagKey.LYRICS])
    }

    @Test
    fun `rewriting a real m4a twice does not grow it unbounded`() {
        val file = fixture("tone.m4a")

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))
        val sizeAfterFirst = file.length()

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        assertEquals(sizeAfterFirst, file.length())
        assertEquals(listOf(LYRICS_TEXT), AudioTags.read(file.path)[TagKey.LYRICS])
    }

    @Test
    fun `rewriting a real flac twice does not grow it unbounded`() {
        val file = fixture("tone.flac")

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))
        val sizeAfterFirst = file.length()

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        assertEquals(sizeAfterFirst, file.length())
        assertEquals(listOf(LYRICS_TEXT), AudioTags.read(file.path)[TagKey.LYRICS])
    }
}
