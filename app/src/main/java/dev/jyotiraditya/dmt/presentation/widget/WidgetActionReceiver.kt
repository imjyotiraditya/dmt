package dev.jyotiraditya.dmt.presentation.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.jyotiraditya.dmt.playback.PlaybackService
import dev.jyotiraditya.dmt.util.await
import dev.jyotiraditya.dmt.util.togglePlayPause
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "DMT_WIDGET_ACTION"

const val ACTION_WIDGET_PREV = "dev.jyotiraditya.dmt.widget.action.PREV"
const val ACTION_WIDGET_PLAY_PAUSE = "dev.jyotiraditya.dmt.widget.action.PLAY_PAUSE"
const val ACTION_WIDGET_NEXT = "dev.jyotiraditya.dmt.widget.action.NEXT"

/**
 * Dedicated receiver for widget playback button commands.
 *
 * Responsibilities:
 *   - Receive explicit widget actions (PREV, PLAY_PAUSE, NEXT)
 *   - Connect a [MediaController], send the command, then release
 *
 * Explicitly NOT responsible for:
 *   - Updating the widget UI (that flows from PlaybackService → WidgetUpdater)
 *   - Any long-lived state
 *
 * Each broadcast is handled in a coroutine bounded by [goAsync] so the OS never
 * kills the process mid-command, and [PendingResult.finish] is always called.
 */
class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive: $action")

        val pendingResult = goAsync()
        // Per-broadcast scope — cancelled implicitly when the coroutine completes
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            try {
                handleAction(context.applicationContext, action)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleAction(context: Context, action: String) {
        val controller: MediaController? = runCatching {
            withTimeoutOrNull(3_000L) {
                MediaController.Builder(
                    context,
                    SessionToken(
                        context,
                        ComponentName(context, PlaybackService::class.java),
                    ),
                ).buildAsync().await()
            }
        }.getOrNull()

        Log.d(TAG, "controller connected: ${controller != null}")

        try {
            if (controller == null) {
                Log.w(TAG, "MediaController unavailable for action: $action")
                return
            }
            when (action) {
                ACTION_WIDGET_PREV -> controller.seekToPreviousMediaItem()
                ACTION_WIDGET_PLAY_PAUSE -> controller.togglePlayPause()
                ACTION_WIDGET_NEXT -> controller.seekToNextMediaItem()
                else -> Log.w(TAG, "Unknown action: $action")
            }
        } finally {
            controller?.release()
            Log.d(TAG, "controller released")
        }
    }
}
