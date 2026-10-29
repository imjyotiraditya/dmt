package dev.jyotiraditya.dmt.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.SourceDraft
import dev.jyotiraditya.dmt.presentation.settings.PermissionListener
import dev.jyotiraditya.dmt.presentation.settings.SignInState
import dev.jyotiraditya.dmt.presentation.settings.SourceSignIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val settings: DmtSettings = DmtSettings(),
    val signIn: SignInState = SignInState(),
    val hasPermission: Boolean = false,
)

/** First-run setup: permissions, the music source and its sign-in. */
@HiltViewModel
class SetupViewModel
@Inject
constructor(
    private val libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
    private val signIn: SourceSignIn,
) : ViewModel(),
    PermissionListener {

    val state: StateFlow<SetupUiState> =
        combine(
            preferences.loadedSettings,
            signIn.state,
            libraryRepository.hasPermission,
            ::stateOf,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(
                    preferences.loadedSettings.value,
                    signIn.state.value,
                    libraryRepository.hasPermission.value,
                ),
            )

    fun connect(draft: SourceDraft) {
        viewModelScope.launch { signIn.connect(draft) }
    }

    /** Ends setup, playing from the server signed in to when [keepServer], else this device. */
    fun finish(keepServer: Boolean) {
        viewModelScope.launch {
            preferences.update {
                val done = it.copy(setupDone = true)
                if (keepServer) done else done.copy(activeSource = null)
            }
        }
    }

    override fun onMusicAccess(granted: Boolean) = libraryRepository.onPermission(granted)

    override fun onFilesAccess() = libraryRepository.rescan()
}

private fun stateOf(settings: DmtSettings?, signIn: SignInState, hasPermission: Boolean) =
    SetupUiState(settings ?: DmtSettings(), signIn, hasPermission)
