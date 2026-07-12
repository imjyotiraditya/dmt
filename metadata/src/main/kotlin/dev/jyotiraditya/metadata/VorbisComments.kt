package dev.jyotiraditya.metadata

import java.io.ByteArrayOutputStream

internal class VorbisComment(val vendor: ByteArray, val entries: List<ByteArray>) {

    fun encode(): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(leIntBytes(vendor.size))
        out.write(vendor)
        out.write(leIntBytes(entries.size))
        entries.forEach { entry ->
            out.write(leIntBytes(entry.size))
            out.write(entry)
        }
        return out.toByteArray()
    }

    fun toTags(): Map<String, List<String>> =
        entries
            .filter { entryKey(it).isNotEmpty() }
            .groupBy(::entryKey) { entryValue(it) }

    fun replacing(tags: Map<String, String>): VorbisComment {
        val replaced = tags.keys.map { it.uppercase() }.toSet()
        val kept = entries.filterNot { entryKey(it) in replaced }
        val added = tags
            .filterValues { it.isNotEmpty() }
            .map { (key, value) -> "${key.uppercase()}=$value".toByteArray(Charsets.UTF_8) }
        return VorbisComment(vendor, kept + added)
    }
}

internal fun parseComment(block: ByteArray, offset: Int = 0): VorbisComment {
    val empty = VorbisComment(ByteArray(0), emptyList())
    if (offset + 8 > block.size) return empty

    val vendorLength = leInt(block, offset)
    var cursor = offset + 4 + vendorLength
    if (vendorLength < 0 || cursor + 4 > block.size) return empty

    val vendor = block.copyOfRange(offset + 4, cursor)
    val count = leInt(block, cursor)
    cursor += 4

    val entries = mutableListOf<ByteArray>()
    repeat(count) {
        if (cursor + 4 > block.size) return VorbisComment(vendor, entries)
        val length = leInt(block, cursor)
        cursor += 4
        if (length < 0 || cursor + length > block.size) return VorbisComment(vendor, entries)
        entries += block.copyOfRange(cursor, cursor + length)
        cursor += length
    }
    return VorbisComment(vendor, entries)
}

private fun entryKey(entry: ByteArray): String {
    val separator = entry.indexOf('='.code.toByte())
    if (separator <= 0) return ""
    return String(entry, 0, separator, Charsets.UTF_8).uppercase()
}

private fun entryValue(entry: ByteArray): String {
    val text = String(entry, Charsets.UTF_8)
    return text.substring(text.indexOf('=') + 1)
}
