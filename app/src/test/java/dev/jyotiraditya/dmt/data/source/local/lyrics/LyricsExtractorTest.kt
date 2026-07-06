package dev.jyotiraditya.dmt.data.source.local.lyrics

import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

private fun leBytes(value: Int): ByteArray =
    byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte(),
    )

private fun vorbisCommentBlock(vararg entries: Pair<String, String>): ByteArray {
    val out = ByteArrayOutputStream()
    val vendor = "test".toByteArray(Charsets.UTF_8)
    out.write(leBytes(vendor.size))
    out.write(vendor)
    out.write(leBytes(entries.size))
    entries.forEach { (key, value) ->
        val field = "$key=$value".toByteArray(Charsets.UTF_8)
        out.write(leBytes(field.size))
        out.write(field)
    }
    return out.toByteArray()
}

private fun flacFile(comment: ByteArray): File {
    val file = File.createTempFile("dmt-lyrics-test", ".flac")
    file.deleteOnExit()
    file.outputStream().use { out ->
        out.write("fLaC".toByteArray(Charsets.ISO_8859_1))
        out.write(0x84) // last-metadata-block flag + type 4 (VORBIS_COMMENT)
        out.write((comment.size shr 16) and 0xFF)
        out.write((comment.size shr 8) and 0xFF)
        out.write(comment.size and 0xFF)
        out.write(comment)
    }
    return file
}

class LyricsExtractorTest {

    @Test
    fun `synced LYRICS tag wins over UNSYNCEDLYRICS regardless of tag order`() {
        val file = flacFile(
            vorbisCommentBlock(
                "UNSYNCEDLYRICS" to "[Intro]\nplain unsynced text",
                "LYRICS" to "[00:01.00]synced line",
            ),
        )
        assertEquals("[00:01.00]synced line", LyricsExtractor.extract(file.path, "audio/flac"))
    }

    @Test
    fun `falls back to UNSYNCEDLYRICS when nothing synced is present`() {
        val file = flacFile(vorbisCommentBlock("UNSYNCEDLYRICS" to "[Intro]\nplain unsynced text"))
        assertEquals(
            "[Intro]\nplain unsynced text",
            LyricsExtractor.extract(file.path, "audio/flac"),
        )
    }

    @Test
    fun `ELRC and LRC rank above UNSYNCEDLYRICS`() {
        val file = flacFile(
            vorbisCommentBlock(
                "UNSYNCEDLYRICS" to "plain",
                "LRC" to "[00:01.00]line synced",
            ),
        )
        assertEquals("[00:01.00]line synced", LyricsExtractor.extract(file.path, "audio/flac"))
    }

    @Test
    fun `ranking is by content, not by which tag key holds it`() {
        // the "LYRICS" tag holding worse content than "LRC" shouldn't matter -
        // richer content wins even if it's under a less-canonical key name
        val file = flacFile(
            vorbisCommentBlock(
                "LYRICS" to "[Intro]\nplain unsynced text",
                "LRC" to "<00:01.00>word <00:02.00>timed<00:03.00>",
            ),
        )
        assertEquals(
            "<00:01.00>word <00:02.00>timed<00:03.00>",
            LyricsExtractor.extract(file.path, "audio/flac"),
        )
    }

    @Test
    fun `ttml content outranks everything else regardless of tag key`() {
        val ttml = "<tt xmlns=\"http://www.w3.org/ns/ttml\"><body/></tt>"
        val file = flacFile(
            vorbisCommentBlock(
                "ELRC" to "<00:01.00>word <00:02.00>timed<00:03.00>",
                "UNSYNCEDLYRICS" to ttml,
            ),
        )
        assertEquals(ttml, LyricsExtractor.extract(file.path, "audio/flac"))
    }
}
