package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.ui.components.ScrollMemory
import dev.jyotiraditya.dmt.ui.motion.Depth

/** The library: a shelf, or the collection opened from it, sliding by depth between them. */
@Composable
fun LibraryPage(state: DmtState, dispatch: (DmtAction) -> Unit) {
    Depth(
        target = state.libraryScreen(),
        keyOf = { it.key },
        depthOf = { it.depth },
    ) { screen ->
        LibraryContent(screen, state, dispatch)
    }
}

@Composable
private fun LibraryContent(screen: LibraryScreen, state: DmtState, dispatch: (DmtAction) -> Unit) {
    when (screen) {
        is LibraryScreen.OfArtistAlbum -> AlbumDetail(screen.album, state, dispatch)

        is LibraryScreen.OfArtist -> ArtistDetail(screen.artist, state, dispatch)

        is LibraryScreen.OfAlbum -> AlbumDetail(screen.album, state, dispatch)

        is LibraryScreen.OfGenre ->
            with(screen.genre) {
                TracksDetail(
                    name,
                    stringResource(R.string.collection_genre),
                    tracks,
                    numbered = false,
                    state,
                    dispatch,
                )
            }

        is LibraryScreen.OfFolder ->
            with(screen.folder) {
                TracksDetail(
                    folderTitle(this),
                    location,
                    tracks,
                    numbered = false,
                    state,
                    dispatch,
                )
            }

        is LibraryScreen.OfPlaylist -> PlaylistDetail(screen.playlist, state, dispatch)

        is LibraryScreen.Shelf ->
            Column {
                LibraryHead(state, screen.view, dispatch)
                ScrollMemory("shelf:${screen.view.name}:${screen.searching}") {
                    Shelf(
                        screen.view,
                        state,
                        dispatch,
                    )
                }
            }
    }
}

@Composable
private fun AlbumDetail(album: Album, state: DmtState, dispatch: (DmtAction) -> Unit) {
    TracksDetail(
        album.name,
        album.artist,
        album.tracks,
        numbered = true,
        state,
        dispatch,
        coverKey = albumCoverKey(album.id),
    )
}
