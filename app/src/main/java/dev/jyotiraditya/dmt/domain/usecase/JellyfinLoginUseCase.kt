package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import dev.jyotiraditya.dmt.util.DispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

class JellyfinLoginUseCase @Inject constructor(
    private val api: JellyfinApi,
    private val settingsRepository: SettingsRepository,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(url: String, username: String, password: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val auth = api.authenticate(url, username, password)
                val settings = settingsRepository.settings.first()

                settingsRepository.save(
                    settings.copy(
                        sourceMode = SourceMode.JELLYFIN,
                        jellyfinUrl = url,
                        jellyfinUserId = auth.userId,
                        jellyfinToken = auth.token,
                    ),
                )
            }
        }
}
