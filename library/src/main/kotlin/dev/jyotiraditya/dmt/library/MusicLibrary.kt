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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * The tracks on the device, stored between launches and read again only when the media store
 * reports a change.
 */
@UnstableApi
class MusicLibrary(private val context: Context, private val storedGeneration: GenerationStore) {

    private val database: LibraryDatabase by lazy {
        Room.databaseBuilder(context, LibraryDatabase::class.java, DATABASE_NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    /** Keeps the media store generation the stored library was read at. */
    interface GenerationStore {
        suspend fun get(): Long

        suspend fun set(generation: Long)
    }

    /** The library as last stored, without tracks in [hidden] folders; empty before a sync. */
    suspend fun stored(hidden: Set<String>): List<LibraryTrack> = withContext(Dispatchers.IO) {
        database.tracks().all().map { it.toTrack() }.visible(hidden)
    }

    /** Brings the stored library in line with the device and returns it; [refresh] reads it all. */
    suspend fun sync(hidden: Set<String>, refresh: Boolean = false): List<LibraryTrack> =
        withContext(Dispatchers.IO) {
            val dao = database.tracks()
            val stored = dao.all().map { it.toTrack() }
            val generation = MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL_PRIMARY)
            if (!refresh && stored.isNotEmpty() && generation == storedGeneration.get()) {
                return@withContext stored.visible(hidden)
            }

            // A failed read must not empty a library that was read before.
            val scanned = suspendRunCatching {
                readDevice(known = stored)
            }
                .getOrElse {
                    return@withContext stored.visible(hidden)
                }

            val storedByPath = stored.associateBy { it.path }
            val scannedPaths = scanned.mapTo(HashSet()) { it.path }
            dao.apply(
                changed = scanned.filter { storedByPath[it.path] != it }.map { it.toEntity() },
                removed = stored.map { it.path }.filter { it !in scannedPaths },
            )
            storedGeneration.set(generation)
            scanned.visible(hidden)
        }

    private suspend fun readDevice(known: List<LibraryTrack>): List<LibraryTrack> = coroutineScope {
        val indexed = async { MediaStoreTracks.query(context) }
        val unindexed = async { UnindexedTracks.query(context, known.associateBy { it.path }) }
        indexed.await() + unindexed.await()
    }

    /** These tracks without those in [hidden] folders, with cue sheets split into their tracks. */
    private fun List<LibraryTrack>.visible(hidden: Set<String>): List<LibraryTrack> =
        CueLibrary.expand(
            if (hidden.isEmpty()) {
                this
            } else {
                filter {
                    it.path.substringBeforeLast('/') !in hidden
                }
            },
        )
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
    genre = genre,
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
    genre = genre,
)
