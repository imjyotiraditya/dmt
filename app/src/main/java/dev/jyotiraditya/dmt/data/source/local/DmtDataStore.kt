package dev.jyotiraditya.dmt.data.source.local

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.serialization.json.Json

val Context.dmtStore by preferencesDataStore(name = "dmt")

/** How structured values are written into the store. */
internal val StoreJson = Json { ignoreUnknownKeys = true }

val KEY_NORMALIZE = booleanPreferencesKey("normalize_volume")
val KEY_ANIMATED_ART = booleanPreferencesKey("animated_art")
val KEY_LYRICS_SOURCE = stringPreferencesKey("lyrics_source")
val KEY_ROMANIZED_LYRICS = booleanPreferencesKey("romanized_lyrics")
val KEY_SINGER_COLORS = booleanPreferencesKey("singer_colors")
val KEY_STOP_ON_DISMISS = booleanPreferencesKey("stop_on_dismiss")
val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
val KEY_HIDDEN_FOLDERS = stringSetPreferencesKey("hidden_folders")

val KEY_SPEED = floatPreferencesKey("speed")
val KEY_KEEP_PITCH = booleanPreferencesKey("keep_pitch")
val KEY_SLEEP_CUSTOM_MINUTES = intPreferencesKey("sleep_custom_minutes")
val KEY_SLEEP_FINISH_SONG = booleanPreferencesKey("sleep_finish_song")
val KEY_STAT_TOTAL = longPreferencesKey("stat_total_ms")
val KEY_LIBRARY_GENERATION = longPreferencesKey("library_generation")
val KEY_LAST_SCAN_AT = longPreferencesKey("last_scan_at")
val KEY_LAST_SCAN_TRACKS = intPreferencesKey("last_scan_tracks")
val KEY_PLAY_COUNTS = stringPreferencesKey("play_counts")
val KEY_SESSION = stringPreferencesKey("session")
val KEY_LIBRARY_SORT = stringPreferencesKey("sort")
val KEY_ACCENT = stringPreferencesKey("accent")
val KEY_JELLYFIN_DEVICE_ID = stringPreferencesKey("jellyfin_device_id")
val KEY_SOURCES = stringPreferencesKey("sources")
val KEY_ACTIVE_SOURCE = stringPreferencesKey("active_source")

/** The stored value at [key] read as [T], or null when absent or unreadable. */
internal inline fun <reified T> Preferences.decoded(key: Preferences.Key<String>): T? =
    this[key]?.let {
        try {
            StoreJson.decodeFromString<T>(it)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

/** Stores [value] at [key] as JSON. */
internal inline fun <reified T> MutablePreferences.encode(key: Preferences.Key<String>, value: T) {
    this[key] = StoreJson.encodeToString(value)
}
