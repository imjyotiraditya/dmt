package dev.jyotiraditya.metadata

import java.io.File

private enum class Format { FLAC, ID3, MP4, OGG }

private fun formatOf(file: File): Format? {
    if (!file.canRead()) return null

    val magic = ByteArray(12)
    val read = file.inputStream().use { it.read(magic) }
    if (read < 12) return null

    return when {
        magic.startsWith(FLAC_MARKER) -> Format.FLAC
        magic.startsWith(ID3_MARKER) -> Format.ID3
        magic.startsWith(OGG_MARKER) -> Format.OGG
        String(magic, 4, 4, Charsets.ISO_8859_1) == "ftyp" -> Format.MP4
        else -> null
    }
}

object AudioTags {

    fun read(path: String): Map<String, List<String>> =
        runCatching {
            val file = File(path)
            when (formatOf(file)) {
                Format.FLAC -> FlacTags.read(file)
                Format.ID3 -> Id3Tags.read(file)
                Format.MP4 -> Mp4Tags.read(file)
                Format.OGG -> OggTags.read(file)
                null -> emptyMap()
            }
        }.getOrDefault(emptyMap())

    fun canWrite(path: String): Boolean =
        runCatching { formatOf(File(path)) != null }.getOrDefault(false)

    fun write(file: File, tempDir: File, tags: Map<String, String>): Boolean =
        runCatching {
            when (formatOf(file)) {
                Format.FLAC -> FlacTags.write(file, tempDir, tags)
                Format.ID3 -> Id3Tags.write(file, tempDir, tags)
                Format.MP4 -> Mp4Tags.write(file, tempDir, tags)
                Format.OGG -> OggTags.write(file, tempDir, tags)
                null -> false
            }
        }.getOrDefault(false)
}
