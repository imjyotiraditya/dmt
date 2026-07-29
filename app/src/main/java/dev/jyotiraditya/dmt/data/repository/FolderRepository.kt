package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.domain.model.FolderNode
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.toFolderTree
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the folder-tree view of the indexed music library.
 *
 * This never touches storage directly and never scans anything itself — it
 * only derives and caches a hierarchy from the track list the app already
 * indexed (see [dev.jyotiraditya.dmt.domain.usecase.ScanLibraryUseCase]), and
 * exposes it as a [StateFlow] so observers refresh automatically whenever the
 * library is rescanned. Contains no playback logic and no UI logic.
 */
@Singleton
class FolderRepository @Inject constructor() {

    private val _tree = MutableStateFlow<List<FolderNode>>(emptyList())
    val tree: StateFlow<List<FolderNode>> = _tree.asStateFlow()

    /** Rebuilds the cached tree from the latest indexed tracks. O(n) over [tracks]. */
    fun refresh(tracks: List<Track>) {
        _tree.value = tracks.toFolderTree()
    }
}
