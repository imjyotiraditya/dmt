package dev.jyotiraditya.dmt.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LibraryScan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: DmtSettings = DmtSettings(),
    val scanning: Boolean = false,
    /** The last library scan that finished, or null before any. */
    val lastScan: LibraryScan? = null,
)

@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(
            preferences.loadedSettings,
            libraryRepository.scanning,
            libraryRepository.lastScan,
            ::stateOf,
        )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(
                    preferences.loadedSettings.value,
                    libraryRepository.scanning.value,
                    libraryRepository.lastScan.value,
                ),
            )

    fun update(transform: (DmtSettings) -> DmtSettings) {
        viewModelScope.launch { preferences.update(transform) }
    }

    fun rescan() = libraryRepository.rescan()
}

private fun stateOf(settings: DmtSettings?, scanning: Boolean, lastScan: LibraryScan?) =
    SettingsUiState(settings ?: DmtSettings(), scanning, lastScan)
