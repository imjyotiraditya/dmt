package dev.jyotiraditya.dmt.presentation.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.core.base.BaseViewModel
import dev.jyotiraditya.dmt.data.repository.FolderRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

private const val KEY_PATH = "folder_current_path"
private const val KEY_QUERY = "folder_query"
private const val KEY_SORT = "folder_sort"

/**
 * Drives folder-tree browsing (open/back/search/sort). Playback itself is
 * intentionally left to the app's single [dev.jyotiraditya.dmt.presentation.player.PlayerViewModel]
 * and its already-connected MediaController — this ViewModel never touches
 * playback or the queue, only what's currently visible in the Folders tab.
 *
 * [currentPath], [query] and [sort] are mirrored into [savedStateHandle] so
 * they survive process death, same as the rest of the app's state restoration.
 */
@HiltViewModel
class FolderViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<FolderAction, FolderUiState, Nothing>(
    initialState = FolderUiState(
        currentPath = savedStateHandle[KEY_PATH],
        query = savedStateHandle[KEY_QUERY] ?: "",
        sort = savedStateHandle.get<String>(KEY_SORT)
            ?.let { runCatching { FolderSort.valueOf(it) }.getOrNull() }
            ?: FolderSort.ALPHABETICAL,
    ),
) {

    init {
        folderRepository.tree
            .onEach { tree -> reduce { it.copy(tree = tree) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: FolderAction) {
        when (intent) {
            is FolderAction.Open -> openPath(intent.path)
            is FolderAction.Back -> {
                val up = FolderNavigation.up(currentState.tree, currentState.currentPath)
                openPath(up)
            }
            is FolderAction.Query -> {
                savedStateHandle[KEY_QUERY] = intent.value
                reduce { it.copy(query = intent.value) }
            }
            is FolderAction.Sort -> {
                savedStateHandle[KEY_SORT] = intent.value.name
                reduce { it.copy(sort = intent.value) }
            }
        }
    }

    private fun openPath(path: String?) {
        savedStateHandle[KEY_PATH] = path
        reduce { it.copy(currentPath = path) }
    }
}
