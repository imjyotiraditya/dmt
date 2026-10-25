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

private val COLUMNS =
    arrayOf(
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
        MediaStore.Audio.Media.GENRE,
    )

private val TAG_COLUMNS =
    arrayOf(
        MediaStore.Audio.Media.CD_TRACK_NUMBER,
        MediaStore.Audio.Media.DISC_NUMBER,
    )

object MediaStoreTracks {

    fun query(context: Context): List<LibraryTrack> = context.contentResolver
        .query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            columns(),
            // A null music flag marks files the scanner could not read, which may still play.
            "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
                " OR ${MediaStore.Audio.Media.IS_MUSIC} IS NULL",
            null,
            null,
        )
        ?.use { cursor ->
            val row = Row(cursor)
            ArrayList<LibraryTrack>(cursor.count).apply {
                while (cursor.moveToNext()) add(row.read())
            }
        }
        .orEmpty()

    private fun columns(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) COLUMNS + TAG_COLUMNS else COLUMNS

    private class Row(private val cursor: Cursor) {
        private val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        private val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        private val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        private val albumArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
        private val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        private val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        private val data = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        private val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        private val mime = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        private val bitrate = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
        private val size = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        private val track = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        private val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        private val dateModified =
            cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        private val genre = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)

        // Absent before Android 12.
        private val cdTrack = cursor.getColumnIndex(MediaStore.Audio.Media.CD_TRACK_NUMBER)
        private val disc = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)

        fun read(): LibraryTrack {
            val trackId = cursor.getLong(id)
            val packed = cursor.getInt(track)
            return LibraryTrack(
                id = trackId,
                uri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        trackId,
                    ),
                title = text(title, UNKNOWN_TITLE),
                artist = text(artist, UNKNOWN_ARTIST),
                albumArtist = text(albumArtist, ""),
                album = text(album, UNKNOWN_ALBUM),
                path = cursor.getString(data).orEmpty(),
                durationMs = cursor.getLong(duration),
                mime = text(mime, "audio/?"),
                bitrate = cursor.getInt(bitrate),
                size = cursor.getLong(size),
                trackNumber = tagNumber(cdTrack) ?: (packed % TRACKS_PER_DISC),
                discNumber = tagNumber(disc) ?: (packed / TRACKS_PER_DISC),
                dateAdded = cursor.getLong(dateAdded),
                dateModified = cursor.getLong(dateModified),
                coverUri = ContentUris.withAppendedId(ALBUM_ART_BASE, cursor.getLong(albumId)),
                genre = cursor.getString(genre)?.takeIf { it.isNotBlank() },
            )
        }

        private fun text(index: Int, fallback: String): String {
            val value = cursor.getString(index)
            return if (value.isNullOrBlank() || value == "<unknown>") fallback else value
        }

        /** A track or disc number, which may carry the total, as in "9/12". */
        private fun tagNumber(index: Int): Int? = if (index < 0) {
            null
        } else {
            cursor.getString(index)?.substringBefore('/')?.trim()?.toIntOrNull()
        }
    }
}
