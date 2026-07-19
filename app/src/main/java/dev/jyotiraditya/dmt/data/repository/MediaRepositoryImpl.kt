package dev.jyotiraditya.dmt.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: PreferencesRepository,
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
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
    )

    override suspend fun scan(): List<Track> {
        val blocked = settingsRepository.settings.first().blockedFolders
        return buildList {
            runCatching {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
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
        }
    }
}

private fun Cursor.text(column: String, fallback: String): String =
    getString(getColumnIndexOrThrow(column)).orUnknown(fallback)

private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

private val albumArtBase: Uri = "content://media/external/audio/albumart".toUri()

private fun Cursor.toTrack(): Track {
    val id = long(MediaStore.Audio.Media._ID)
    val albumId = long(MediaStore.Audio.Media.ALBUM_ID)
    return Track(
        id = id,
        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
        title = text(MediaStore.Audio.Media.TITLE, "unknown title"),
        artist = text(MediaStore.Audio.Media.ARTIST, "unknown artist"),
        album = text(MediaStore.Audio.Media.ALBUM, "unknown album"),
        path = getString(getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)).orEmpty(),
        durationMs = long(MediaStore.Audio.Media.DURATION),
        mime = text(MediaStore.Audio.Media.MIME_TYPE, "audio/?"),
        bitrate = int(MediaStore.Audio.Media.BITRATE),
        size = long(MediaStore.Audio.Media.SIZE),
        trackNumber = int(MediaStore.Audio.Media.TRACK),
        dateAdded = long(MediaStore.Audio.Media.DATE_ADDED),
        dateModified = long(MediaStore.Audio.Media.DATE_MODIFIED),
        coverUri = ContentUris.withAppendedId(albumArtBase, albumId),
        source = TrackSource.LOCAL,
    )
}

fun String?.orUnknown(fallback: String): String =
    if (isNullOrBlank() || this == "<unknown>") fallback else this
