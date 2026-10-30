package dev.jyotiraditya.dmt.domain.usecase

import dev.jyotiraditya.dmt.core.model.ServerSource
import dev.jyotiraditya.dmt.core.model.SourceDraft
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.data.repository.SourceRepository
import dev.jyotiraditya.dmt.util.suspendRunCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import kotlin.uuid.Uuid

class ConnectSourceUseCase
@Inject
constructor(
    private val sources: SourceRepository,
    private val settingsRepository: PreferencesRepository,
) {
    /** Saves [draft], signing in and switching to it unless its current sign-in still applies. */
    suspend operator fun invoke(draft: SourceDraft): Result<Unit> = withContext(Dispatchers.IO) {
        suspendRunCatching {
            val kind = sources.of(draft.kind)
            val saved =
                settingsRepository.settings.first().sources.firstOrNull { it.id == draft.id }
            val url = kind.normalize(draft.url)
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
                            kind.signIn(url, username, draft.password)
                        },
                )
            settingsRepository.update {
                val next = it.withSource(source)
                if (keepsSignIn) next else next.copy(activeSource = source.id)
            }
        }
    }
}

/** The host of this server address, or the address itself when it has none. */
private fun String.host(): String = try {
    URI(this).host ?: this
} catch (_: URISyntaxException) {
    this
}
