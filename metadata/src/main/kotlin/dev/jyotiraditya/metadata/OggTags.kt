package dev.jyotiraditya.metadata

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.Channels

internal const val OGG_MARKER = "OggS"
private const val BOS_FLAG = 0x02
private const val CONTINUED_FLAG = 0x01
private const val OPUS_TAGS_MAGIC = "OpusTags"
private const val VORBIS_MAGIC = "vorbis"

private val CRC_TABLE = IntArray(256) { index ->
    var register = index shl 24
    repeat(8) {
        register = if (register and 0x80000000.toInt() != 0) {
            (register shl 1) xor 0x04C11DB7
        } else {
            register shl 1
        }
    }
    register
}

private fun oggCrc(bytes: ByteArray): Int {
    var crc = 0
    bytes.forEach { byte ->
        crc = (crc shl 8) xor CRC_TABLE[((crc ushr 24) xor (byte.toInt() and 0xFF)) and 0xFF]
    }
    return crc
}

private class OggPage(
    val headerType: Int,
    val granule: ByteArray,
    val serial: Int,
    val segments: ByteArray,
    val payload: ByteArray,
)

private fun readPage(raf: RandomAccessFile): OggPage? {
    val fixed = ByteArray(27)
    if (raf.filePointer + 27 > raf.length()) return null
    raf.readFully(fixed)
    if (!fixed.startsWith(OGG_MARKER)) return null

    val segmentCount = fixed[26].toInt() and 0xFF
    val segments = ByteArray(segmentCount)
    raf.readFully(segments)

    val payload = ByteArray(segments.sumOf { it.toInt() and 0xFF })
    raf.readFully(payload)

    return OggPage(
        headerType = fixed[5].toInt() and 0xFF,
        granule = fixed.copyOfRange(6, 14),
        serial = leInt(fixed, 14),
        segments = segments,
        payload = payload,
    )
}

private fun ByteArrayOutputStream.writePage(page: OggPage, sequence: Int) {
    val header = ByteArrayOutputStream()
    header.write(OGG_MARKER.toByteArray(Charsets.ISO_8859_1))
    header.write(0)
    header.write(page.headerType)
    header.write(page.granule)
    header.write(leIntBytes(page.serial))
    header.write(leIntBytes(sequence))
    header.write(leIntBytes(0))
    header.write(page.segments.size)
    header.write(page.segments)

    val bytes = header.toByteArray() + page.payload
    val crc = leIntBytes(oggCrc(bytes))
    System.arraycopy(crc, 0, bytes, 22, 4)
    write(bytes, 0, bytes.size)
}

private fun packetPages(
    packet: ByteArray,
    headerType: Int,
    serial: Int,
): List<OggPage> {
    val lacing = mutableListOf<Int>()
    var remaining = packet.size
    while (remaining >= 255) {
        lacing += 255
        remaining -= 255
    }
    lacing += remaining

    val pages = mutableListOf<OggPage>()
    var consumed = 0
    var offset = 0
    while (offset < lacing.size) {
        val slice = lacing.subList(offset, minOf(offset + 255, lacing.size))
        val payloadSize = slice.sum()
        pages += OggPage(
            headerType = if (offset == 0) headerType else CONTINUED_FLAG,
            granule = ByteArray(8),
            serial = serial,
            segments = ByteArray(slice.size) { slice[it].toByte() },
            payload = packet.copyOfRange(consumed, consumed + payloadSize),
        )
        consumed += payloadSize
        offset += 255
    }
    return pages
}

private class OggHeaders(
    val packets: List<ByteArray>,
    val serial: Int,
    val audioStart: Long,
    val audioSequence: Int,
)

private fun readHeaders(raf: RandomAccessFile): OggHeaders? {
    raf.seek(0)
    val first = readPage(raf) ?: return null
    if (first.headerType and BOS_FLAG == 0) return null

    val ident = first.payload
    val packetCount = when {
        ident.startsWith("OpusHead") -> 2
        ident.size > 6 && ident[0].toInt() == 1 &&
            String(ident, 1, 6, Charsets.ISO_8859_1) == VORBIS_MAGIC -> 3

        else -> return null
    }

    val packets = mutableListOf(ident)
    val pending = ByteArrayOutputStream()
    var pagesRead = 1

    while (packets.size < packetCount) {
        val page = readPage(raf) ?: return null
        pagesRead++
        if (page.serial != first.serial) return null

        var offset = 0
        page.segments.forEach { lacing ->
            val size = lacing.toInt() and 0xFF
            pending.write(page.payload, offset, size)
            offset += size
            if (size < 255) {
                packets += pending.toByteArray()
                pending.reset()
            }
        }
    }
    if (pending.size() > 0) return null

    return OggHeaders(
        packets = packets,
        serial = first.serial,
        audioStart = raf.filePointer,
        audioSequence = pagesRead,
    )
}

private fun commentOf(packet: ByteArray): VorbisComment? = when {
    packet.startsWith(OPUS_TAGS_MAGIC) -> parseComment(packet, offset = 8)
    packet.size > 7 && packet[0].toInt() == 3 -> parseComment(packet, offset = 7)
    else -> null
}

private fun encodeCommentPacket(original: ByteArray, comment: VorbisComment): ByteArray {
    val out = ByteArrayOutputStream()
    if (original.startsWith(OPUS_TAGS_MAGIC)) {
        out.write(OPUS_TAGS_MAGIC.toByteArray(Charsets.ISO_8859_1))
        out.write(comment.encode())
    } else {
        out.write(3)
        out.write(VORBIS_MAGIC.toByteArray(Charsets.ISO_8859_1))
        out.write(comment.encode())
        out.write(1)
    }
    return out.toByteArray()
}

internal object OggTags {

    fun read(file: File): Map<String, List<String>> =
        RandomAccessFile(file, "r").use { raf ->
            val headers = readHeaders(raf) ?: return emptyMap()
            commentOf(headers.packets[1])?.toTags() ?: emptyMap()
        }

    fun write(file: File, tempDir: File, tags: Map<String, String>): Boolean =
        RandomAccessFile(file, "rw").use { raf ->
            val headers = readHeaders(raf) ?: return false
            val comment = commentOf(headers.packets[1]) ?: return false

            val newPacket = encodeCommentPacket(headers.packets[1], comment.replacing(tags))
            val packets = headers.packets.toMutableList().apply { this[1] = newPacket }

            val head = ByteArrayOutputStream()
            head.writePage(
                OggPage(
                    headerType = BOS_FLAG,
                    granule = ByteArray(8),
                    serial = headers.serial,
                    segments = byteArrayOf(packets[0].size.toByte()),
                    payload = packets[0],
                ),
                sequence = 0,
            )

            var sequence = 1
            packets.drop(1).forEach { packet ->
                packetPages(packet, headerType = 0, serial = headers.serial).forEach { page ->
                    head.writePage(page, sequence++)
                }
            }

            val body = File.createTempFile("dmt-ogg", ".tmp", tempDir)
            try {
                raf.seek(headers.audioStart)
                body.outputStream().buffered().use { out ->
                    val rewritten = ByteArrayOutputStream()
                    while (true) {
                        val page = readPage(raf) ?: break
                        rewritten.reset()
                        rewritten.writePage(page, sequence++)
                        rewritten.writeTo(out)
                    }
                }

                raf.seek(0)
                raf.write(head.toByteArray())
                body.inputStream().use { input ->
                    input.copyTo(Channels.newOutputStream(raf.channel))
                }
                raf.setLength(head.size().toLong() + body.length())
            } finally {
                body.delete()
            }
            true
        }
}
