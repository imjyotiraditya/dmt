package dev.jyotiraditya.dmt.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

private const val SEEK_HOLD_STEP_MS = 5_000L

/** The music player: bundled decoders first, offload where supported, focus and noisy handled. */
@OptIn(UnstableApi::class)
internal fun Context.buildMusicPlayer(dataSourceFactory: DataSource.Factory): ExoPlayer {
    val renderers =
        DefaultRenderersFactory(this)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    val offload =
        AudioOffloadPreferences.Builder()
            .setAudioOffloadMode(AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
            .setIsGaplessSupportRequired(true)
            .build()
    val trackSelector =
        DefaultTrackSelector(this).apply {
            parameters = buildUponParameters().setAudioOffloadPreferences(offload).build()
        }
    val attributes =
        AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
    val handleAudioFocus = true
    return ExoPlayer.Builder(this, renderers)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .setAudioAttributes(attributes, handleAudioFocus)
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setTrackSelector(trackSelector)
        .setSeekBackIncrementMs(SEEK_HOLD_STEP_MS)
        .setSeekForwardIncrementMs(SEEK_HOLD_STEP_MS)
        .build()
}
