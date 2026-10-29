package dev.jyotiraditya.dmt.presentation.library

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.presentation.navigation.Shelf
import dev.jyotiraditya.dmt.presentation.nowplaying.rememberWantsPlayback
import dev.jyotiraditya.dmt.presentation.settings.noTracksMessage
import dev.jyotiraditya.dmt.ui.components.ChoiceRow
import dev.jyotiraditya.dmt.ui.components.CollectionRow
import dev.jyotiraditya.dmt.ui.components.CoverTile
import dev.jyotiraditya.dmt.ui.components.DmtSheet
import dev.jyotiraditya.dmt.ui.components.EmptyState
import dev.jyotiraditya.dmt.ui.components.RowPlaceholders
import dev.jyotiraditya.dmt.ui.components.SearchBox
import dev.jyotiraditya.dmt.ui.components.SheetHeader
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.components.TrackRow
import dev.jyotiraditya.dmt.ui.components.WordTabs
import dev.jyotiraditya.dmt.ui.motion.sharedElement
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted

private val SearchTileWidth = 140.dp

/** The search field and shelf tabs pinned above the library. */
@Composable
internal fun LibraryHead(
    state: LibraryUiState,
    search: TextFieldState,
    shelf: Shelf,
    onShelf: (Shelf) -> Unit,
    modifier: Modifier = Modifier,
) {
    val library = state.library
    val shelves =
        Shelf.entries.filter {
            when (it) {
                Shelf.GENRES -> library.genres.isNotEmpty()

                Shelf.FOLDERS,
                Shelf.PLAYLISTS,
                -> library.folders.isNotEmpty()

                else -> true
            }
        }
    Column(modifier = modifier.fillMaxWidth().background(Ground).padding(top = 8.dp)) {
        SearchBox(
            state = search,
            hint = stringResource(R.string.search_hint),
            modifier = Modifier.padding(horizontal = Margin),
        )
        if (search.text.isBlank()) {
            WordTabs(
                labels = shelves.map { stringResource(it.label) },
                selected = shelves.indexOf(shelf).coerceAtLeast(0),
                onSelect = { onShelf(shelves[it]) },
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
internal fun Shelf(
    shelf: Shelf,
    state: LibraryUiState,
    viewModel: LibraryViewModel,
    onOpen: (Destination) -> Unit,
) {
    val bottom = PaddingValues(bottom = LocalContentBottom.current)
    val results = state.results
    val openTrackMenu = rememberTrackMenu(trackMenuNavigation(onOpen))
    val playing = rememberWantsPlayback()
    var actions by remember { mutableStateOf<Pair<String, List<Track>>?>(null) }
    actions?.let { (title, tracks) ->
        ActionSheet(
            title = title,
            tracks = tracks,
            actions = viewModel,
            onDismiss = { actions = null },
        )
    }

    if (results.searching) {
        LazyColumn(contentPadding = bottom) {
            results(state, playing, viewModel, onOpen, openTrackMenu)
        }
        return
    }

    if (shelf == Shelf.ALBUMS && state.library.albums.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding =
                PaddingValues(
                    start = Margin,
                    end = Margin,
                    top = 8.dp,
                    bottom = LocalContentBottom.current,
                ),
        ) {
            items(results.albums, key = { it.id }) { album ->
                CoverTile(
                    cover = album.tracks.firstOrNull(),
                    title = album.name,
                    subtitle = album.artist,
                    titleLines = 2,
                    onClick = { onOpen(Destination.Album(album.id)) },
                    onLongClick = { actions = album.name to album.tracks },
                    coverModifier = Modifier.sharedElement(albumCoverKey(album.id)),
                )
            }
        }
        return
    }

    LazyColumn(contentPadding = bottom) {
        when (shelf) {
            Shelf.ARTISTS ->
                items(results.artists, key = { it.name }) { artist ->
                    CollectionRow(
                        title = artist.name,
                        detail = artistSummary(artist),
                        onClick = { onOpen(Destination.Artist(artist.name)) },
                        onLongClick = { actions = artist.name to artist.tracks },
                        modifier = Modifier.animateItem(),
                    )
                }

            Shelf.GENRES ->
                items(results.genres, key = { it.name }) { genre ->
                    CollectionRow(
                        title = genre.name,
                        detail = tracksSummary(genre.tracks),
                        onClick = { onOpen(Destination.Genre(genre.name)) },
                        onLongClick = { actions = genre.name to genre.tracks },
                        modifier = Modifier.animateItem(),
                    )
                }

            Shelf.FOLDERS ->
                items(results.folders, key = { it.path }) { folder ->
                    val title = folderTitle(folder)
                    CollectionRow(
                        title = title,
                        detail = folderSummary(folder),
                        onClick = { onOpen(Destination.Folder(folder.path)) },
                        onLongClick = { actions = title to folder.tracks },
                        modifier = Modifier.animateItem(),
                    )
                }

            Shelf.PLAYLISTS -> playlists(state.playlists, viewModel, onOpen)

            Shelf.TRACKS,
            Shelf.ALBUMS,
            -> tracks(state, playing, viewModel, openTrackMenu)
        }
    }
}

private fun LazyListScope.tracks(
    state: LibraryUiState,
    playing: Boolean,
    viewModel: LibraryViewModel,
    openTrackMenu: (Track) -> Unit,
) {
    val tracks = state.library.tracks
    if (state.scanning && tracks.isEmpty()) {
        item {
            RowPlaceholders(text = stringResource(R.string.library_scanning))
        }
        return
    }
    if (tracks.isEmpty()) {
        item {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                body = stringResource(state.sourceMode.noTracksMessage),
                modifier = Modifier.padding(horizontal = Margin),
            )
        }
        return
    }
    item(key = "summary") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Margin),
        ) {
            Text(
                text = tracksSummary(tracks),
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
                modifier = Modifier.weight(1f),
            )
            var sorting by remember { mutableStateOf(false) }
            TextLink(
                label = stringResource(R.string.sort_by, stringResource(state.sort.labelRes())),
                onClick = { sorting = true },
            )
            if (sorting) {
                SortSheet(
                    selected = state.sort,
                    options = LibrarySort.options(state.sourceMode),
                    onPick = viewModel::sortBy,
                    onDismiss = { sorting = false },
                )
            }
        }
    }
    val shown = state.results.tracks
    itemsIndexed(shown, key = { _, track -> track.id }) { index, track ->
        TrackRow(
            track = track,
            meta = trackSummary(track),
            current = track.id.toString() == state.nowPlayingId,
            playing = playing,
            onClick = { viewModel.play(shown, index) },
            onLongClick = { openTrackMenu(track) },
            modifier = Modifier.animateItem(),
        )
    }
}

/** Search results: matching artists, albums, then tracks. */
private fun LazyListScope.results(
    state: LibraryUiState,
    playing: Boolean,
    viewModel: LibraryViewModel,
    onOpen: (Destination) -> Unit,
    openTrackMenu: (Track) -> Unit,
) {
    val results = state.results
    val artists = results.artists.take(4)
    val albums = results.albums.take(10)
    if (artists.isEmpty() && albums.isEmpty() && results.tracks.isEmpty()) {
        item {
            EmptyState(
                title = stringResource(R.string.search_no_match_title),
                body = stringResource(R.string.search_no_match_body, results.query),
                modifier = Modifier.padding(horizontal = Margin),
            )
        }
        return
    }
    items(artists, key = { "r:${it.name}" }) { artist ->
        CollectionRow(
            title = artist.name,
            detail = artistSummary(artist),
            onClick = { onOpen(Destination.Artist(artist.name)) },
        )
    }
    if (albums.isNotEmpty()) {
        item(key = "albums") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = Margin, vertical = 16.dp),
            ) {
                items(albums, key = { "a:${it.id}" }) { album ->
                    CoverTile(
                        cover = album.tracks.firstOrNull(),
                        title = album.name,
                        onClick = { onOpen(Destination.Album(album.id)) },
                        modifier = Modifier.width(SearchTileWidth),
                        coverModifier = Modifier.sharedElement(albumCoverKey(album.id)),
                    )
                }
            }
        }
    }
    itemsIndexed(results.tracks, key = { _, track -> track.id }) { index, track ->
        TrackRow(
            track = track,
            meta = trackSummary(track),
            current = track.id.toString() == state.nowPlayingId,
            playing = playing,
            onClick = { viewModel.play(results.tracks, index) },
            onLongClick = { openTrackMenu(track) },
        )
    }
}

@StringRes
private fun LibrarySort.labelRes(): Int = when (this) {
    LibrarySort.TITLE -> R.string.sort_title
    LibrarySort.ARTIST -> R.string.sort_artist
    LibrarySort.RECENT_ADDED -> R.string.sort_date_added
    LibrarySort.RECENT_MODIFIED -> R.string.sort_date_modified
    LibrarySort.RECENT -> R.string.sort_recent
}

/** Picks the order tracks are listed in, closing once picked. */
@Composable
private fun SortSheet(
    selected: LibrarySort,
    options: List<LibrarySort>,
    onPick: (LibrarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    DmtSheet(onDismiss = onDismiss) {
        SheetHeader(title = stringResource(R.string.sort_sheet_title))
        Column(modifier = Modifier.selectableGroup()) {
            options.forEach { option ->
                ChoiceRow(
                    label = stringResource(option.labelRes()),
                    selected = option == selected,
                    onClick = {
                        onPick(option)
                        dismiss()
                    },
                )
            }
        }
    }
}
