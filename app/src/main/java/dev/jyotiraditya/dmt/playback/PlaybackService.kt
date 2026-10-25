package dev.jyotiraditya.dmt.playback

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.MainActivity
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.usecase.ScanLibraryUseCase
import dev.jyotiraditya.dmt.util.notificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

private const val RESUME_CHANNEL_ID = "resume"
private const val RESUME_NOTIFICATION_ID = 2
private const val MIN_COUNTED_PLAY_MS = 5_000L

/** Plays music in the background and serves the library to the app, the system and Android Auto. */
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var scanLibrary: ScanLibraryUseCase

    @Inject lateinit var preferencesRepository: PreferencesRepository

    @Inject lateinit var dataSourceFactory: DataSource.Factory

    private var mediaSession: MediaLibrarySession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var sleepTimer: SleepTimer
    private var normalizeVolume = false

    /** The ReplayGain volume of the playing track. */
    private var trackVolume = 1f

    /** The sleep timer's fade-out level. */
    private var fadeLevel = 1f
    private var stopOnDismiss = false
    private val gainCache = LruCache<Long, Float>(512)
    private var shufflePlan: ShufflePlan? = null
    private var pendingShuffle: LastSession? = null

    /** Whether a controller just set a new queue, to shuffle once it has loaded. */
    private var queueReplaced = false
    private val browseTree by lazy { MediaBrowseTree(this) }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(this).apply { setSmallIcon(R.drawable.ic_stat_dmt) },
        )
        setListener(
            object : Listener {
                override fun onForegroundServiceStartNotAllowedException() = notifyResumeBlocked()
            },
        )

        val player = buildMusicPlayer(dataSourceFactory)
        sleepTimer =
            SleepTimer(scope, player) { level ->
                fadeLevel = level
                applyVolume()
            }
        player.addAnalyticsListener(playStatsListener())
        player.addListener(sessionListener(player))
        if (player.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            broadcastEffectSession(
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION,
                player.audioSessionId,
            )
        }

        val callback =
            PlaybackLibraryCallback(
                appPackage = packageName,
                scope = scope,
                browseTree = browseTree,
                preferences = preferencesRepository,
                sleepTimer = sleepTimer,
                library = ::library,
                onQueueReplaced = { queueReplaced = true },
            )
        // Covers are decoded no larger than the notification and Android Auto show them.
        val artworkLoader =
            DataSourceBitmapLoader.Builder(this).setMaximumOutputDimension(1024).build()
        mediaSession =
            MediaLibrarySession.Builder(this, player, callback)
                .setBitmapLoader(FreshCopyBitmapLoader(CacheBitmapLoader(artworkLoader)))
                .setSessionActivity(sessionActivity())
                .setMediaButtonPreferences(sessionButtons(player))
                .build()

        scope.launch {
            pendingShuffle = preferencesRepository.lastSession()?.takeIf { it.shuffle }
        }
        scope.launch {
            preferencesRepository.settings.collect { settings ->
                stopOnDismiss = settings.stopOnDismiss
                if (normalizeVolume != settings.normalizeVolume) {
                    normalizeVolume = settings.normalizeVolume
                    applyReplayGain(player.currentMediaItem)
                }
            }
        }
    }

    /** Records each play longer than [MIN_COUNTED_PLAY_MS] in the listening stats. */
    @OptIn(UnstableApi::class)
    private fun playStatsListener(): PlaybackStatsListener {
        val keepHistory = false
        return PlaybackStatsListener(keepHistory) { eventTime, playbackStats ->
            val playedMs = playbackStats.totalPlayTimeMs
            if (playedMs < MIN_COUNTED_PLAY_MS || eventTime.timeline.isEmpty) {
                return@PlaybackStatsListener
            }
            val mediaId = runCatching {
                eventTime.timeline
                    .getWindow(eventTime.windowIndex, Timeline.Window())
                    .mediaItem
                    .mediaId
                    .toLongOrNull()
            }
                .getOrNull()
            scope.launch { preferencesRepository.recordPlayback(playedMs, mediaId) }
        }
    }

    /** Keeps the shuffle order, saved session, buttons, gain and effect session in step. */
    @OptIn(UnstableApi::class)
    private fun sessionListener(player: ExoPlayer) = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (timeline.windowCount == 0) return
            val replaced =
                queueReplaced && reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED
            if (replaced) queueReplaced = false
            val saved = pendingShuffle
            pendingShuffle = null
            val restored = saved?.let { ShufflePlan.parse(it.shuffleOrder) }
            when {
                restored != null && restored.count == timeline.windowCount -> {
                    shufflePlan = restored
                    player.shuffleOrder = restored.order()
                }

                // A fresh queue would otherwise play in order while shuffle is on.
                replaced && player.shuffleModeEnabled -> reshuffle(player)
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            if (shuffleModeEnabled) reshuffle(player)
            publishButtons()
        }

        override fun onRepeatModeChanged(repeatMode: Int) = publishButtons()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) saveSession()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK) saveSession()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            saveSession()
            applyReplayGain(mediaItem)
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            broadcastEffectSession(
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION,
                audioSessionId,
            )
        }
    }

    private fun applyVolume() {
        mediaSession?.player?.volume = trackVolume * fadeLevel
    }

    /** Shuffles the queue with the playing item first. */
    @OptIn(UnstableApi::class)
    private fun reshuffle(player: ExoPlayer) {
        val plan =
            ShufflePlan(
                seed = Random.nextLong(),
                head = player.currentMediaItemIndex,
                count = player.mediaItemCount,
            )
        shufflePlan = plan
        player.shuffleOrder = plan.order()
    }

    /** Sets [trackVolume] from the ReplayGain tag of [mediaItem] when normalizing. */
    private fun applyReplayGain(mediaItem: MediaItem?) {
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (!normalizeVolume || id == null) {
            trackVolume = 1f
            applyVolume()
            return
        }
        scope.launch {
            val volume =
                gainCache.get(id)
                    ?: run {
                        val path = library().track(id)?.path?.takeIf { it.isNotEmpty() }
                        val gainDb = path?.let { withContext(Dispatchers.IO) { trackGainDb(it) } }
                        (gainDb?.let(::volumeForGain) ?: 1f).also { gainCache.put(id, it) }
                    }
            if (mediaSession?.player?.currentMediaItem?.mediaId == mediaItem.mediaId) {
                trackVolume = volume
                applyVolume()
            }
        }
    }

    /** The shuffle and repeat buttons for the notification and Android Auto. */
    @OptIn(UnstableApi::class)
    private fun sessionButtons(player: Player): List<CommandButton> {
        val shuffleIcon =
            if (player.shuffleModeEnabled) {
                CommandButton.ICON_SHUFFLE_ON
            } else {
                CommandButton.ICON_SHUFFLE_OFF
            }
        val repeatIcon =
            when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                else -> CommandButton.ICON_REPEAT_OFF
            }
        return listOf(
            CommandButton.Builder(shuffleIcon)
                .setDisplayName(getString(R.string.auto_shuffle))
                .setSessionCommand(PlaybackCommands.TOGGLE_SHUFFLE)
                .build(),
            CommandButton.Builder(repeatIcon)
                .setDisplayName(getString(R.string.auto_repeat))
                .setSessionCommand(PlaybackCommands.CYCLE_REPEAT)
                .build(),
        )
    }

    @OptIn(UnstableApi::class)
    private fun publishButtons() {
        val session = mediaSession ?: return
        session.setMediaButtonPreferences(sessionButtons(session.player))
    }

    private suspend fun library(): LibrarySnapshot = withContext(Dispatchers.IO) {
        scanLibrary.current()
    }

    /** Saves the queue and position for resuming later. */
    private fun saveSession() {
        val player = mediaSession?.player ?: return
        if (player.mediaItemCount == 0) return
        val ids =
            (0 until player.mediaItemCount).mapNotNull {
                player.getMediaItemAt(it).mediaId.toLongOrNull()
            }
        val session =
            LastSession(
                queueIds = ids,
                index = player.currentMediaItemIndex,
                positionMs = player.currentPosition.coerceAtLeast(0L),
                shuffle = player.shuffleModeEnabled,
                shuffleOrder = shufflePlan?.toString().orEmpty(),
            )
        scope.launch { preferencesRepository.saveSession(session) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaSession

    /** Keeps playing after the app is swiped away, unless paused or set to stop. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        saveSession()
        val player = mediaSession?.player
        val keepPlaying =
            player != null && !stopOnDismiss && player.playWhenReady && player.mediaItemCount > 0
        if (!keepPlaying) {
            player?.pause()
            stopSelf()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onDestroy() {
        clearListener()
        (mediaSession?.player as? ExoPlayer)?.let {
            broadcastEffectSession(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION,
                it.audioSessionId,
            )
        }
        scope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private fun sessionActivity(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE,
    )

    /** Asks the user to open the app when the system blocked resuming from the background. */
    private fun notifyResumeBlocked() {
        val permission = notificationPermission
        if (
            permission != null &&
            ContextCompat.checkSelfPermission(this, permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(
                RESUME_CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
            )
                .setName(getString(R.string.notification_channel_playback))
                .build(),
        )
        manager.notify(
            RESUME_NOTIFICATION_ID,
            NotificationCompat.Builder(this, RESUME_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_dmt)
                .setContentTitle(getString(R.string.notification_resume_title))
                .setContentText(getString(R.string.notification_resume_text))
                .setContentIntent(sessionActivity())
                .setAutoCancel(true)
                .build(),
        )
    }

    /** Tells equalizer apps that the audio session opened or closed. */
    private fun broadcastEffectSession(action: String, sessionId: Int) {
        sendBroadcast(
            Intent(action).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            },
        )
    }
}
