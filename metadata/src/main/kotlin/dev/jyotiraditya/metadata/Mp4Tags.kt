package dev.jyotiraditya.metadata

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

private const val GROW_PADDING = 4096

private class Mp4Box(val type: String, val start: Int, val headerSize: Int, val size: Int) {
    val contentStart get() = start + headerSize
    val end get() = start + size
}

private class TopBox(val type: String, val start: Long, val headerSize: Long, val size: Long) {
    val end get() = start + size
}

private fun topBoxes(raf: RandomAccessFile): List<TopBox> {
    val out = mutableListOf<TopBox>()
    val length = raf.length()
    var pos = 0L
    while (pos + 8 <= length) {
        raf.seek(pos)
        val size32 = raf.readInt().toLong() and 0xFFFFFFFFL
        val type = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.ISO_8859_1)

        var headerSize = 8L
        var size = size32
        if (size32 == 1L) {
            size = raf.readLong()
            headerSize = 16L
        } else if (size32 == 0L) {
            size = length - pos
        }
        if (size < headerSize || pos + size > length) break

        out += TopBox(type, pos, headerSize, size)
        pos += size
    }
    return out
}

private fun contentOffset(type: String) = if (type == "meta") 4 else 0

private fun boxesIn(bytes: ByteArray, from: Int, until: Int): List<Mp4Box> {
    val out = mutableListOf<Mp4Box>()
    var pos = from
    while (pos + 8 <= until) {
        val size32 = beInt(bytes, pos).toLong() and 0xFFFFFFFFL
        val type = String(bytes, pos + 4, 4, Charsets.ISO_8859_1)

        var headerSize = 8
        var size = size32
        if (size32 == 1L) {
            if (pos + 16 > until) break
            size = (beInt(bytes, pos + 8).toLong() shl 32) or
                (beInt(bytes, pos + 12).toLong() and 0xFFFFFFFFL)
            headerSize = 16
        } else if (size32 == 0L) {
            size = (until - pos).toLong()
        }
        if (size < headerSize || pos + size > until) break

        out += Mp4Box(type, pos, headerSize, size.toInt())
        pos += size.toInt()
    }
    return out
}

private fun box(type: String, payload: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(beIntBytes(8 + payload.size))
    out.write(type.toByteArray(Charsets.ISO_8859_1))
    out.write(payload)
    return out.toByteArray()
}

private fun dataBox(value: String): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(beIntBytes(1))
    out.write(ByteArray(4))
    out.write(value.toByteArray(Charsets.UTF_8))
    return box("data", out.toByteArray())
}

private fun metaHdlr(): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(ByteArray(8))
    out.write("mdir".toByteArray(Charsets.ISO_8859_1))
    out.write("appl".toByteArray(Charsets.ISO_8859_1))
    out.write(ByteArray(9))
    return box("hdlr", out.toByteArray())
}

private fun rebuild(
    bytes: ByteArray,
    boxType: String,
    path: List<String>,
    atoms: Map<String, String>,
): ByteArray {
    val from = contentOffset(boxType)
    if (path.isEmpty()) {
        val kept = ByteArrayOutputStream()
        kept.write(bytes, 0, from)
        boxesIn(bytes, from, bytes.size)
            .filterNot { it.type in atoms }
            .forEach { kept.write(bytes, it.start, it.size) }
        atoms.forEach { (atom, value) ->
            if (value.isNotEmpty()) kept.write(box(atom, dataBox(value)))
        }
        return kept.toByteArray()
    }

    val next = path.first()
    val children = boxesIn(bytes, from, bytes.size)
    val target = children.firstOrNull { it.type == next }

    val out = ByteArrayOutputStream()
    out.write(bytes, 0, from)
    children.forEach { child ->
        if (child === target) {
            val payload = rebuild(
                bytes.copyOfRange(child.contentStart, child.end),
                next,
                path.drop(1),
                atoms,
            )
            out.write(box(next, payload))
        } else {
            out.write(bytes, child.start, child.size)
        }
    }
    if (target == null) {
        var payload = rebuild(ByteArray(contentOffset(next)), next, path.drop(1), atoms)
        if (next == "meta") payload = metaHdlr().let { hdlr ->
            payload.copyOfRange(0, 4) + hdlr + payload.copyOfRange(4, payload.size)
        }
        out.write(box(next, payload))
    }
    return out.toByteArray()
}

private fun patchChunkOffsets(
    moov: ByteArray,
    from: Int,
    until: Int,
    oldMoovEnd: Long,
    delta: Long,
) {
    boxesIn(moov, from, until).forEach { child ->
        when (child.type) {
            "trak", "mdia", "minf", "stbl" ->
                patchChunkOffsets(moov, child.contentStart, child.end, oldMoovEnd, delta)

            "stco", "co64" -> {
                val wide = child.type == "co64"
                val count = beInt(moov, child.contentStart + 4)
                var entry = child.contentStart + 8
                repeat(count) {
                    val offset = if (wide) {
                        (beInt(moov, entry).toLong() shl 32) or
                            (beInt(moov, entry + 4).toLong() and 0xFFFFFFFFL)
                    } else {
                        beInt(moov, entry).toLong() and 0xFFFFFFFFL
                    }
                    if (offset >= oldMoovEnd) {
                        val patched = offset + delta
                        if (wide) {
                            beIntBytes((patched ushr 32).toInt()).copyInto(moov, entry)
                            beIntBytes(patched.toInt()).copyInto(moov, entry + 4)
                        } else {
                            beIntBytes(patched.toInt()).copyInto(moov, entry)
                        }
                    }
                    entry += if (wide) 8 else 4
                }
            }
        }
    }
}

internal object Mp4Tags {

    fun read(file: File): Map<String, List<String>> =
        RandomAccessFile(file, "r").use { raf ->
            val out = mutableMapOf<String, MutableList<String>>()

            fun dataOf(start: Long, end: Long): String? {
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

            fun freeformOf(start: Long, end: Long): Pair<String, String>? {
                var pos = start
                var name: String? = null
                var value: String? = null
                while (pos + 8 <= end) {
                    raf.seek(pos)
                    val size = raf.readInt().toLong() and 0xFFFFFFFFL
                    val type = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.ISO_8859_1)
                    if (size < 8 || pos + size > end) break

                    when (type) {
                        "name" -> (size - 12).toInt().takeIf { it > 0 }?.let { payload ->
                            raf.seek(pos + 12)
                            val bytes = ByteArray(payload)
                            raf.readFully(bytes)
                            name = String(bytes, Charsets.UTF_8)
                        }

                        "data" -> (size - 16).toInt().takeIf { it > 0 }?.let { payload ->
                            raf.seek(pos + 16)
                            val bytes = ByteArray(payload)
                            raf.readFully(bytes)
                            value = String(bytes, Charsets.UTF_8).trim()
                        }
                    }
                    pos += size
                }
                val resolvedName = name
                val resolvedValue = value
                return if (resolvedName != null && resolvedValue != null) {
                    resolvedName.uppercase() to resolvedValue
                } else {
                    null
                }
            }

            fun scan(start: Long, end: Long) {
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
                    if (boxSize < headerSize) return

                    val contentStart = pos + headerSize
                    val contentEnd = (pos + boxSize).coerceAtMost(end)

                    when {
                        type == "moov" || type == "udta" || type == "ilst" ->
                            scan(contentStart, contentEnd)

                        type == "meta" -> scan(contentStart + 4, contentEnd)

                        type == "----" -> freeformOf(contentStart, contentEnd)?.let { (key, value) ->
                            out.getOrPut(key) { mutableListOf() } += value
                        }

                        MP4_KEYS.containsKey(type) -> dataOf(contentStart, contentEnd)?.let {
                            out.getOrPut(MP4_KEYS.getValue(type)) { mutableListOf() } += it
                        }
                    }
                    pos = contentEnd
                }
            }

            scan(0L, raf.length())
            out
        }

    fun write(file: File, tempDir: File, tags: Map<String, String>): Boolean =
        RandomAccessFile(file, "rw").use { raf ->
            val atoms = tags.mapNotNull { (key, value) ->
                MP4_ATOMS[key.uppercase()]?.let { it to value }
            }.toMap()
            if (atoms.isEmpty()) return false

            val top = topBoxes(raf)
            val moov = top.firstOrNull { it.type == "moov" } ?: return false
            if (moov.size > Int.MAX_VALUE) return false

            val moovBytes = ByteArray(moov.size.toInt())
            raf.seek(moov.start)
            raf.readFully(moovBytes)

            val newPayload = rebuild(
                moovBytes.copyOfRange(moov.headerSize.toInt(), moovBytes.size),
                "moov",
                listOf("udta", "meta", "ilst"),
                atoms,
            )
            val newMoov = box("moov", newPayload)
            var delta = newMoov.size - moov.size

            val following = top.firstOrNull { it.start == moov.end }
            val head = ByteArrayOutputStream()
            head.write(newMoov)
            when {
                delta == 0L -> Unit

                following?.type == "free" && following.size - delta >= 8 -> {
                    head.write(box("free", ByteArray((following.size - delta).toInt() - 8)))
                    delta = 0
                }

                else -> {
                    head.write(box("free", ByteArray(GROW_PADDING)))
                    delta += 8 + GROW_PADDING
                }
            }

            val bytes = head.toByteArray()
            if (delta == 0L) {
                raf.seek(moov.start)
                raf.write(bytes)
            } else {
                patchChunkOffsets(bytes, 8, newMoov.size, oldMoovEnd = moov.end, delta = delta)
                raf.rewriteWithTail(
                    metaEnd = moov.start + bytes.size,
                    newHead = bytes,
                    oldAudioStart = moov.end,
                    tempDir = tempDir,
                )
            }
            true
        }
}
