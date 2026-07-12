package dev.jyotiraditya.metadata

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

internal const val FLAC_MARKER = "fLaC"
private const val VORBIS_BLOCK = 4
private const val PADDING_BLOCK = 1
private const val LAST_FLAG = 0x80
private const val GROW_PADDING = 4096

private class FlacBlock(val type: Int, val size: Int, val data: ByteArray?)

private class FlacMeta(val blocks: List<FlacBlock>, val audioStart: Long)

private fun readMeta(raf: RandomAccessFile, keep: (Int) -> Boolean): FlacMeta? {
    val magic = ByteArray(4)
    raf.seek(0)
    raf.readFully(magic)
    if (!magic.startsWith(FLAC_MARKER)) return null

    val blocks = mutableListOf<FlacBlock>()
    while (true) {
        val header = ByteArray(4)
        raf.readFully(header)
        val last = header[0].toInt() and LAST_FLAG != 0
        val type = header[0].toInt() and 0x7F
        val size = ((header[1].toInt() and 0xFF) shl 16) or
                ((header[2].toInt() and 0xFF) shl 8) or
                (header[3].toInt() and 0xFF)

        val data = if (keep(type)) {
            ByteArray(size).also { raf.readFully(it) }
        } else {
            raf.seek(raf.filePointer + size)
            null
        }
        blocks += FlacBlock(type, size, data)
        if (last) break
    }
    return FlacMeta(blocks, raf.filePointer)
}

private fun ByteArrayOutputStream.writeBlockHeader(type: Int, size: Int, last: Boolean) {
    write(type or if (last) LAST_FLAG else 0)
    write((size ushr 16) and 0xFF)
    write((size ushr 8) and 0xFF)
    write(size and 0xFF)
}

internal object FlacTags {

    fun read(file: File): Map<String, List<String>> =
        RandomAccessFile(file, "r").use { raf ->
            val meta = readMeta(raf) { it == VORBIS_BLOCK } ?: return emptyMap()
            val block = meta.blocks.firstOrNull { it.type == VORBIS_BLOCK }?.data
                ?: return emptyMap()

            parseComment(block).toTags()
        }

    fun write(file: File, tempDir: File, tags: Map<String, String>): Boolean =
        RandomAccessFile(file, "rw").use { raf ->
            val meta = readMeta(raf) { true } ?: return false
            val oldMetaSize = meta.audioStart - 4

            val comment = parseComment(
                meta.blocks.firstOrNull { it.type == VORBIS_BLOCK }?.data ?: ByteArray(0),
            )
            val vorbis = comment.replacing(tags).encode()

            val kept = meta.blocks
                .filter { it.type != VORBIS_BLOCK && it.type != PADDING_BLOCK } +
                    FlacBlock(VORBIS_BLOCK, vorbis.size, vorbis)
            val coreSize = kept.sumOf { 4L + it.size }

            val paddingSize = when {
                coreSize == oldMetaSize -> -1
                coreSize + 4 <= oldMetaSize -> (oldMetaSize - coreSize - 4).toInt()
                else -> GROW_PADDING
            }

            val head = ByteArrayOutputStream()
            kept.forEachIndexed { index, block ->
                val last = paddingSize < 0 && index == kept.lastIndex
                head.writeBlockHeader(block.type, block.size, last)
                head.write(block.data ?: ByteArray(block.size))
            }
            if (paddingSize >= 0) {
                head.writeBlockHeader(PADDING_BLOCK, paddingSize, last = true)
                head.write(ByteArray(paddingSize))
            }

            val bytes = head.toByteArray()

            if (4L + bytes.size <= meta.audioStart) {
                raf.seek(4)
                raf.write(bytes)
            } else {
                raf.rewriteWithTail(
                    metaEnd = 4L + bytes.size,
                    newHead = bytes,
                    oldAudioStart = meta.audioStart,
                    tempDir = tempDir,
                )
            }
            true
        }
}
