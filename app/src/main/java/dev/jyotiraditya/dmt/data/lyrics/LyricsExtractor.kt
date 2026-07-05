package dev.jyotiraditya.dmt.data.lyrics

import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile

private fun InputStream.readFully(target: ByteArray): Boolean {
    var read = 0
    while (read < target.size) {
        val n = read(target, read, target.size - read)
        if (n < 0) return false
        read += n
    }
    return true
}

private fun InputStream.skipFully(count: Long) {
    var remaining = count
    while (remaining > 0) {
        val skipped = skip(remaining)
        if (skipped > 0) {
            remaining -= skipped
        } else {
            if (read() < 0) return
            remaining--
        }
    }
}

private fun syncsafe(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0x7F) shl 21) or
        ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
        ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
        (bytes[offset + 3].toInt() and 0x7F)

private fun beInt(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
        (bytes[offset + 3].toInt() and 0xFF)

private fun leInt(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xFF) or
        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
        ((bytes[offset + 3].toInt() and 0xFF) shl 24)

private fun decodeUslt(data: ByteArray): String? {
    if (data.size < 5) return null
    val encoding = data[0].toInt()
    val charset = when (encoding) {
        0 -> Charsets.ISO_8859_1
        1 -> Charsets.UTF_16
        2 -> Charsets.UTF_16BE
        else -> Charsets.UTF_8
    }
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
    return String(data, index, data.size - index, charset).trim { it <= ' ' }
}

private fun readId3(file: File): String? = file.inputStream().buffered().use { input ->
    val header = ByteArray(10)
    if (!input.readFully(header)) return null
    if (header[0] != 'I'.code.toByte() ||
        header[1] != 'D'.code.toByte() ||
        header[2] != '3'.code.toByte()
    ) {
        return null
    }
    val version = header[3].toInt()
    val flags = header[5].toInt()
    var remaining = syncsafe(header, 6)
    if (flags and 0x40 != 0) {
        val extended = ByteArray(4)
        if (!input.readFully(extended)) return null
        val extendedSize = if (version == 4) syncsafe(extended, 0) else beInt(extended, 0)
        input.skipFully((extendedSize - 4).coerceAtLeast(0).toLong())
        remaining -= extendedSize
    }
    val frameHeader = ByteArray(10)
    while (remaining > 10) {
        if (!input.readFully(frameHeader)) return null
        remaining -= 10
        if (frameHeader[0] == 0.toByte()) return null
        val id = String(frameHeader, 0, 4, Charsets.ISO_8859_1)
        val size = if (version == 4) syncsafe(frameHeader, 4) else beInt(frameHeader, 4)
        if (size <= 0 || size > remaining) return null
        if (id == "USLT") {
            val data = ByteArray(size)
            if (!input.readFully(data)) return null
            return decodeUslt(data)
        }
        input.skipFully(size.toLong())
        remaining -= size
    }
    null
}

private val WORD_TIMED_TAG = Regex("""<\d+:\d{1,2}""")
private val LINE_TIMED_TAG = Regex("""\[\d+:\d{1,2}""")
private val ENRICHED_TAG = Regex("""\[bg:|]\s*v\d+:""", RegexOption.IGNORE_CASE)

private fun isLyricsKey(key: String): Boolean =
    key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "UNSYNCED LYRICS" ||
        key == "ELRC" || key == "LRC" || key.startsWith("LYRICS-")

private fun contentRank(text: String): Int = when {
    text.startsWith("<") && text.contains("<tt") -> 4
    WORD_TIMED_TAG.containsMatchIn(text) && ENRICHED_TAG.containsMatchIn(text) -> 3
    WORD_TIMED_TAG.containsMatchIn(text) -> 2
    LINE_TIMED_TAG.containsMatchIn(text) -> 1
    else -> 0
}

private fun parseVorbisComment(block: ByteArray): String? {
    var offset = 0
    if (block.size < 8) return null
    val vendorLength = leInt(block, offset)
    offset += 4 + vendorLength
    if (offset + 4 > block.size) return null
    val count = leInt(block, offset)
    offset += 4
    var best: String? = null
    var bestRank = -1
    repeat(count) {
        if (offset + 4 > block.size) return null
        val length = leInt(block, offset)
        offset += 4
        if (offset + length > block.size) return null
        val entry = String(block, offset, length, Charsets.UTF_8)
        offset += length
        val separator = entry.indexOf('=')
        if (separator > 0) {
            val key = entry.substring(0, separator).uppercase()
            val value = entry.substring(separator + 1).trim()
            if (isLyricsKey(key) && value.isNotEmpty()) {
                val rank = contentRank(value)
                if (rank > bestRank) {
                    best = value
                    bestRank = rank
                }
            }
        }
    }
    return best
}

private fun readFlac(file: File): String? = file.inputStream().buffered().use { input ->
    val magic = ByteArray(4)
    if (!input.readFully(magic) || String(magic, Charsets.ISO_8859_1) != "fLaC") return null
    val blockHeader = ByteArray(4)
    while (true) {
        if (!input.readFully(blockHeader)) return null
        val last = blockHeader[0].toInt() and 0x80 != 0
        val type = blockHeader[0].toInt() and 0x7F
        val size = ((blockHeader[1].toInt() and 0xFF) shl 16) or
            ((blockHeader[2].toInt() and 0xFF) shl 8) or
            (blockHeader[3].toInt() and 0xFF)
        if (type == 4) {
            val block = ByteArray(size)
            if (!input.readFully(block)) return null
            return parseVorbisComment(block)
        }
        input.skipFully(size.toLong())
        if (last) return null
    }
    @Suppress("UNREACHABLE_CODE")
    null
}

private fun readMp4(file: File): String? = RandomAccessFile(file, "r").use { raf ->
    fun findData(start: Long, end: Long): String? {
        var pos = start
        while (pos + 8 <= end) {
            raf.seek(pos)
            val size = raf.readInt().toLong() and 0xFFFFFFFFL
            val type = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.ISO_8859_1)
            if (size < 8) return null
            if (type == "data") {
                val payload = (size - 16).toInt()
                if (payload <= 0) return null
                raf.seek(pos + 16)
                val bytes = ByteArray(payload)
                raf.readFully(bytes)
                return String(bytes, Charsets.UTF_8).trim()
            }
            pos += size
        }
        return null
    }

    fun scan(start: Long, end: Long): String? {
        var pos = start
        while (pos + 8 <= end) {
            raf.seek(pos)
            val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
            val type = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.ISO_8859_1)
            var headerSize = 8L
            var boxSize = size32
            if (size32 == 1L) {
                boxSize = raf.readLong()
                headerSize = 16L
            } else if (size32 == 0L) {
                boxSize = end - pos
            }
            if (boxSize < headerSize) return null
            val contentStart = pos + headerSize
            val contentEnd = (pos + boxSize).coerceAtMost(end)
            when (type) {
                "moov", "udta", "ilst" -> scan(contentStart, contentEnd)?.let { return it }
                "meta" -> scan(contentStart + 4, contentEnd)?.let { return it }
                "©lyr" -> findData(contentStart, contentEnd)?.let { return it }
            }
            pos = contentEnd
        }
        return null
    }

    scan(0L, raf.length())
}

object LyricsExtractor {

    fun extract(path: String, mime: String): String? = runCatching {
        if (path.isEmpty()) return null
        val file = File(path)
        if (!file.canRead()) return null
        val magic = ByteArray(12)
        file.inputStream().use { it.readFully(magic) }
        when {
            magic[0] == 'I'.code.toByte() &&
                magic[1] == 'D'.code.toByte() &&
                magic[2] == '3'.code.toByte() -> readId3(file)

            magic[0] == 'f'.code.toByte() &&
                magic[1] == 'L'.code.toByte() &&
                magic[2] == 'a'.code.toByte() &&
                magic[3] == 'C'.code.toByte() -> readFlac(file)

            String(magic, 4, 4, Charsets.ISO_8859_1) == "ftyp" -> readMp4(file)

            mime.contains("mpeg", true) -> readId3(file)

            else -> null
        }
    }.getOrNull()
}
