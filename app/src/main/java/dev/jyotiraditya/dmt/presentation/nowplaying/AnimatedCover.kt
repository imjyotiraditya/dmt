package dev.jyotiraditya.dmt.presentation.nowplaying

import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState

private const val COVER_FADE_MS = 320
private const val COVER_VIDEO_MAX = 640

/**
 * The animated cover at [url], looping silently, cropped to fill the space and fading in once its
 * first frame is drawn.
 */
@OptIn(UnstableApi::class)
@Composable
fun AnimatedCover(url: String, playing: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val player =
        remember(url) {
            ExoPlayer.Builder(context).build().apply {
                trackSelectionParameters =
                    trackSelectionParameters
                        .buildUpon()
                        .setMaxVideoSize(COVER_VIDEO_MAX, COVER_VIDEO_MAX)
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
            }
        }

    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(player, playing) {
        player.playWhenReady = playing
    }

    val presentation = rememberPresentationState(player)
    val alpha by
        animateFloatAsState(
            targetValue = if (presentation.coverSurface) 0f else 1f,
            animationSpec = tween(COVER_FADE_MS),
            label = "animatedCover",
        )

    Box(modifier = modifier.clipToBounds()) {
        PlayerSurface(
            player = player,
            modifier =
                Modifier.resizeWithContentScale(ContentScale.Crop, presentation.videoSizeDp)
                    .graphicsLayer { this.alpha = alpha },
        )
    }
}
