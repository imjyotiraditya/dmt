package dev.jyotiraditya.dmt.data.source.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dmtStore by preferencesDataStore(name = "dmt")

val KEY_WAVE = booleanPreferencesKey("wave")
val KEY_COLS = intPreferencesKey("cols")
val KEY_SPECS = booleanPreferencesKey("specs")
val KEY_ACCENT = intPreferencesKey("accent")
val KEY_RAW = booleanPreferencesKey("raw_art")
val KEY_SPEED = floatPreferencesKey("speed")
val KEY_STAT_TOTAL = longPreferencesKey("stat_total_ms")
val KEY_STAT_COUNTS = stringPreferencesKey("stat_counts")
val KEY_LAST_QUEUE = stringPreferencesKey("last_queue")
val KEY_LAST_INDEX = intPreferencesKey("last_index")
val KEY_LAST_POS = longPreferencesKey("last_pos")
val KEY_SOURCE_MODE = intPreferencesKey("source_mode")
val KEY_JELLYFIN_URL = stringPreferencesKey("jellyfin_url")
val KEY_JELLYFIN_USER_ID = stringPreferencesKey("jellyfin_user_id")
val KEY_JELLYFIN_TOKEN = stringPreferencesKey("jellyfin_token")

fun String.toCounts(): Map<Long, Int> =
    split(';')
        .mapNotNull { entry ->
            val parts = entry.split(':')
            val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
            id to count
        }
        .toMap()

fun Map<Long, Int>.encodeCounts(): String = entries.joinToString(";") { "${it.key}:${it.value}" }
