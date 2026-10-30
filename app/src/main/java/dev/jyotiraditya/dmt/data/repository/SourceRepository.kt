package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceDisplay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** A source in use and the server it reads, or no server for this device. */
data class ActiveSource(val source: MusicSource, val server: ServerSource?)

/** Every kind of music source, and the one the settings pick. */
@Singleton
class SourceRepository
@Inject
constructor(
    private val sources: Map<SourceMode, @JvmSuppressWildcards MusicSource>,
    private val settingsRepository: PreferencesRepository,
) {
    /** How each kind of source is shown. */
    val displays: Map<SourceMode, SourceDisplay> = sources.mapValues { it.value.display }

    fun of(mode: SourceMode): MusicSource = sources.getValue(mode)

    /** The source the settings pick, with its server. */
    suspend fun active(): ActiveSource {
        val server = settingsRepository.settings.first().server
        return ActiveSource(of(server?.kind ?: SourceMode.LOCAL), server)
    }

    /** The headers a request to [url] needs for the server in use. */
    fun headersFor(url: String): Map<String, String> {
        val server = settingsRepository.loadedSettings.value?.server ?: return emptyMap()
        return of(server.kind).headersFor(server, url)
    }
}
