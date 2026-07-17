package dev.jyotiraditya.dmt.data.source.local.cue

import dev.jyotiraditya.dmt.domain.model.Track
import java.io.File

private const val VIRTUAL_ID_STRIDE = 1_000L

object CueLibrary {

    fun expand(tracks: List<Track>): List<Track> {
        val sheetsByDir = tracks
            .asSequence()
            .map { it.path.substringBeforeLast('/') }
            .filter { it.isNotEmpty() }
            .distinct()
            .associateWith { dir -> sheetsIn(File(dir)) }

        return tracks.flatMap { track ->
            val sheets = sheetsByDir[track.path.substringBeforeLast('/')].orEmpty()
            splitOrSelf(track, sheets)
        }
    }

    fun split(track: Track, sheet: CueSheet, cueTracks: List<CueTrack>): List<Track> =
        cueTracks.mapIndexed { index, cue ->
            val endMs = cueTracks.getOrNull(index + 1)?.startMs ?: track.durationMs
            val durationMs = endMs - cue.startMs
            track.copy(
                id = -(track.id * VIRTUAL_ID_STRIDE + cue.number),
                title = cue.title ?: "${track.title} #${cue.number}",
                artist = cue.performer ?: sheet.performer ?: track.artist,
                album = sheet.title ?: track.album,
                durationMs = durationMs,
                size = track.size * durationMs / track.durationMs,
                trackNumber = cue.number,
                clipStartMs = cue.startMs.takeIf { it > 0 },
                clipEndMs = endMs.takeIf { it < track.durationMs },
            )
        }

    private fun splitOrSelf(track: Track, sheets: List<CueSheet>): List<Track> {
        if (track.durationMs <= 0) return listOf(track)
        val fileName = track.path.substringAfterLast('/')
        val (sheet, cueFile) = sheets.firstNotNullOfOrNull { sheet ->
            sheet.files
                .find { it.name.equals(fileName, ignoreCase = true) }
                ?.let { sheet to it }
        } ?: return listOf(track)

        val cueTracks = cueFile.tracks.filter { it.startMs < track.durationMs }
        if (cueTracks.size < 2) return listOf(track)

        return split(track, sheet, cueTracks)
    }

    private fun sheetsIn(dir: File): List<CueSheet> =
        runCatching {
            dir.listFiles { file -> file.isFile && file.extension.equals("cue", true) }
                .orEmpty()
                .mapNotNull { file ->
                    runCatching { CueParser.parse(CueParser.decode(file.readBytes())) }.getOrNull()
                }
        }.getOrDefault(emptyList())
}
