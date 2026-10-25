package dev.jyotiraditya.dmt.data.remote.jellyfin

import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton

/** This install's Jellyfin device id, read from storage once. */
@Singleton
class JellyfinDeviceId @Inject constructor(private val preferences: PreferencesRepository) {
    @Volatile private var cached: String? = null

    suspend fun get(): String = cached ?: preferences.jellyfinDeviceId().also { cached = it }
}
