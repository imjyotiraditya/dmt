package dev.jyotiraditya.dmt.core.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The current time while the app is visible, updated as each minute starts and whenever the clock
 * or time zone changes.
 */
@Composable
fun rememberMinuteClock(): State<Instant> {
    val context = LocalContext.current
    val now: MutableState<Instant> = remember { mutableStateOf(Clock.System.now()) }
    LifecycleStartEffect(context) {
        now.value = Clock.System.now()
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    now.value = Clock.System.now()
                }
            }
        val changes =
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
        ContextCompat.registerReceiver(
            context,
            receiver,
            changes,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    return now
}
