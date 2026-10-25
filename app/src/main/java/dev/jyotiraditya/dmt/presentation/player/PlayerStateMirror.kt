package dev.jyotiraditya.dmt.presentation.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.base.StateStore
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.util.queueWithPosition

/** Mirrors [player]'s track, queue and faults into [store] as they change. */
internal class PlayerStateMirror(
    private val player: Player,
    private val store: StateStore<DmtState>,
    private val onItemChanged: (MediaItem?) -> Unit,
    private val onTracksChanged: (Tracks) -> Unit,
) : Player.Listener {

    fun syncAll() {
        val (queue, queuePosition) = player.queueWithPosition()
        store.reduce {
            it.withNowPlaying(player).copy(queue = queue, queuePosition = queuePosition)
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        store.reduce {
            it.withNowPlaying(player).copy(lyrics = null, fault = null, error = null)
        }
        onItemChanged(mediaItem)
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        store.reduce { it.withNowPlaying(player) }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        store.reduce { it.withNowPlaying(player) }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        val (queue, queuePosition) = player.queueWithPosition()
        store.reduce { it.copy(queue = queue, queuePosition = queuePosition) }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        val (queue, queuePosition) = player.queueWithPosition()
        store.reduce {
            it.withNowPlaying(player).copy(queue = queue, queuePosition = queuePosition)
        }
    }

    /** Pauses and reports a fault when the track holds no audio this device can play. */
    override fun onTracksChanged(tracks: Tracks) {
        if (tracks.groups.isEmpty()) return
        onTracksChanged.invoke(tracks)
        if (tracks.isTypeSupported(C.TRACK_TYPE_AUDIO)) return
        player.pause()
        store.reduce {
            val format = it.tech.firstOrNull { spec -> spec.label == "FMT" }?.value?.lowercase()
            it.copy(fault = UiText(R.string.playback_unsupported, format.orEmpty()))
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        store.reduce {
            it.copy(fault = UiText(R.string.playback_error, error.errorCodeName.lowercase()))
        }
    }
}
