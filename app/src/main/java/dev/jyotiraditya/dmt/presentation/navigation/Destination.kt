package dev.jyotiraditya.dmt.presentation.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import kotlinx.serialization.Serializable

/** A screen of the app, with what it needs to show itself. */
@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Listen : Destination

    @Serializable data object Library : Destination

    /** The album with this id, holding only [artist]'s tracks on it when one is named. */
    @Serializable data class Album(val id: String, val artist: String? = null) : Destination

    @Serializable data class Artist(val name: String) : Destination

    @Serializable data class Genre(val name: String) : Destination

    @Serializable data class Folder(val path: String) : Destination

    @Serializable data class Playlist(val name: String) : Destination

    @Serializable data object Settings : Destination

    @Serializable data object Sources : Destination

    /** The kinds of server that can be added as a source. */
    @Serializable data object SourceKinds : Destination

    /** The form for a new source of [kind], or for the saved source [sourceId]. */
    @Serializable
    data class SourceForm(val kind: SourceMode, val sourceId: String? = null) : Destination

    @Serializable data object Permissions : Destination

    @Serializable data object LibraryFolders : Destination

    @Serializable data object Licenses : Destination
}

/** The top-level places, with their label, icon and the screen each opens on. */
enum class Place(
    @param:StringRes val label: Int,
    @param:DrawableRes val icon: Int,
    val root: Destination,
) {
    LISTEN(R.string.nav_listen, DmtIcons.Home, Destination.Listen),
    LIBRARY(R.string.nav_library, DmtIcons.Library, Destination.Library),
    SETTINGS(R.string.nav_settings, DmtIcons.Settings, Destination.Settings),
}

/** The shelves the library opens on, as tabs over its list. */
enum class Shelf(@param:StringRes val label: Int) {
    TRACKS(R.string.library_tab_tracks),
    ALBUMS(R.string.library_tab_albums),
    ARTISTS(R.string.library_tab_artists),
    PLAYLISTS(R.string.library_tab_playlists),
    GENRES(R.string.library_tab_genres),
    FOLDERS(R.string.library_tab_folders),
}
