package dev.jyotiraditya.dmt.library

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri

private const val TRACKS_PER_DISC = 1000

private val ALBUM_ART_BASE: Uri = "content://media/external/audio/albumart".toUri()

private val COLUMNS = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.TITLE,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM_ARTIST,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.ALBUM_ID,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.MIME_TYPE,
    MediaStore.Audio.Media.BITRATE,
    MediaStore.Audio.Media.SIZE,
    MediaStore.Audio.Media.TRACK,
    MediaStore.Audio.Media.DATE_ADDED,
    MediaStore.Audio.Media.DATE_MODIFIED,
)

private val TAG_COLUMNS = arrayOf(
    MediaStore.Audio.Media.CD_TRACK_NUMBER,
    MediaStore.Audio.Media.DISC_NUMBER,
)

/** Reads the tracks that the platform indexes, which is every format it can decode itself. */
object MediaStoreTracks {

    /** Returns the indexed tracks, other than those in a folder of [blocked]. */
    fun query(context: Context, blocked: Set<String>): List<LibraryTrack> = buildList {
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val track = cursor.toTrack()
                if (track.path.substringBeforeLast('/') !in blocked) add(track)
            }
        }
    }

    private fun columns(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) COLUMNS + TAG_COLUMNS else COLUMNS

    private fun Cursor.toTrack(): LibraryTrack {
        val id = long(MediaStore.Audio.Media._ID)
        val albumId = long(MediaStore.Audio.Media.ALBUM_ID)

        return LibraryTrack(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
            title = text(MediaStore.Audio.Media.TITLE, UNKNOWN_TITLE),
            artist = text(MediaStore.Audio.Media.ARTIST, UNKNOWN_ARTIST),
            albumArtist = text(MediaStore.Audio.Media.ALBUM_ARTIST, ""),
            album = text(MediaStore.Audio.Media.ALBUM, UNKNOWN_ALBUM),
            path = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)).orEmpty(),
            durationMs = long(MediaStore.Audio.Media.DURATION),
            mime = text(MediaStore.Audio.Media.MIME_TYPE, "audio/?"),
            bitrate = int(MediaStore.Audio.Media.BITRATE),
            size = long(MediaStore.Audio.Media.SIZE),
            trackNumber = tagNumber(MediaStore.Audio.Media.CD_TRACK_NUMBER)
                ?: (int(MediaStore.Audio.Media.TRACK) % TRACKS_PER_DISC),
            discNumber = tagNumber(MediaStore.Audio.Media.DISC_NUMBER)
                ?: (int(MediaStore.Audio.Media.TRACK) / TRACKS_PER_DISC),
            dateAdded = long(MediaStore.Audio.Media.DATE_ADDED),
            dateModified = long(MediaStore.Audio.Media.DATE_MODIFIED),
            coverUri = ContentUris.withAppendedId(ALBUM_ART_BASE, albumId),
        )
    }

    /**
     * Returns the number that a tag holds, which may count the tracks of the release as well, as in
     * "9/12", or null on the versions that do not store the tags.
     */
    private fun Cursor.tagNumber(column: String): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val index = getColumnIndex(column).takeIf { it >= 0 } ?: return null

        return getString(index)?.substringBefore('/')?.trim()?.toIntOrNull()
    }

    private fun Cursor.text(column: String, fallback: String): String =
        getString(getColumnIndexOrThrow(column)).orUnknown(fallback)

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

    private fun String?.orUnknown(fallback: String): String =
        if (isNullOrBlank() || this == "<unknown>") fallback else this
}
