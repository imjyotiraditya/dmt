package dev.jyotiraditya.metadata

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

internal const val ID3_MARKER = "ID3"
private const val UNSYNC_FLAG = 0x80
private const val EXTENDED_FLAG = 0x40
private const val GROW_PADDING = 4096

private class Id3Header(val version: Int, val flags: Int, val size: Int)

private class Id3Frame(val id: String, val start: Int, val size: Int)

private fun parseHeader(header: ByteArray): Id3Header? {
    if (!header.startsWith(ID3_MARKER)) return null
    return Id3Header(
        version = header[3].toInt(),
        flags = header[5].toInt(),
        size = syncsafe(header, 6),
    )
}

private fun frames(bytes: ByteArray, version: Int, from: Int): List<Id3Frame> {
    val out = mutableListOf<Id3Frame>()
    var offset = from
    while (offset + 10 <= bytes.size) {
        if (bytes[offset] == 0.toByte()) break
        val id = String(bytes, offset, 4, Charsets.ISO_8859_1)
        val size = if (version == 4) syncsafe(bytes, offset + 4) else beInt(bytes, offset + 4)
        if (size <= 0 || offset + 10 + size > bytes.size) break
        out += Id3Frame(id, offset, size)
        offset += 10 + size
    }
    return out
}

private fun framesStart(bytes: ByteArray, header: Id3Header): Int =
    if (header.flags and EXTENDED_FLAG != 0) {
        val extendedSize = if (header.version == 4) syncsafe(bytes, 0) else beInt(bytes, 0)
        extendedSize.coerceAtLeast(4)
    } else {
        0
    }

private fun charsetFor(encoding: Int) = when (encoding) {
    0 -> Charsets.ISO_8859_1
    1 -> Charsets.UTF_16
    2 -> Charsets.UTF_16BE
    else -> Charsets.UTF_8
}

private fun decodeText(data: ByteArray): String? {
    if (data.isEmpty()) return null
    val text = String(data, 1, data.size - 1, charsetFor(data[0].toInt()))
    return text.trim { it <= ' ' }.ifEmpty { null }
}

private fun decodeUslt(data: ByteArray): String? {
    if (data.size < 5) return null

    val encoding = data[0].toInt()
    val wide = encoding == 1 || encoding == 2

    var index = 4
    if (wide) {
        while (index + 1 < data.size &&
            !(data[index] == 0.toByte() && data[index + 1] == 0.toByte())
        ) {
            index += 2
        }
        index += 2
    } else {
        while (index < data.size && data[index] != 0.toByte()) index++
        index += 1
    }
    if (index >= data.size) return null

    return String(data, index, data.size - index, charsetFor(encoding)).trim { it <= ' ' }
}

private fun encodeText(version: Int, value: String): ByteArray {
    val out = ByteArrayOutputStream()
    if (version == 4) {
        out.write(3)
        out.write(value.toByteArray(Charsets.UTF_8))
    } else {
        out.write(1)
        out.write(value.toByteArray(Charsets.UTF_16))
    }
    return out.toByteArray()
}

private fun encodeUslt(version: Int, value: String): ByteArray {
    val out = ByteArrayOutputStream()
    if (version == 4) {
        out.write(3)
        out.write("und".toByteArray(Charsets.ISO_8859_1))
        out.write(0)
        out.write(value.toByteArray(Charsets.UTF_8))
    } else {
        out.write(1)
        out.write("und".toByteArray(Charsets.ISO_8859_1))
        out.write(byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0, 0))
        out.write(value.toByteArray(Charsets.UTF_16))
    }
    return out.toByteArray()
}

private fun buildFrame(id: String, version: Int, value: String): ByteArray {
    val body = if (id == USLT) encodeUslt(version, value) else encodeText(version, value)

    val out = ByteArrayOutputStream()
    out.write(id.toByteArray(Charsets.ISO_8859_1))
    out.write(if (version == 4) syncsafeBytes(body.size) else beIntBytes(body.size))
    out.write(0)
    out.write(0)
    out.write(body)
    return out.toByteArray()
}

internal object Id3Tags {

    fun read(file: File): Map<String, List<String>> =
        RandomAccessFile(file, "r").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            val tag = parseHeader(header) ?: return emptyMap()

            val bytes = ByteArray(tag.size)
            raf.readFully(bytes)

            val out = mutableMapOf<String, MutableList<String>>()
            frames(bytes, tag.version, framesStart(bytes, tag)).forEach { frame ->
                val body = bytes.copyOfRange(frame.start + 10, frame.start + 10 + frame.size)
                val key = if (frame.id == USLT) TagKey.LYRICS else ID3_KEYS[frame.id]
                val value = if (frame.id == USLT) decodeUslt(body) else decodeText(body)

                if (key != null && value != null) {
                    out.getOrPut(key) { mutableListOf() } += value
                }
            }
            out
        }

    fun write(file: File, tempDir: File, tags: Map<String, String>): Boolean =
        RandomAccessFile(file, "rw").use { raf ->
            val header = ByteArray(10)
            raf.readFully(header)
            val tag = parseHeader(header) ?: return false
            if (tag.version != 3 && tag.version != 4) return false
            if (tag.flags and UNSYNC_FLAG != 0 || tag.flags and EXTENDED_FLAG != 0) return false

            val bytes = ByteArray(tag.size)
            raf.readFully(bytes)
            val audioStart = 10L + tag.size

            val written = tags.mapNotNull { (key, value) ->
                id3FrameFor(key.uppercase(), tag.version)?.let { it to value }
            }
            val replaced = written.map { (id, _) -> id }.toSet()

            val kept = ByteArrayOutputStream()
            frames(bytes, tag.version, from = 0).forEach { frame ->
                if (frame.id !in replaced) kept.write(bytes, frame.start, 10 + frame.size)
            }
            written.forEach { (id, value) ->
                if (value.isNotEmpty()) kept.write(buildFrame(id, tag.version, value))
            }

            val newFrames = kept.toByteArray()

            if (newFrames.size <= tag.size) {
                raf.seek(10)
                raf.write(newFrames)
                raf.write(ByteArray(tag.size - newFrames.size))
            } else {
                val newTagSize = newFrames.size + GROW_PADDING
                val head = ByteArrayOutputStream()
                head.write(header, 0, 6)
                head.write(syncsafeBytes(newTagSize))
                head.write(newFrames)
                head.write(ByteArray(GROW_PADDING))
                raf.rewriteWithTail(
                    metaEnd = 10L + newTagSize,
                    newHead = head.toByteArray(),
                    oldAudioStart = audioStart,
                    tempDir = tempDir,
                )
            }
            true
        }
}
