package dev.jyotiraditya.dmt.library.cue

import dev.jyotiraditya.dmt.library.LibraryTrack
import java.io.File
import kotlin.time.Duration.Companion.minutes

/** The gap between track ids within one file, and so the most tracks a sheet can list. */
private const val VIRTUAL_ID_STRIDE = 1_000L

private const val MIN_SPLIT_TRACKS = 2

/** The shortest file looked up in cue sheets; shorter files are not disc rips. */
private val MIN_SPLIT_DURATION_MS = 10.minutes.inWholeMilliseconds

/** Splits audio files that cue sheets describe into the tracks they list. */
object CueLibrary {

    /** [tracks] with every file a cue sheet beside it describes split into its tracks. */
    fun expand(tracks: List<LibraryTrack>): List<LibraryTrack> {
        val sheetsByDirectory =
            tracks
                .asSequence()
                .filter { it.durationMs >= MIN_SPLIT_DURATION_MS }
                .map { it.path.substringBeforeLast('/') }
                .filter { it.isNotEmpty() }
                .distinct()
                .associateWith { directory -> sheetsIn(File(directory)) }

        if (sheetsByDirectory.isEmpty()) return tracks

        return tracks.flatMap { track ->
            splitOrSelf(track, sheetsByDirectory[track.path.substringBeforeLast('/')].orEmpty())
        }
    }

    fun split(
        track: LibraryTrack,
        sheet: CueSheet,
        cueTracks: List<CueTrack>,
    ): List<LibraryTrack> = cueTracks.mapIndexed { index, cue ->
        val endMs = cueTracks.getOrNull(index + 1)?.startMs ?: track.durationMs
        val durationMs = endMs - cue.startMs

        track.copy(
            id = virtualId(track.id, cue.number),
            cue = true,
            title = cue.title ?: "${track.title} #${cue.number}",
            artist = cue.performer ?: sheet.performer ?: track.artist,
            albumArtist = sheet.performer ?: track.albumArtist,
            album = sheet.title ?: track.album,
            durationMs = durationMs,
            size = track.size * durationMs / track.durationMs,
            trackNumber = cue.number,
            clipStartMs = cue.startMs.takeIf { it > 0 },
            clipEndMs = endMs.takeIf { it < track.durationMs },
        )
    }

    private fun splitOrSelf(track: LibraryTrack, sheets: List<CueSheet>): List<LibraryTrack> {
        if (track.durationMs <= 0) return listOf(track)

        val fileName = track.path.substringAfterLast('/')
        val (sheet, cueFile) =
            sheets.firstNotNullOfOrNull { sheet ->
                sheet.files
                    .find { it.name.equals(fileName, ignoreCase = true) }
                    ?.let { sheet to it }
            } ?: return listOf(track)

        // A sheet may describe a longer file; tracks past the end of this one are dropped.
        val cueTracks = cueFile.tracks.filter { it.startMs < track.durationMs }
        if (cueTracks.size < MIN_SPLIT_TRACKS) return listOf(track)

        return split(track, sheet, cueTracks)
    }

    /** The id of track [number] in file [fileId]: distinct per track and always negative. */
    private fun virtualId(fileId: Long, number: Int): Long =
        -((fileId * VIRTUAL_ID_STRIDE + number) and Long.MAX_VALUE)

    private fun sheetsIn(directory: File): List<CueSheet> = runCatching {
        directory
            .listFiles { file -> file.isFile && file.extension.equals("cue", ignoreCase = true) }
            .orEmpty()
            .mapNotNull { file ->
                runCatching { CueParser.parse(CueParser.decode(file.readBytes())) }.getOrNull()
            }
    }
        .getOrDefault(emptyList())
}
