package dev.jyotiraditya.dmt.presentation.widget

/**
 * Snapshot of playback state needed to render the widget.
 * Persisted via [WidgetPlaybackStateRepository] so the widget survives
 * process death, force-stop, reboot, and launcher recreation.
 */
data class WidgetPlaybackState(
    /** Whether there is a track currently loaded. */
    val hasTrack: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    /** Current playback position in milliseconds. */
    val positionMs: Long = 0L,
    /** Track duration in milliseconds. 0 if unknown. */
    val durationMs: Long = 0L,
    /** URI of the artwork image, or null when unavailable. */
    val artworkUriString: String? = null,
    /**
     * URI of the track file itself (content:// or file://).
     * Used as a fallback to extract embedded artwork via [android.media.MediaMetadataRetriever]
     * when [artworkUriString] is null (common for local files where art is embedded in the tag).
     */
    val trackUriString: String? = null,
) {
    companion object {
        /** Idle state rendered before any track has ever played. */
        val IDLE = WidgetPlaybackState()
    }
}
