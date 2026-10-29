package dev.jyotiraditya.dmt.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryFoldersUiState(
    val folders: List<Folder> = emptyList(),
    /** The paths of the folders whose tracks the library leaves out. */
    val hidden: Set<String> = emptySet(),
    /** Each folder's track count as the page opened, so a folder hidden since keeps its count. */
    val counts: Map<String, Int> = emptyMap(),
)

/** Which folders' tracks appear in the library. */
@HiltViewModel
class LibraryFoldersViewModel
@Inject
constructor(
    libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    private val counts =
        libraryRepository.library.value.folders.associate { it.path to it.tracks.size }

    val state: StateFlow<LibraryFoldersUiState> =
        combine(libraryRepository.library, preferences.loadedSettings, ::stateOf)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(libraryRepository.library.value, preferences.loadedSettings.value),
            )

    private fun stateOf(library: LibrarySnapshot, settings: DmtSettings?) =
        LibraryFoldersUiState(library.folders, settings?.hiddenFolders.orEmpty(), counts)

    fun setShown(path: String, shown: Boolean) {
        viewModelScope.launch {
            preferences.update {
                val hidden = if (shown) it.hiddenFolders - path else it.hiddenFolders + path
                it.copy(hiddenFolders = hidden)
            }
        }
    }
}
