package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.di.JellyfinSource
import dev.jyotiraditya.dmt.di.Local
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaSourceProvider @Inject constructor(
    @Local private val local: MediaRepository,
    @JellyfinSource private val jellyfin: MediaRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun current(): MediaRepository =
        when (settingsRepository.settings.first().sourceMode) {
            SourceMode.LOCAL -> local
            SourceMode.JELLYFIN -> jellyfin
        }
}
