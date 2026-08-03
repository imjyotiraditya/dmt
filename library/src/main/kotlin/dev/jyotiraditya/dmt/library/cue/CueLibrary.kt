package dev.jyotiraditya.dmt.library.cue

import dev.jyotiraditya.dmt.library.LibraryTrack
import java.io.File

/** The stride between the ids of the tracks of one file, which bounds a sheet to that many tracks. */
private const val VIRTUAL_ID_STRIDE = 1_000L

/** The fewest tracks a sheet must describe before a file is worth splitting. */
private const val MIN_SPLIT_TRACKS = 2

/**
 * Splits the files that a cue sheet describes into the tracks it lists.
 *
 * A rip of a disc is often one file next to a sheet naming what plays when. Such a file is turned
 * into a track per entry, each clipped to its part of the file, so that the library holds the songs
 * of the disc rather than the disc itself.
 */
object CueLibrary {

    /** Returns [tracks] with every file that a sheet beside it describes split into its tracks. */
    fun expand(tracks: List<LibraryTrack>): List<LibraryTrack> {
        val sheetsByDirectory = tracks
            .asSequence()
            .map { it.path.substringBeforeLast('/') }
            .filter { it.isNotEmpty() }
            .distinct()
            .associateWith { directory -> sheetsIn(File(directory)) }

        return tracks.flatMap { track ->
            splitOrSelf(track, sheetsByDirectory[track.path.substringBeforeLast('/')].orEmpty())
        }
    }

    /** Returns the tracks that [cueTracks] describe, each clipped to its part of [track]. */
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

    /**
     * Returns the tracks of [track], which is the track itself unless a sheet of [sheets] names its
     * file and lists more than one track in it.
     */
    private fun splitOrSelf(track: LibraryTrack, sheets: List<CueSheet>): List<LibraryTrack> {
        if (track.durationMs <= 0) return listOf(track)

        val fileName = track.path.substringAfterLast('/')
        val (sheet, cueFile) = sheets.firstNotNullOfOrNull { sheet ->
            sheet.files
                .find { it.name.equals(fileName, ignoreCase = true) }
                ?.let { sheet to it }
        } ?: return listOf(track)

        // A sheet may name a longer file than the one at hand, so tracks past its end are dropped.
        val cueTracks = cueFile.tracks.filter { it.startMs < track.durationMs }
        if (cueTracks.size < MIN_SPLIT_TRACKS) return listOf(track)

        return split(track, sheet, cueTracks)
    }

    /**
     * Returns the id of the track numbered [number] of the file whose id is [fileId].
     *
     * The id of the file is mixed rather than multiplied, so that a file id of any size still gives
     * every track of that file its own id, and the result is negative so that it cannot collide
     * with the id of a track that the platform indexes.
     */
    private fun virtualId(fileId: Long, number: Int): Long =
        -((fileId * VIRTUAL_ID_STRIDE + number) and Long.MAX_VALUE)

    /** Returns the sheets that [directory] holds, ignoring any that cannot be read. */
    private fun sheetsIn(directory: File): List<CueSheet> = runCatching {
        directory
            .listFiles { file -> file.isFile && file.extension.equals("cue", ignoreCase = true) }
            .orEmpty()
            .mapNotNull { file ->
                runCatching { CueParser.parse(CueParser.decode(file.readBytes())) }.getOrNull()
            }
    }.getOrDefault(emptyList())
}
