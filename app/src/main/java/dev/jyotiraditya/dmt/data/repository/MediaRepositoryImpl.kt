package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.library.LibraryTrack
import dev.jyotiraditya.dmt.library.MusicLibrary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class MediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: PreferencesRepository,
    private val musicLibrary: MusicLibrary,
) : MediaRepository {

    private val scanLock = Mutex()

    @Volatile
    private var cache: List<Track>? = null

    @Volatile
    private var refresh = false

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val mediaObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            cache = null
            changes.tryEmit(Unit)
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            mediaObserver,
        )
    }

    override fun changes(): Flow<Unit> = changes

    override fun invalidate() {
        cache = null
        refresh = true
    }

    override suspend fun scan(): List<Track> = scanLock.withLock {
        cache ?: load().also { cache = it }
    }

    private suspend fun load(): List<Track> {
        val blocked = settingsRepository.settings.first().blockedFolders
        val tracks = musicLibrary.tracks(blocked, refresh).map { it.toTrack() }
        refresh = false

        return tracks.sortedBy { it.title.lowercase() }
    }
}

private fun LibraryTrack.toTrack(): Track = Track(
    id = id,
    uri = uri,
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
    coverUri = coverUri,
    cue = cue,
    clipStartMs = clipStartMs,
    clipEndMs = clipEndMs,
    source = TrackSource.LOCAL,
)
