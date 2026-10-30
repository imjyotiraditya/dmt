package dev.jyotiraditya.dmt.source.jellyfin

import dev.jyotiraditya.dmt.core.source.InstallId
import javax.inject.Inject
import javax.inject.Singleton

/** This install's Jellyfin device id, read once; Jellyfin keeps a session per id. */
@Singleton
class JellyfinDeviceId @Inject constructor(private val installId: InstallId) {
    @Volatile private var cached: String? = null

    suspend fun get(): String = cached ?: installId.get().also { cached = it }
}
