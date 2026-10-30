package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.database.ContentObserver
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceDisplay
import dev.jyotiraditya.dmt.library.LibraryTrack
import dev.jyotiraditya.dmt.library.MusicLibrary
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** The music stored on this device, as the media store and the library database hold it. */
@OptIn(UnstableApi::class)
@Singleton
class LocalMusicSource
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: PreferencesRepository,
    private val musicLibrary: MusicLibrary,
) : MusicSource {

    private val scanLock = Mutex()

    @Volatile private var cache: List<Track>? = null

    @Volatile private var refresh = false

    override val display =
        SourceDisplay(
            title = R.string.source_kind_local,
            line = R.string.setup_source_local_detail,
            setupDetail = R.string.setup_source_local_detail,
            icon = DmtIcons.Device,
            noTracks = R.string.library_empty_local,
            scanFailed = R.string.library_scan_failed_local,
        )

    override suspend fun signIn(
        address: String,
        username: String,
        password: String,
    ): Map<String, String> = throw UnsupportedOperationException("This device has no sign-in")

    /** Emits when audio files change, watching only while collected. */
    override fun changes(): Flow<Unit> = callbackFlow {
        val observer =
            object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    cache = null
                    trySend(Unit)
                }
            }
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
        .conflate()

    override fun invalidate() {
        cache = null
        refresh = true
    }

    override suspend fun stored(server: ServerSource?): List<Track> =
        cache ?: musicLibrary.stored(hidden = hiddenFolders()).toTracks()

    override suspend fun scan(server: ServerSource?): List<Track> = scanLock.withLock {
        cache
            ?: musicLibrary.sync(hidden = hiddenFolders(), refresh = refresh).toTracks().also {
                cache = it
                refresh = false
            }
    }

    private suspend fun hiddenFolders(): Set<String> =
        settingsRepository.settings.first().hiddenFolders
}

/** These tracks as app tracks, in id order so a stored and a fresh read compare equal. */
private fun List<LibraryTrack>.toTracks(): List<Track> = map { it.toTrack() }.sortedBy { it.id }

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
    source = SourceMode.LOCAL,
    genre = genre,
)
