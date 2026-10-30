package dev.jyotiraditya.dmt.presentation.settings

import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.model.SourceDraft
import dev.jyotiraditya.dmt.data.repository.SourceRepository
import dev.jyotiraditya.dmt.domain.usecase.ConnectSourceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** A sign-in to a source in progress, for the screens that add one. */
data class SignInState(
    val connecting: Boolean = false,
    /** Why the last sign-in failed. */
    val error: UiText? = null,
)

/** Saves a source and signs in to it, tracking the attempt for a form to show. */
class SourceSignIn
@Inject
constructor(
    private val connectSource: ConnectSourceUseCase,
    private val sources: SourceRepository,
) {

    val state: StateFlow<SignInState>
        field = MutableStateFlow(SignInState())

    /** Saves [draft] and signs in to it; returns whether it worked. */
    suspend fun connect(draft: SourceDraft): Boolean {
        state.value = SignInState(connecting = true)
        val failure = connectSource(draft).exceptionOrNull()
        val source = sources.of(draft.kind)
        state.value =
            SignInState(
                error = failure?.let { UiText(source.failure(it).loginMessage(source.display)) },
            )
        return failure == null
    }
}
