package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.di.JellyfinSource
import dev.jyotiraditya.dmt.di.Local
import dev.jyotiraditya.dmt.di.SubsonicSource
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** The music source the settings pick: this device, Jellyfin or Subsonic. */
@Singleton
class MediaSourceProvider
@Inject
constructor(
    @Local private val local: MediaRepository,
    @JellyfinSource private val jellyfin: MediaRepository,
    @SubsonicSource private val subsonic: MediaRepository,
    private val settingsRepository: PreferencesRepository,
) {
    suspend fun current(): MediaRepository = when (settingsRepository.settings.first().sourceMode) {
        SourceMode.LOCAL -> local
        SourceMode.JELLYFIN -> jellyfin
        SourceMode.SUBSONIC -> subsonic
    }
}
