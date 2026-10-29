package dev.jyotiraditya.dmt.presentation.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.presentation.navigation.Shelf
import dev.jyotiraditya.dmt.ui.components.ScrollMemory

/** The shared-element key of an album's cover, used by its tiles and its page header. */
internal fun albumCoverKey(id: String): String = "cover:album:$id"

/** Track menu navigation that opens albums and artists on top of the screen shown. */
internal fun trackMenuNavigation(onOpen: (Destination) -> Unit) = TrackMenuNavigation(
    openAlbum = { onOpen(Destination.Album(it)) },
    openArtist = { onOpen(Destination.Artist(it)) },
)

/** The library's first screen: the search box and shelf tabs over the shelf or search results. */
@Composable
fun LibraryScreen(
    shelf: Shelf,
    onShelf: (Shelf) -> Unit,
    onOpen: (Destination) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val searching = state.results.searching
    BackHandler(enabled = searching) { viewModel.clearSearch() }
    Column {
        LibraryHead(
            state = state,
            search = viewModel.search,
            shelf = shelf,
            onShelf = onShelf,
        )
        ScrollMemory("shelf:${shelf.name}:$searching") {
            Shelf(shelf = shelf, state = state, viewModel = viewModel, onOpen = onOpen)
        }
    }
}

/** An album, artist, genre, folder or playlist opened from the library. */
@Composable
fun CollectionScreen(
    destination: Destination,
    onOpen: (Destination) -> Unit,
    viewModel: CollectionViewModel =
        hiltViewModel<CollectionViewModel, CollectionViewModel.Factory>(
            creationCallback = { it.create(destination) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigation = trackMenuNavigation(onOpen)
    when (val collection = state.collection) {
        is Collection.OfAlbum ->
            with(collection.album) {
                TracksDetail(
                    title = name,
                    meta = artist,
                    tracks = tracks,
                    numbered = true,
                    nowPlayingId = state.nowPlayingId,
                    actions = viewModel,
                    navigation = navigation,
                    coverKey = albumCoverKey(id),
                )
            }

        is Collection.OfArtist ->
            ArtistDetail(
                artist = collection.artist,
                actions = viewModel,
                onOpenAlbum = { onOpen(Destination.Album(it, artist = collection.artist.name)) },
            )

        is Collection.OfGenre ->
            with(collection.genre) {
                TracksDetail(
                    title = name,
                    meta = stringResource(R.string.collection_genre),
                    tracks = tracks,
                    numbered = false,
                    nowPlayingId = state.nowPlayingId,
                    actions = viewModel,
                    navigation = navigation,
                )
            }

        is Collection.OfFolder ->
            with(collection.folder) {
                TracksDetail(
                    title = folderTitle(this),
                    meta = location,
                    tracks = tracks,
                    numbered = false,
                    nowPlayingId = state.nowPlayingId,
                    actions = viewModel,
                    navigation = navigation,
                )
            }

        is Collection.OfPlaylist ->
            PlaylistDetail(
                playlist = collection.playlist,
                state = state,
                viewModel = viewModel,
            )

        null -> Unit
    }
}
