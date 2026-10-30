package dev.jyotiraditya.dmt.data.repository

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.di.ApplicationScope
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.NowPlaying
import dev.jyotiraditya.dmt.domain.model.PlaybackFault
import dev.jyotiraditya.dmt.domain.model.SleepState
import dev.jyotiraditya.dmt.domain.model.steppedSpeed
import dev.jyotiraditya.dmt.library.takeIfSet
import dev.jyotiraditya.dmt.playback.PlaybackCommands
import dev.jyotiraditya.dmt.playback.PlaybackConnection
import dev.jyotiraditya.dmt.util.clearUpcoming
import dev.jyotiraditya.dmt.util.enqueue
import dev.jyotiraditya.dmt.util.jumpTo
import dev.jyotiraditya.dmt.util.moveInQueue
import dev.jyotiraditya.dmt.util.playNext
import dev.jyotiraditya.dmt.util.playTracks
import dev.jyotiraditya.dmt.util.queueEntries
import dev.jyotiraditya.dmt.util.removeFromQueue
import dev.jyotiraditya.dmt.util.resolveQueue
import dev.jyotiraditya.dmt.util.suspendRunCatching
import dev.jyotiraditya.dmt.util.toQueueRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/** How long the connection stays after the last screen stops following it. */
private const val STOP_TIMEOUT_MS = 5_000L

/** The playback service's player and what it plays, connected while a screen follows it. */
@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class PlaybackRepository
@Inject
constructor(
    private val connection: PlaybackConnection,
    private val libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    /** A connected controller and the latest extras its session shared. */
    private class Session(val controller: MediaController, val extras: StateFlow<Bundle>)

    private var sessionRestored = false

    private val session: StateFlow<Session?> = callbackFlow {
        val sessionExtras = MutableStateFlow(Bundle.EMPTY)
        val listener =
            object : MediaController.Listener {
                override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                    sessionExtras.value = extras
                }
            }
        val controller = suspendRunCatching { connection.connect(listener) }.getOrNull()
        if (controller != null) {
            sessionExtras.value = controller.sessionExtras
            send(Session(controller, sessionExtras))
            launch { follow(controller) }
        }
        awaitClose { controller?.release() }
    }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val controller: MediaController?
        get() = session.value?.controller

    /** The connected player, or null until it connects. */
    val player: StateFlow<Player?> =
        session
            .map { it?.controller }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val nowPlaying: StateFlow<NowPlaying> =
        session
            .flatMapLatest { it?.controller?.nowPlayingUpdates() ?: flowOf(NowPlaying()) }
            .combine(libraryRepository.library) { playing, library -> playing.from(library) }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), NowPlaying())

    val sleep: StateFlow<SleepState> =
        session
            .flatMapLatest { it?.extras ?: flowOf(Bundle.EMPTY) }
            .map { extras ->
                SleepState(
                    endAt = extras.getLong(PlaybackCommands.KEY_END_AT),
                    finishSong = extras.getBoolean(PlaybackCommands.KEY_FINISH_SONG),
                )
            }
            .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SleepState())

    fun play(tracks: List<Track>, index: Int) {
        controller?.playTracks(tracks, index)
    }

    /** Plays [tracks] with shuffle on, from a random one. */
    fun shufflePlay(tracks: List<Track>) {
        val controller = controller ?: return
        if (tracks.isEmpty()) return
        controller.playTracks(tracks, tracks.indices.random())
        controller.shuffleModeEnabled = true
    }

    /** Adds [tracks] to the end of the queue; false when the player is not connected. */
    fun enqueue(tracks: List<Track>): Boolean {
        val controller = controller ?: return false
        controller.enqueue(tracks)
        return true
    }

    /** Inserts [tracks] right after the playing one; false when the player is not connected. */
    fun playNext(tracks: List<Track>): Boolean {
        val controller = controller ?: return false
        controller.playNext(tracks)
        return true
    }

    /** Moves the item at player index [from] to index [to]. */
    fun move(from: Int, to: Int) {
        controller?.moveInQueue(from, to)
    }

    fun remove(index: Int) {
        controller?.removeFromQueue(index)
    }

    /** Removes everything queued after the playing item. */
    fun clearUpcoming() {
        val playing = nowPlaying.value
        controller?.clearUpcoming(playing.queue, playing.queuePosition)
    }

    fun jumpTo(index: Int) {
        controller?.jumpTo(index)
    }

    /** Seeks to [fraction] of the playing item. */
    fun seek(fraction: Float) {
        val duration = nowPlaying.value.durationMs
        if (duration > 0) controller?.seekTo((fraction * duration).toLong())
    }

    /** Starts the sleep timer for [minutes], or stops it at 0. */
    fun setSleep(minutes: Int) {
        scope.launch {
            val endAt = if (minutes == 0) 0L else now() + minutes.minutes.inWholeMilliseconds
            sendSleep(endAt, preferences.settings.first().sleepFinishSong)
        }
    }

    /** Stops playback when the playing song ends. */
    fun sleepAtSongEnd() = sendSleep(now(), finishSong = true)

    /** Plays at [speed], stepped, and keeps it for next time. */
    fun setSpeed(speed: Float) {
        val controller = controller ?: return
        val stepped = steppedSpeed(speed)
        if (stepped == steppedSpeed(controller.playbackParameters.speed)) return
        scope.launch {
            val keepPitch = preferences.settings.first().keepPitch
            controller.playbackParameters = parameters(stepped, keepPitch)
            preferences.saveSpeed(stepped)
        }
    }

    private fun sendSleep(endAt: Long, finishSong: Boolean) {
        controller?.sendCustomCommand(
            PlaybackCommands.SLEEP_SET,
            Bundle().apply {
                putLong(PlaybackCommands.KEY_END_AT, endAt)
                putBoolean(PlaybackCommands.KEY_FINISH_SONG, finishSong)
            },
        )
    }

    /** Applies the saved speed, restores the last session, and follows settings that apply now. */
    private suspend fun follow(controller: MediaController) = coroutineScope {
        val settings = preferences.settings.first()
        controller.playbackParameters = parameters(preferences.savedSpeed(), settings.keepPitch)
        launch { restoreSession(controller) }
        launch {
            preferences.settings
                .map { it.keepPitch }
                .distinctUntilChanged()
                .drop(1)
                .collect { keepPitch ->
                    val speed = controller.playbackParameters.speed
                    controller.playbackParameters = parameters(speed, keepPitch)
                }
        }
        launch {
            preferences.settings
                .map { it.sleepFinishSong }
                .distinctUntilChanged()
                .drop(1)
                .collect { finishSong ->
                    val endAt = sleep.value.endAt
                    if (endAt > now()) sendSleep(endAt, finishSong)
                }
        }
        // Music from one source does not play once another is in use.
        launch {
            preferences.settings
                .map { it.server?.id }
                .distinctUntilChanged()
                .drop(1)
                .collect {
                    controller.stop()
                    controller.clearMediaItems()
                }
        }
    }

    /** Queues the last session once the library is known, unless something is queued already. */
    private suspend fun restoreSession(controller: MediaController) {
        val tracks = libraryRepository.library.first { it.tracks.isNotEmpty() }.tracks
        if (sessionRestored) return
        sessionRestored = true
        if (controller.mediaItemCount > 0) return
        val session = preferences.lastSession() ?: return
        val (existing, index, position) = session.resolveQueue(tracks) ?: return
        controller.shuffleModeEnabled = session.shuffle
        controller.setMediaItems(existing.map { it.toQueueRequest() }, index, position)
        controller.prepare()
    }

    private fun now(): Long = System.currentTimeMillis()

    private fun parameters(speed: Float, keepPitch: Boolean) =
        PlaybackParameters(speed, if (keepPitch) 1f else speed)
}

/** What this player is on, again after every change to it. */
@OptIn(UnstableApi::class)
private fun Player.nowPlayingUpdates(): Flow<NowPlaying> = channelFlow {
    send(nowPlaying())
    listenTo(
        Player.EVENT_MEDIA_ITEM_TRANSITION,
        Player.EVENT_MEDIA_METADATA_CHANGED,
        Player.EVENT_PLAYBACK_STATE_CHANGED,
        Player.EVENT_TIMELINE_CHANGED,
        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
        Player.EVENT_TRACKS_CHANGED,
        Player.EVENT_PLAYER_ERROR,
    ) {
        trySend(nowPlaying())
    }
}

private fun Player.nowPlaying(): NowPlaying {
    val meta = mediaMetadata
    val queue = queueEntries()
    return NowPlaying(
        mediaId = currentMediaItem?.mediaId,
        title = meta.title?.toString().orEmpty(),
        artist = meta.artist?.toString().orEmpty(),
        album = meta.albumTitle?.toString().orEmpty(),
        durationMs = duration.takeIfSet()?.coerceAtLeast(0L) ?: 0L,
        queue = queue,
        queuePosition = queue.indexOfFirst { it.index == currentMediaItemIndex },
        fault = fault(),
    )
}

private fun Player.fault(): PlaybackFault? {
    playerError?.let {
        return PlaybackFault.Failed(it.errorCodeName)
    }
    val tracks = currentTracks
    if (tracks.groups.isNotEmpty() && !tracks.isTypeSupported(C.TRACK_TYPE_AUDIO)) {
        return PlaybackFault.Unsupported
    }
    return null
}

/** This item with its library track, which fills in what the player does not know yet. */
private fun NowPlaying.from(library: LibrarySnapshot): NowPlaying {
    val track = library.track(mediaId) ?: return this
    return copy(
        track = track,
        title = title.ifEmpty { track.title },
        artist = artist.ifEmpty { track.artist },
        album = album.ifEmpty { track.album },
        durationMs = durationMs.takeIf { it > 0L } ?: track.durationMs,
    )
}
