package dev.jyotiraditya.metadata

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File

private const val AUDIO_MARKER = "AUDIO-FRAMES-1234"
private const val LYRICS_TEXT = "[00:01.00]a line\n[00:02.00]a 行 line"

class AudioTagsTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun leBytes(value: Int): ByteArray =
        byteArrayOf(
            value.toByte(),
            (value ushr 8).toByte(),
            (value ushr 16).toByte(),
            (value ushr 24).toByte(),
        )

    private fun vorbisBlock(entries: List<String>): ByteArray {
        val out = ByteArrayOutputStream()
        val vendor = "test".toByteArray()
        out.write(leBytes(vendor.size))
        out.write(vendor)
        out.write(leBytes(entries.size))
        entries.forEach {
            val bytes = it.toByteArray(Charsets.UTF_8)
            out.write(leBytes(bytes.size))
            out.write(bytes)
        }
        return out.toByteArray()
    }

    private fun flacFile(paddingSize: Int, entries: List<String>? = null): File {
        val out = ByteArrayOutputStream()
        out.write("fLaC".toByteArray())

        val blocks = mutableListOf<Pair<Int, ByteArray>>()
        blocks += 0 to ByteArray(34)
        entries?.let { blocks += 4 to vorbisBlock(it) }
        if (paddingSize >= 0) blocks += 1 to ByteArray(paddingSize)

        blocks.forEachIndexed { index, (type, data) ->
            val last = index == blocks.lastIndex
            out.write((type or if (last) 0x80 else 0))
            out.write((data.size ushr 16) and 0xFF)
            out.write((data.size ushr 8) and 0xFF)
            out.write(data.size and 0xFF)
            out.write(data)
        }
        out.write(AUDIO_MARKER.toByteArray())

        val file = temp.newFile("test-${System.nanoTime()}.flac")
        file.writeBytes(out.toByteArray())
        return file
    }

    private fun mp3File(paddingSize: Int): File {
        val title = byteArrayOf(0) + "Song".toByteArray(Charsets.ISO_8859_1)
        val frame = ByteArrayOutputStream()
        frame.write("TIT2".toByteArray())
        frame.write(byteArrayOf(0, 0, 0, title.size.toByte()))
        frame.write(byteArrayOf(0, 0))
        frame.write(title)
        val frames = frame.toByteArray()

        val tagSize = frames.size + paddingSize
        val out = ByteArrayOutputStream()
        out.write("ID3".toByteArray())
        out.write(byteArrayOf(3, 0, 0))
        out.write(
            byteArrayOf(
                ((tagSize ushr 21) and 0x7F).toByte(),
                ((tagSize ushr 14) and 0x7F).toByte(),
                ((tagSize ushr 7) and 0x7F).toByte(),
                (tagSize and 0x7F).toByte(),
            ),
        )
        out.write(frames)
        out.write(ByteArray(paddingSize))
        out.write(AUDIO_MARKER.toByteArray())

        val file = temp.newFile("test-${System.nanoTime()}.mp3")
        file.writeBytes(out.toByteArray())
        return file
    }

    private fun assertAudioIntact(file: File) {
        val bytes = file.readBytes()
        assertEquals(
            AUDIO_MARKER,
            String(bytes, bytes.size - AUDIO_MARKER.length, AUDIO_MARKER.length)
        )
    }

    @Test
    fun `flac reads every tag it carries`() {
        val file = flacFile(
            paddingSize = 64,
            entries = listOf("TITLE=Song", "ARTIST=Someone", "LYRICS=$LYRICS_TEXT"),
        )

        val tags = AudioTags.read(file.path)

        assertEquals(listOf("Song"), tags[TagKey.TITLE])
        assertEquals(listOf("Someone"), tags[TagKey.ARTIST])
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
    }

    @Test
    fun `flac writes any tag in place when padding fits`() {
        val file = flacFile(paddingSize = 512, entries = listOf("ARTIST=Someone"))
        val sizeBefore = file.length()

        assertTrue(
            AudioTags.write(
                file,
                temp.root,
                mapOf(TagKey.TITLE to "New Title", TagKey.LYRICS to LYRICS_TEXT),
            ),
        )

        assertEquals(sizeBefore, file.length())
        val tags = AudioTags.read(file.path)
        assertEquals(listOf("New Title"), tags[TagKey.TITLE])
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertEquals(listOf("Someone"), tags[TagKey.ARTIST])
        assertAudioIntact(file)
    }

    @Test
    fun `flac grows the header when there is no padding`() {
        val file = flacFile(paddingSize = -1)

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        assertEquals(listOf(LYRICS_TEXT), AudioTags.read(file.path)[TagKey.LYRICS])
        assertAudioIntact(file)
    }

    @Test
    fun `flac replaces an existing value and drops it when blank`() {
        val file = flacFile(paddingSize = 512, entries = listOf("TITLE=Old", "ARTIST=Someone"))

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.TITLE to "New")))
        assertEquals(listOf("New"), AudioTags.read(file.path)[TagKey.TITLE])

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.TITLE to "")))
        assertNull(AudioTags.read(file.path)[TagKey.TITLE])
        assertEquals(listOf("Someone"), AudioTags.read(file.path)[TagKey.ARTIST])
    }

    @Test
    fun `id3 reads and writes any tag, keeping the others`() {
        val file = mp3File(paddingSize = 512)
        val sizeBefore = file.length()

        assertEquals(listOf("Song"), AudioTags.read(file.path)[TagKey.TITLE])

        assertTrue(
            AudioTags.write(
                file,
                temp.root,
                mapOf(TagKey.ARTIST to "Someone", TagKey.LYRICS to LYRICS_TEXT),
            ),
        )

        assertEquals(sizeBefore, file.length())
        val tags = AudioTags.read(file.path)
        assertEquals(listOf("Song"), tags[TagKey.TITLE])
        assertEquals(listOf("Someone"), tags[TagKey.ARTIST])
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertAudioIntact(file)
    }

    @Test
    fun `id3 grows the tag when there is no padding`() {
        val file = mp3File(paddingSize = 0)

        assertTrue(AudioTags.write(file, temp.root, mapOf(TagKey.LYRICS to LYRICS_TEXT)))

        val tags = AudioTags.read(file.path)
        assertEquals(listOf(LYRICS_TEXT), tags[TagKey.LYRICS])
        assertEquals(listOf("Song"), tags[TagKey.TITLE])
        assertAudioIntact(file)
    }

    @Test
    fun `unknown formats read empty and refuse writes`() {
        val file = temp.newFile("noise.bin")
        file.writeBytes(ByteArray(64) { it.toByte() })

        assertTrue(AudioTags.read(file.path).isEmpty())
        assertFalse(AudioTags.canWrite(file.path))
        assertFalse(AudioTags.write(file, temp.root, mapOf(TagKey.TITLE to "x")))
    }
}
