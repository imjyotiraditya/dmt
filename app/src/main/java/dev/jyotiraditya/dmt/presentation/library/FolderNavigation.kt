package dev.jyotiraditya.dmt.presentation.library

import dev.jyotiraditya.dmt.domain.model.FolderNode
import dev.jyotiraditya.dmt.domain.model.findNode

/**
 * Navigation rules for browsing a [FolderNode] tree one directory at a time.
 *
 * Kept separate from [FolderViewModel] and [dev.jyotiraditya.dmt.presentation.main.DmtScreen]
 * so both the in-screen "up" action and the system back button drive the exact
 * same logic instead of two copies of it.
 */
object FolderNavigation {

    /**
     * The path to open when navigating up one level from [currentPath].
     * Returns `null` when already at (or navigating up from) the folder root.
     */
    fun up(tree: List<FolderNode>, currentPath: String?): String? =
        currentPath?.let { tree.findNode(it)?.parentPath }

    /** The node currently open, or `null` when browsing the folder root. */
    fun open(tree: List<FolderNode>, currentPath: String?): FolderNode? =
        currentPath?.let { tree.findNode(it) }
}
