package dev.jyotiraditya.dmt.data.source.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dmtStore by preferencesDataStore(name = "dmt")

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
val KEY_STAT_COUNTS = stringPreferencesKey("stat_counts")
val KEY_LAST_QUEUE = stringPreferencesKey("last_queue")
val KEY_LAST_INDEX = intPreferencesKey("last_index")
val KEY_LAST_POS = longPreferencesKey("last_pos")
val KEY_LAST_SHUFFLE = booleanPreferencesKey("last_shuffle")
val KEY_LAST_SHUFFLE_ORDER = stringPreferencesKey("last_shuffle_order")
val KEY_LIBRARY_SORT = stringPreferencesKey("sort")
val KEY_ACCENT = stringPreferencesKey("accent")
val KEY_JELLYFIN_DEVICE_ID = stringPreferencesKey("jellyfin_device_id")
val KEY_SOURCES = stringPreferencesKey("sources")
val KEY_ACTIVE_SOURCE = stringPreferencesKey("active_source")

fun String.toCounts(): Map<Long, Int> = split(';')
    .mapNotNull { entry ->
        val parts = entry.split(':')
        val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        id to count
    }
    .toMap()

fun Map<Long, Int>.encodeCounts(): String = entries.joinToString(";") { "${it.key}:${it.value}" }
