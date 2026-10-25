package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState

/** The connected player, provided at the root of the UI, or null until playback connects. */
val LocalPlayer = staticCompositionLocalOf<Player?> { null }

/** Whether playback is on or about to be, as the pause button shows it. */
@OptIn(UnstableApi::class)
@Composable
fun rememberWantsPlayback(): Boolean {
    val player = LocalPlayer.current ?: return false
    return !rememberPlayPauseButtonState(player).showPlay
}
