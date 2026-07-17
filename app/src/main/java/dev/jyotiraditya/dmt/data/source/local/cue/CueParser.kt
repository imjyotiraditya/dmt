package dev.jyotiraditya.dmt.data.source.local.cue

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

data class CueTrack(
    val number: Int,
    val title: String?,
    val performer: String?,
    val startMs: Long,
)

data class CueFile(
    val name: String,
    val tracks: List<CueTrack>,
)

data class CueSheet(
    val title: String?,
    val performer: String?,
    val files: List<CueFile>,
)

object CueParser {

    private val INDEX_TIME = Regex("""^(\d+)\s+(\d+):(\d{1,2}):(\d{1,2})$""")
    private val TRACK_HEADER = Regex("""^(\d+)(?:\s+(\S+))?$""")
    private val FILE_TYPES = setOf("WAVE", "MP3", "AIFF", "BINARY", "MOTOROLA", "FLAC")

    fun parse(raw: String): CueSheet? {
        val files = mutableListOf<CueFile>()
        var sheetTitle: String? = null
        var sheetPerformer: String? = null
        var fileName: String? = null
        var fileTracks = mutableListOf<CueTrack>()
        var trackNumber: Int? = null
        var trackTitle: String? = null
        var trackPerformer: String? = null
        var index01Ms: Long? = null
        var index00Ms: Long? = null

        fun closeTrack() {
            val number = trackNumber
            val startMs = index01Ms ?: index00Ms
            if (number != null && startMs != null) {
                fileTracks += CueTrack(
                    number = number,
                    title = trackTitle,
                    performer = trackPerformer,
                    startMs = startMs,
                )
            }
            trackNumber = null
            trackTitle = null
            trackPerformer = null
            index01Ms = null
            index00Ms = null
        }

        fun closeFile() {
            closeTrack()
            val name = fileName
            if (name != null && fileTracks.isNotEmpty()) {
                files += CueFile(name = name, tracks = fileTracks.sortedBy { it.startMs })
            }
            fileName = null
            fileTracks = mutableListOf()
        }

        raw.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach
            val keyword = trimmed.substringBefore(' ').uppercase()
            val rest = trimmed.substringAfter(' ', "").trim()

            when (keyword) {
                "FILE" -> {
                    closeFile()
                    fileName = unquote(stripFileType(rest)).takeIf { it.isNotEmpty() }
                }

                "TRACK" -> {
                    closeTrack()
                    trackNumber = TRACK_HEADER.matchEntire(rest)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                }

                "TITLE" ->
                    if (trackNumber != null) {
                        trackTitle = unquote(rest).takeIf { it.isNotEmpty() }
                    } else if (fileName == null) {
                        sheetTitle = unquote(rest).takeIf { it.isNotEmpty() }
                    }

                "PERFORMER" ->
                    if (trackNumber != null) {
                        trackPerformer = unquote(rest).takeIf { it.isNotEmpty() }
                    } else if (fileName == null) {
                        sheetPerformer = unquote(rest).takeIf { it.isNotEmpty() }
                    }

                "INDEX" -> {
                    val match = INDEX_TIME.matchEntire(rest)
                    if (match != null && trackNumber != null) {
                        val ms = match.toMs()
                        when (match.groupValues[1].toIntOrNull()) {
                            0 -> index00Ms = ms
                            1 -> index01Ms = ms
                        }
                    }
                }
            }
        }
        closeFile()

        if (files.isEmpty()) return null

        return CueSheet(
            title = sheetTitle,
            performer = sheetPerformer,
            files = files,
        )
    }

    fun decode(bytes: ByteArray): String {
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        val body = if (
            bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            bytes.copyOfRange(3, bytes.size)
        } else {
            bytes
        }

        for (name in listOf("UTF-8", "Shift_JIS", "GBK")) {
            val decoded = runCatching {
                Charset.forName(name)
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(body))
                    .toString()
            }.getOrNull()
            if (decoded != null) return decoded
        }
        return String(body, Charsets.ISO_8859_1)
    }

    private fun MatchResult.toMs(): Long {
        val minutes = groupValues[2].toLong()
        val seconds = groupValues[3].toLong()
        val frames = groupValues[4].toLong()
        return minutes * 60_000 + seconds * 1_000 + frames * 1_000 / 75
    }

    private fun stripFileType(value: String): String {
        val lastToken = value.substringAfterLast(' ').uppercase()
        return if (lastToken in FILE_TYPES) {
            value.substringBeforeLast(' ').trim()
        } else {
            value
        }
    }

    private fun unquote(value: String): String =
        value.trim().removeSurrounding("\"").trim()
}
