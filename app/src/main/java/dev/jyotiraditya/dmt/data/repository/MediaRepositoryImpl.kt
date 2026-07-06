package dev.jyotiraditya.dmt.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : MediaRepository {

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.BITRATE,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.TRACK,
    )

    override fun scan(): List<Track> =
        buildList {
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
                )?.use { cursor ->
                    while (cursor.moveToNext()) add(cursor.toTrack())
                }
            }
        }
}

private fun Cursor.text(column: String, fallback: String): String =
    getString(getColumnIndexOrThrow(column)).orUnknown(fallback)

private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

private fun Cursor.toTrack(): Track {
    val id = long(MediaStore.Audio.Media._ID)
    return Track(
        id = id,
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
        title = text(MediaStore.Audio.Media.TITLE, "unknown title"),
        artist = text(MediaStore.Audio.Media.ARTIST, "unknown artist"),
        album = text(MediaStore.Audio.Media.ALBUM, "unknown album"),
        albumId = long(MediaStore.Audio.Media.ALBUM_ID),
        path = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)).orEmpty(),
        durationMs = long(MediaStore.Audio.Media.DURATION),
        mime = text(MediaStore.Audio.Media.MIME_TYPE, "audio/?"),
        bitrate = int(MediaStore.Audio.Media.BITRATE),
        size = long(MediaStore.Audio.Media.SIZE),
        trackNumber = int(MediaStore.Audio.Media.TRACK),
    )
}

fun String?.orUnknown(fallback: String): String =
    if (isNullOrBlank() || this == "<unknown>") fallback else this
