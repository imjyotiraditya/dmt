package dev.jyotiraditya.dmt.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.jyotiraditya.dmt.MainActivity
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.KEY_STAT_COUNTS
import dev.jyotiraditya.dmt.data.KEY_STAT_TOTAL
import dev.jyotiraditya.dmt.data.MediaLibrary
import dev.jyotiraditya.dmt.data.Track
import dev.jyotiraditya.dmt.data.dmtStore
import dev.jyotiraditya.dmt.data.encodeCounts
import dev.jyotiraditya.dmt.data.toAlbums
import dev.jyotiraditya.dmt.data.toCounts
import dev.jyotiraditya.dmt.data.toFolders
import dev.jyotiraditya.dmt.player.albumArtUri
import dev.jyotiraditya.dmt.player.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ROOT_ID = "root"
private const val TRACKS_ID = "tracks"
private const val ALBUMS_ID = "albums"
private const val FOLDERS_ID = "folders"
private const val ALBUM_PREFIX = "album/"
private const val FOLDER_PREFIX = "folder/"

class PlaybackService : MediaLibraryService() {

    companion object {
        const val KEY_END_AT = "end_at"
        const val KEY_AUDIO_SESSION = "audio_session"
        val CMD_SLEEP_SET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_SET", Bundle.EMPTY)
        val CMD_SLEEP_GET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_GET", Bundle.EMPTY)
        val CMD_AUDIO_SESSION =
            SessionCommand("dev.jyotiraditya.dmt.command.AUDIO_SESSION", Bundle.EMPTY)
    }

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null
    private var sleepEndAt: Long? = null

    @Volatile
    private var libraryCache: List<Track>? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_dmt)
            }
        )
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addAnalyticsListener(
            PlaybackStatsListener(false) { eventTime, playbackStats ->
                val playedMs = playbackStats.totalPlayTimeMs
                if (playedMs < 5_000L || eventTime.timeline.isEmpty) {
                    return@PlaybackStatsListener
                }
                val mediaId = runCatching {
                    eventTime.timeline
                        .getWindow(eventTime.windowIndex, Timeline.Window())
                        .mediaItem
                        .mediaId
                        .toLongOrNull()
                }.getOrNull()
                recordStats(playedMs, mediaId)
            }
        )
        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
    }

    private suspend fun library(): List<Track> =
        libraryCache ?: withContext(Dispatchers.IO) {
            MediaLibrary.scan(this@PlaybackService)
        }.also { libraryCache = it }

    private fun searchLibrary(tracks: List<Track>, query: String): List<Track> =
        tracks.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }

    private fun browsableItem(
        id: String,
        title: String,
        subtitle: String? = null,
        artwork: Uri? = null,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(subtitle)
                .setArtworkUri(artwork)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                .build()
        )
        .build()

    private suspend fun childrenOf(parentId: String): List<MediaItem> {
        val tracks = library()
        return when {
            parentId == ROOT_ID -> listOf(
                browsableItem(TRACKS_ID, getString(R.string.auto_tracks)),
                browsableItem(ALBUMS_ID, getString(R.string.auto_albums)),
                browsableItem(FOLDERS_ID, getString(R.string.auto_folders)),
            )

            parentId == TRACKS_ID -> tracks.map { it.toMediaItem() }

            parentId == ALBUMS_ID -> tracks.toAlbums().map { album ->
                browsableItem(
                    id = ALBUM_PREFIX + album.name,
                    title = album.name,
                    subtitle = album.artist,
                    artwork = album.tracks.firstOrNull()?.albumArtUri(),
                )
            }

            parentId.startsWith(ALBUM_PREFIX) -> {
                val name = parentId.removePrefix(ALBUM_PREFIX)
                tracks.toAlbums()
                    .find { it.name == name }
                    ?.tracks
                    .orEmpty()
                    .map { it.toMediaItem() }
            }

            parentId == FOLDERS_ID -> tracks.toFolders().map { folder ->
                browsableItem(
                    id = FOLDER_PREFIX + folder.path,
                    title = folder.name,
                    subtitle = "${folder.tracks.size} trk",
                )
            }

            parentId.startsWith(FOLDER_PREFIX) -> {
                val path = parentId.removePrefix(FOLDER_PREFIX)
                tracks.toFolders()
                    .find { it.path == path }
                    ?.tracks
                    .orEmpty()
                    .map { it.toMediaItem() }
            }

            else -> emptyList()
        }
    }

    @OptIn(UnstableApi::class)
    private inner class LibraryCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(CMD_SLEEP_SET)
                .add(CMD_SLEEP_GET)
                .add(CMD_AUDIO_SESSION)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> = when (customCommand.customAction) {
            CMD_SLEEP_SET.customAction -> {
                scheduleSleep(args.getLong(KEY_END_AT))
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            CMD_SLEEP_GET.customAction -> {
                val extras = Bundle().apply { putLong(KEY_END_AT, sleepEndAt ?: 0L) }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
            }

            CMD_AUDIO_SESSION.customAction -> {
                val id = (mediaSession?.player as? ExoPlayer)?.audioSessionId ?: 0
                val extras = Bundle().apply { putInt(KEY_AUDIO_SESSION, id) }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, extras))
            }

            else -> super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> = Futures.immediateFuture(
            LibraryResult.ofItem(
                browsableItem(ROOT_ID, getString(R.string.app_name)),
                params
            )
        )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            LibraryResult.ofItemList(childrenOf(parentId), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = scope.future {
            val track = library().find { it.id.toString() == mediaId }
            if (track != null) {
                LibraryResult.ofItem(track.toMediaItem(), null)
            } else {
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            }
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> = scope.future {
            val count = searchLibrary(library(), query).size
            session.notifySearchResultChanged(browser, query, count, params)
            LibraryResult.ofVoid()
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = scope.future {
            val results = searchLibrary(library(), query).map { it.toMediaItem() }
            LibraryResult.ofItemList(results, params)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> = scope.future {
            resolveItems(mediaItems)
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = scope.future {
            val tracks = library()
            val single = mediaItems.singleOrNull()
            val query = single?.requestMetadata?.searchQuery

            when {
                single != null && query != null -> {
                    val matches = searchLibrary(tracks, query).ifEmpty { tracks }
                    MediaSession.MediaItemsWithStartPosition(
                        matches.map { it.toMediaItem() },
                        0,
                        0L
                    )
                }

                single != null && single.localConfiguration == null -> {
                    val index = tracks.indexOfFirst { it.id.toString() == single.mediaId }
                    if (index >= 0) {
                        MediaSession.MediaItemsWithStartPosition(
                            tracks.map { it.toMediaItem() },
                            index,
                            startPositionMs
                        )
                    } else {
                        MediaSession.MediaItemsWithStartPosition(
                            resolveItems(mediaItems),
                            startIndex,
                            startPositionMs
                        )
                    }
                }

                else -> MediaSession.MediaItemsWithStartPosition(
                    resolveItems(mediaItems),
                    startIndex,
                    startPositionMs
                )
            }
        }

        private suspend fun resolveItems(mediaItems: List<MediaItem>): List<MediaItem> {
            val tracks = library()
            return mediaItems.map { item ->
                if (item.localConfiguration != null) {
                    item
                } else {
                    tracks.find { it.id.toString() == item.mediaId }?.toMediaItem() ?: item
                }
            }
        }
    }

    private fun recordStats(playedMs: Long, mediaId: Long?) {
        scope.launch {
            dmtStore.edit { prefs ->
                prefs[KEY_STAT_TOTAL] = (prefs[KEY_STAT_TOTAL] ?: 0L) + playedMs
                if (playedMs >= 30_000L && mediaId != null) {
                    val counts = (prefs[KEY_STAT_COUNTS] ?: "").toCounts().toMutableMap()
                    counts[mediaId] = (counts[mediaId] ?: 0) + 1
                    prefs[KEY_STAT_COUNTS] = counts.encodeCounts()
                }
            }
        }
    }

    private fun scheduleSleep(endAt: Long) {
        sleepJob?.cancel()
        sleepEndAt = endAt.takeIf { it > System.currentTimeMillis() }
        sleepJob = sleepEndAt?.let { end ->
            scope.launch {
                delay(end - System.currentTimeMillis())
                mediaSession?.player?.pause()
                sleepEndAt = null
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
