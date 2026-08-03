package dev.jyotiraditya.dmt.library

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import dev.jyotiraditya.dmt.library.cue.CueLibrary
import dev.jyotiraditya.dmt.library.db.LibraryDatabase
import dev.jyotiraditya.dmt.library.db.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The tracks that the device holds.
 *
 * <p>Reading them means asking the platform for the ones it indexes and parsing the ones it does
 * not, which is slow enough that the result is stored. A stored library is returned as it is while
 * the volume says that nothing has changed, so that a launch that follows an unchanged one reads no
 * files at all.
 */
@UnstableApi
class MusicLibrary(
    private val context: Context,
    private val storedGeneration: GenerationStore,
) {

    private val database: LibraryDatabase by lazy {
        Room.databaseBuilder(context, LibraryDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /** Remembers the volume that a stored library was read from. */
    interface GenerationStore {
        suspend fun get(): Long

        suspend fun set(generation: Long)
    }

    /**
     * Returns the tracks of the device, reading only what has changed since the last time.
     *
     * @param blocked The folders whose tracks to leave out.
     * @param refresh Whether to read everything again even if nothing says it changed.
     */
    suspend fun tracks(blocked: Set<String>, refresh: Boolean = false): List<LibraryTrack> =
        withContext(Dispatchers.IO) {
            val dao = database.tracks()
            val stored = dao.all().map { it.toTrack() }
            val generation = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL_PRIMARY)

            if (!refresh && stored.isNotEmpty() && generation == storedGeneration.get()) {
                return@withContext CueLibrary.expand(stored)
            }

            // A failed read must not empty a library that was read before.
            val scanned = runCatching {
                MediaStoreTracks.query(context, blocked) +
                    UnindexedTracks.query(context, blocked, stored.associateBy { it.path })
            }.getOrElse { return@withContext CueLibrary.expand(stored) }

            dao.replaceAll(scanned.map { it.toEntity() })
            storedGeneration.set(generation)

            CueLibrary.expand(scanned)
        }
}

private const val DATABASE_NAME = "library"

private fun LibraryTrack.toEntity(): TrackEntity = TrackEntity(
    path = path,
    id = id,
    uri = uri.toString(),
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    durationMs = durationMs,
    mime = mime,
    bitrate = bitrate,
    size = size,
    trackNumber = trackNumber,
    discNumber = discNumber,
    dateAdded = dateAdded,
    dateModified = dateModified,
    coverUri = coverUri?.toString(),
)

private fun TrackEntity.toTrack(): LibraryTrack = LibraryTrack(
    id = id,
    uri = uri.toUri(),
    title = title,
    artist = artist,
    albumArtist = albumArtist,
    album = album,
    path = path,
    durationMs = durationMs,
    mime = mime,
    bitrate = bitrate,
    size = size,
    trackNumber = trackNumber,
    discNumber = discNumber,
    dateAdded = dateAdded,
    dateModified = dateModified,
    coverUri = coverUri?.toUri(),
)
