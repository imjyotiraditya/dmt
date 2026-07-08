package dev.jyotiraditya.dmt.playback

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.MainActivity
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.toAlbums
import dev.jyotiraditya.dmt.domain.model.toFolders
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import dev.jyotiraditya.dmt.domain.repository.StatsRepository
import dev.jyotiraditya.dmt.domain.usecase.MediaSourceProvider
import dev.jyotiraditya.dmt.util.toMediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val ROOT_ID = "root"
private const val TRACKS_ID = "tracks"
private const val ALBUMS_ID = "albums"
private const val FOLDERS_ID = "folders"
private const val ALBUM_PREFIX = "album/"
private const val FOLDER_PREFIX = "folder/"

@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject
    lateinit var mediaSourceProvider: MediaSourceProvider

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var statsRepository: StatsRepository

    companion object {
        const val KEY_END_AT = "end_at"
        const val KEY_AUDIO_SESSION = "audio_session"
        val CMD_SLEEP_SET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_SET", Bundle.EMPTY)
        val CMD_SLEEP_GET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_GET", Bundle.EMPTY)
        val CMD_AUDIO_SESSION =
            SessionCommand("dev.jyotiraditya.dmt.command.AUDIO_SESSION", Bundle.EMPTY)
        val CMD_TOGGLE_SHUFFLE =
            SessionCommand("dev.jyotiraditya.dmt.command.TOGGLE_SHUFFLE", Bundle.EMPTY)
        val CMD_CYCLE_REPEAT =
            SessionCommand("dev.jyotiraditya.dmt.command.CYCLE_REPEAT", Bundle.EMPTY)
    }

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null
    private var sleepEndAt: Long? = null

    @Volatile
    private var libraryCache: List<Track>? = null

    @Volatile
    private var libraryCacheSource: MediaRepository? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply {
                setSmallIcon(R.drawable.ic_stat_dmt)
            },
        )
        val handleAudioFocus = true
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                handleAudioFocus,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        val keepPlaybackHistory = false
        player.addAnalyticsListener(
            PlaybackStatsListener(keepPlaybackHistory) { eventTime, playbackStats ->
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
            },
        )
        player.addListener(
            object : Player.Listener {
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) =
                    publishButtons()

                override fun onRepeatModeChanged(repeatMode: Int) = publishButtons()

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (!isPlaying) saveSession()
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    saveSession()
                }
            },
        )
        val artworkLoader = DataSourceBitmapLoader.Builder(this).build()
        mediaSession = MediaLibrarySession.Builder(this, player, LibraryCallback())
            .setBitmapLoader(FreshCopyBitmapLoader(CacheBitmapLoader(artworkLoader)))
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setMediaButtonPreferences(sessionButtons(player))
            .build()
    }

    @OptIn(UnstableApi::class)
    private fun sessionButtons(player: Player): List<CommandButton> {
        val shuffleIcon = if (player.shuffleModeEnabled) {
            CommandButton.ICON_SHUFFLE_ON
        } else {
            CommandButton.ICON_SHUFFLE_OFF
        }
        val repeatIcon = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
            Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
            else -> CommandButton.ICON_REPEAT_OFF
        }
        return listOf(
            CommandButton.Builder(shuffleIcon)
                .setDisplayName(getString(R.string.auto_shuffle))
                .setSessionCommand(CMD_TOGGLE_SHUFFLE)
                .build(),
            CommandButton.Builder(repeatIcon)
                .setDisplayName(getString(R.string.auto_repeat))
                .setSessionCommand(CMD_CYCLE_REPEAT)
                .build(),
        )
    }

    @OptIn(UnstableApi::class)
    private fun publishButtons() {
        val session = mediaSession ?: return
        session.setMediaButtonPreferences(sessionButtons(session.player))
    }

    private suspend fun library(): List<Track> {
        val source = mediaSourceProvider.current()
        if (libraryCacheSource !== source) {
            libraryCache = null
            libraryCacheSource = source
        }
        return libraryCache ?: withContext(Dispatchers.IO) {
            source.scan()
        }.also { libraryCache = it }
    }

    private fun searchLibrary(tracks: List<Track>, query: String): List<Track> =
        tracks.filter {
            it.title.contains(query, true) ||
                    it.artist.contains(query, true) ||
                    it.album.contains(query, true)
        }

    @OptIn(UnstableApi::class)
    private fun browsableItem(
        id: String,
        title: String,
        subtitle: String? = null,
        artwork: Uri? = null,
        childrenAsGrid: Boolean = false,
    ): MediaItem {
        val extras = Bundle().apply {
            putInt(
                MediaConstants.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE,
                if (childrenAsGrid) {
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_GRID_ITEM
                } else {
                    MediaConstants.EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
                },
            )
        }
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(subtitle)
                    .setArtworkUri(artwork)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setExtras(extras)
                    .build(),
            )
            .build()
    }

    private suspend fun childrenOf(parentId: String): List<MediaItem> {
        val tracks = library()
        return when {
            parentId == ROOT_ID -> listOf(
                browsableItem(
                    id = TRACKS_ID,
                    title = getString(R.string.auto_tracks),
                ),
                browsableItem(
                    id = ALBUMS_ID,
                    title = getString(R.string.auto_albums),
                    childrenAsGrid = true,
                ),
                browsableItem(
                    id = FOLDERS_ID,
                    title = getString(R.string.auto_folders),
                ),
            )

            parentId == TRACKS_ID -> tracks.map { it.toMediaItem() }

            parentId == ALBUMS_ID -> tracks.toAlbums().map { album ->
                browsableItem(
                    id = ALBUM_PREFIX + album.name,
                    title = album.name,
                    subtitle = album.artist,
                    artwork = album.tracks.firstOrNull()?.coverUri,
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
                .add(CMD_TOGGLE_SHUFFLE)
                .add(CMD_CYCLE_REPEAT)
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
        ): ListenableFuture<SessionResult> =
            when (customCommand.customAction) {
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

                CMD_TOGGLE_SHUFFLE.customAction -> {
                    session.player.shuffleModeEnabled = !session.player.shuffleModeEnabled
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                CMD_CYCLE_REPEAT.customAction -> {
                    session.player.repeatMode = when (session.player.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> super.onCustomCommand(session, controller, customCommand, args)
            }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(
                LibraryResult.ofItem(
                    browsableItem(
                        id = ROOT_ID,
                        title = getString(R.string.app_name),
                    ),
                    params,
                ),
            )

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            scope.future {
                LibraryResult.ofItemList(childrenOf(parentId), params)
            }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            scope.future {
                val track = library().find { it.id.toString() == mediaId }
                if (track != null) {
                    LibraryResult.ofItem(track.toMediaItem(), null)
                } else {
                    LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
                }
            }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> =
            scope.future {
                val count = searchLibrary(library(), query).size
                session.notifySearchResultChanged(
                    browser,
                    query,
                    count,
                    params,
                )
                LibraryResult.ofVoid()
            }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> =
            scope.future {
                val results = searchLibrary(library(), query).map { it.toMediaItem() }
                LibraryResult.ofItemList(results, params)
            }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> =
            scope.future {
                resolveItems(mediaItems)
            }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            scope.future {
                val tracks = library()
                val single = mediaItems.singleOrNull()
                val query = single?.requestMetadata?.searchQuery

                when {
                    single != null && query != null -> {
                        val matches = searchLibrary(tracks, query).ifEmpty { tracks }
                        MediaSession.MediaItemsWithStartPosition(
                            matches.map { it.toMediaItem() },
                            0,
                            0L,
                        )
                    }

                    single != null && single.localConfiguration == null -> {
                        val index = tracks.indexOfFirst { it.id.toString() == single.mediaId }
                        if (index >= 0) {
                            MediaSession.MediaItemsWithStartPosition(
                                tracks.map { it.toMediaItem() },
                                index,
                                startPositionMs,
                            )
                        } else {
                            MediaSession.MediaItemsWithStartPosition(
                                resolveItems(mediaItems),
                                startIndex,
                                startPositionMs,
                            )
                        }
                    }

                    else -> MediaSession.MediaItemsWithStartPosition(
                        resolveItems(mediaItems),
                        startIndex,
                        startPositionMs,
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

    private fun saveSession() {
        val player = mediaSession?.player ?: return
        if (player.mediaItemCount == 0) return
        val ids = (0 until player.mediaItemCount)
            .mapNotNull { player.getMediaItemAt(it).mediaId.toLongOrNull() }
        val session = LastSession(
            queueIds = ids,
            index = player.currentMediaItemIndex,
            positionMs = player.currentPosition.coerceAtLeast(0L),
        )
        scope.launch {
            settingsRepository.saveSession(session)
        }
    }

    private fun recordStats(playedMs: Long, mediaId: Long?) {
        scope.launch {
            statsRepository.recordPlayback(playedMs, mediaId)
        }
    }

    private fun scheduleSleep(endAt: Long) {
        sleepJob?.cancel()
        sleepEndAt = endAt.takeIf { it > System.currentTimeMillis() }
        sleepJob = sleepEndAt?.let { end ->
            scope.launch {
                delay((end - System.currentTimeMillis()).milliseconds)
                mediaSession?.player?.pause()
                sleepEndAt = null
            }
        }
    }

    @OptIn(UnstableApi::class)
    private class FreshCopyBitmapLoader(private val delegate: BitmapLoader) : BitmapLoader {
        override fun supportsMimeType(mimeType: String): Boolean =
            delegate.supportsMimeType(mimeType)

        override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
            fresh(delegate.decodeBitmap(data))

        override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
            fresh(delegate.loadBitmap(uri))

        private fun fresh(future: ListenableFuture<Bitmap>): ListenableFuture<Bitmap> =
            Futures.transform(
                future,
                { it.copy(it.config ?: Bitmap.Config.ARGB_8888, false) ?: it },
                MoreExecutors.directExecutor(),
            )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveSession()
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
