package dev.jyotiraditya.dmt.core.source

import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.lyrics.Lyrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * A kind of place music comes from, such as this device or a kind of server; [server] is the saved
 * server to use, or null for this device.
 */
interface MusicSource {

    /** How this kind of source is named and shown. */
    val display: SourceDisplay

    /** The address as this source keeps it, from what the user typed. */
    fun normalize(address: String): String = address

    /** Signs in to the server at [address] and returns the sign-in data to save. */
    suspend fun signIn(address: String, username: String, password: String): Map<String, String>

    /** The tracks as last stored for [server], read without asking it; empty when none are. */
    suspend fun stored(server: ServerSource?): List<Track> = emptyList()

    /** The tracks [server] holds now. */
    suspend fun scan(server: ServerSource?): List<Track>

    /** Forgets what was read, so the next scan reads everything again. */
    fun invalidate() = Unit

    /** Emits whenever the tracks have changed and are worth reading again. */
    fun changes(): Flow<Unit> = emptyFlow()

    /** The lyrics [server] has for [track], or null when it has none. */
    suspend fun lyrics(server: ServerSource, track: Track): Lyrics? = null

    /** The headers a request to [url] needs, such as a sign-in. */
    fun headersFor(server: ServerSource, url: String): Map<String, String> = emptyMap()

    /** What the user can do about [failure] from this source. */
    fun failure(failure: Throwable): SourceFailure = SourceFailure.UNREACHABLE
}
