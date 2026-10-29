package dev.jyotiraditya.dmt.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SourcesUiState(
    val settings: DmtSettings = DmtSettings(),
    /** The tracks of the source in use. */
    val tracks: List<Track> = emptyList(),
    val hasPermission: Boolean = false,
)

/** This device and the added servers, one of them in use. */
@HiltViewModel
class SourcesViewModel
@Inject
constructor(
    libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SourcesUiState> =
        combine(
            preferences.loadedSettings,
            libraryRepository.library,
            libraryRepository.hasPermission,
            ::stateOf,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(
                    preferences.loadedSettings.value,
                    libraryRepository.library.value,
                    libraryRepository.hasPermission.value,
                ),
            )

    /** Plays music from the source [id], or from this device when it is null. */
    fun use(id: String?) {
        viewModelScope.launch { preferences.update { it.copy(activeSource = id) } }
    }

    fun signOut(id: String) {
        viewModelScope.launch { preferences.update { it.signedOut(id) } }
    }
}

private fun stateOf(settings: DmtSettings?, library: LibrarySnapshot, hasPermission: Boolean) =
    SourcesUiState(settings ?: DmtSettings(), library.tracks, hasPermission)
