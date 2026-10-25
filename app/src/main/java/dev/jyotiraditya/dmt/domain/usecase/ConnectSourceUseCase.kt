package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinApi
import dev.jyotiraditya.dmt.data.remote.jellyfin.normalizeServerUrl
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicAccount
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicApi
import dev.jyotiraditya.dmt.data.remote.subsonic.normalizeSubsonicUrl
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceDraft
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.util.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI
import javax.inject.Inject
import kotlin.uuid.Uuid

class ConnectSourceUseCase
@Inject
constructor(
    private val jellyfinApi: JellyfinApi,
    private val subsonicApi: SubsonicApi,
    private val settingsRepository: PreferencesRepository,
) {
    /** Saves [draft], signing in and switching to it unless its current sign-in still applies. */
    suspend operator fun invoke(draft: SourceDraft): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val saved =
                settingsRepository.settings.first().sources.firstOrNull { it.id == draft.id }
            val url = draft.kind.normalize(draft.url)
            val username = draft.username.trim()
            val keepsSignIn =
                saved != null &&
                    saved.signedIn &&
                    draft.password.isEmpty() &&
                    saved.url == url &&
                    saved.username == username
            val source =
                ServerSource(
                    id = draft.id ?: Uuid.random().toHexString(),
                    kind = draft.kind,
                    name = draft.name.trim().ifEmpty { url.host() },
                    url = url,
                    username = username,
                    secrets =
                        if (keepsSignIn) {
                            checkNotNull(saved).secrets
                        } else {
                            signIn(draft.kind, url, username, draft.password)
                        },
                )
            settingsRepository.update {
                val next = it.withSource(source)
                if (keepsSignIn) next else next.copy(activeSource = source.id)
            }
        }
    }

    /** Signs in to a [kind] server and returns its sign-in data. */
    private suspend fun signIn(
        kind: SourceMode,
        url: String,
        username: String,
        password: String,
    ): Map<String, String> = when (kind) {
        SourceMode.JELLYFIN -> jellyfinApi.authenticate(url, username, password).secrets

        SourceMode.SUBSONIC ->
            SubsonicAccount.create(url, username, password)
                .also { subsonicApi.ping(it) }
                .secrets

        SourceMode.LOCAL -> throw IllegalArgumentException("This device has no sign-in")
    }
}

/** Normalises [url] for this kind of server. */
private fun SourceMode.normalize(url: String): String = when (this) {
    SourceMode.SUBSONIC -> normalizeSubsonicUrl(url)

    SourceMode.JELLYFIN,
    SourceMode.LOCAL,
    -> normalizeServerUrl(url)
}

/** The host of this server address, or the address itself when it has none. */
private fun String.host(): String = runCatching { URI(this).host }.getOrNull() ?: this
