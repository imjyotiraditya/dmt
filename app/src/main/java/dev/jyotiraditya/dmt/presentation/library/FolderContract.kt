package dev.jyotiraditya.dmt.presentation.library

import dev.jyotiraditya.dmt.domain.model.FolderNode
import dev.jyotiraditya.dmt.domain.model.flatten

/** How the folders *currently on screen* are ordered. Never affects song sorting. */
enum class FolderSort(val label: String) {
    ALPHABETICAL("a-z"),
    REVERSE_ALPHABETICAL("z-a"),
    RECENT_MODIFIED("recent-m"),
    OLDEST("oldest"),
    MOST_SONGS("most-trk"),
    ;

    val comparator: Comparator<FolderNode>
        get() = when (this) {
            ALPHABETICAL -> compareBy { it.name.lowercase() }
            REVERSE_ALPHABETICAL -> compareByDescending { it.name.lowercase() }
            RECENT_MODIFIED -> compareByDescending { it.lastModified }
            OLDEST -> compareBy { it.lastModified }
            MOST_SONGS -> compareByDescending { it.songCount }
        }

    fun next(): FolderSort = entries[(ordinal + 1) % entries.size]
}

/** UI state for [FolderViewModel]: what's on screen in the Folders tab right now. */
data class FolderUiState(
    val tree: List<FolderNode> = emptyList(),
    val currentPath: String? = null,
    val query: String = "",
    val sort: FolderSort = FolderSort.ALPHABETICAL,
) {
    val openNode: FolderNode? get() = FolderNavigation.open(tree, currentPath)
    val matches: List<FolderNode>
        get() = if (query.isBlank()) emptyList() else tree.flatten().matchingName(query)
}

sealed interface FolderAction {
    /** Opens [path], or the folder root when `null`. */
    data class Open(val path: String?) : FolderAction
    /** Navigates up one directory level from the currently open folder. */
    data object Back : FolderAction
    data class Query(val value: String) : FolderAction
    data class Sort(val value: FolderSort) : FolderAction
}

private fun List<FolderNode>.matchingName(query: String): List<FolderNode> =
    filter { it.name.contains(query, ignoreCase = true) }
