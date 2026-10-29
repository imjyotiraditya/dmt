package dev.jyotiraditya.dmt.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceDraft
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SourceFormUiState(
    /** The saved source the form edits, or null for a new one. */
    val saved: ServerSource? = null,
    val signIn: SignInState = SignInState(),
    /** Whether the source was saved or removed, so the form can close. */
    val done: Boolean = false,
)

/** The form that adds the source in [form], or edits or signs back in to a saved one. */
@HiltViewModel(assistedFactory = SourceFormViewModel.Factory::class)
class SourceFormViewModel
@AssistedInject
constructor(
    @Assisted private val form: Destination.SourceForm,
    private val preferences: PreferencesRepository,
    private val signIn: SourceSignIn,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(form: Destination.SourceForm): SourceFormViewModel
    }

    private val done = MutableStateFlow(false)

    val state: StateFlow<SourceFormUiState> =
        combine(preferences.loadedSettings, signIn.state, done, ::stateOf)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(preferences.loadedSettings.value, signIn.state.value, done.value),
            )

    fun save(draft: SourceDraft) {
        viewModelScope.launch { if (signIn.connect(draft)) done.value = true }
    }

    fun remove() {
        val id = form.sourceId ?: return
        viewModelScope.launch {
            preferences.update { it.without(id) }
            done.value = true
        }
    }

    private fun stateOf(settings: DmtSettings?, signIn: SignInState, done: Boolean) =
        SourceFormUiState(
            saved = settings?.sources?.firstOrNull { it.id == form.sourceId },
            signIn = signIn,
            done = done,
        )
}
