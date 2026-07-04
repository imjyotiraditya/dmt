package dev.jyotiraditya.dmt.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dmtStore by preferencesDataStore(name = "dmt")

val KEY_WAVE = booleanPreferencesKey("wave")
val KEY_COLS = intPreferencesKey("cols")
val KEY_SPECS = booleanPreferencesKey("specs")
val KEY_ACCENT = intPreferencesKey("accent")
val KEY_RAW = booleanPreferencesKey("raw_art")
val KEY_SPEED = floatPreferencesKey("speed")
