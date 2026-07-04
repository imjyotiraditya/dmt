package dev.jyotiraditya.dmt.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.jyotiraditya.dmt.MainActivity
import dev.jyotiraditya.dmt.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    companion object {
        const val KEY_END_AT = "end_at"
        const val KEY_AUDIO_SESSION = "audio_session"
        val CMD_SLEEP_SET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_SET", Bundle.EMPTY)
        val CMD_SLEEP_GET = SessionCommand("dev.jyotiraditya.dmt.command.SLEEP_GET", Bundle.EMPTY)
        val CMD_AUDIO_SESSION =
            SessionCommand("dev.jyotiraditya.dmt.command.AUDIO_SESSION", Bundle.EMPTY)
    }

    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var sleepJob: Job? = null
    private var sleepEndAt: Long? = null

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
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
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

    @OptIn(UnstableApi::class)
    private inner class SessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
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
