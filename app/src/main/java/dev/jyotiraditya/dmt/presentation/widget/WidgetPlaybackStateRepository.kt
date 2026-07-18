package dev.jyotiraditya.dmt.presentation.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Durable store for [WidgetPlaybackState].
 *
 * Backed by [SharedPreferences] so state survives process death, force-stop,
 * device reboot, and launcher recreation.  An in-memory cache eliminates
 * redundant disk reads on hot paths.
 *
 * This is a plain object — no Hilt injection — because [android.appwidget.AppWidgetProvider]
 * and [android.content.BroadcastReceiver] do not participate in Hilt's component hierarchy.
 */
object WidgetPlaybackStateRepository {

    private const val PREFS_NAME = "dmt_widget_state"
    private const val KEY_HAS_TRACK = "has_track"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_IS_PLAYING = "is_playing"
    private const val KEY_POSITION_MS = "position_ms"
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_ARTWORK_URI = "artwork_uri"
    private const val KEY_TRACK_URI = "track_uri"

    @Volatile
    private var cache: WidgetPlaybackState? = null

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Persist [state] to shared preferences and update the in-memory cache.
     */
    fun save(context: Context, state: WidgetPlaybackState) {
        cache = state
        prefs(context).edit().apply {
            putBoolean(KEY_HAS_TRACK, state.hasTrack)
            putString(KEY_TITLE, state.title)
            putString(KEY_ARTIST, state.artist)
            putBoolean(KEY_IS_PLAYING, state.isPlaying)
            putLong(KEY_POSITION_MS, state.positionMs)
            putLong(KEY_DURATION_MS, state.durationMs)
            putString(KEY_ARTWORK_URI, state.artworkUriString)
            putString(KEY_TRACK_URI, state.trackUriString)
            apply()
        }
    }

    /**
     * Load the last persisted state. Returns [WidgetPlaybackState.IDLE] on first run.
     */
    fun load(context: Context): WidgetPlaybackState {
        cache?.let { return it }
        val p = prefs(context)
        return WidgetPlaybackState(
            hasTrack = p.getBoolean(KEY_HAS_TRACK, false),
            title = p.getString(KEY_TITLE, "") ?: "",
            artist = p.getString(KEY_ARTIST, "") ?: "",
            isPlaying = p.getBoolean(KEY_IS_PLAYING, false),
            positionMs = p.getLong(KEY_POSITION_MS, 0L),
            durationMs = p.getLong(KEY_DURATION_MS, 0L),
            artworkUriString = p.getString(KEY_ARTWORK_URI, null),
            trackUriString = p.getString(KEY_TRACK_URI, null),
        ).also { cache = it }
    }

    /** Invalidate in-memory cache, forcing next [load] to re-read from disk. */
    fun invalidateCache() {
        cache = null
    }
}
