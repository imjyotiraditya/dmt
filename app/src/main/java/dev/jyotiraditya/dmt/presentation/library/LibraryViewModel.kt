package dev.jyotiraditya.dmt.presentation.library

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PlaybackRepository
import dev.jyotiraditya.dmt.data.repository.PlaylistRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.common.TrackActions
import dev.jyotiraditya.dmt.presentation.common.TrackListActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How long typing pauses before the library is searched. */
private val SEARCH_SETTLE = 150.milliseconds

data class LibraryUiState(
    val scanning: Boolean = false,
    val library: LibrarySnapshot = LibrarySnapshot(),
    /** The library narrowed to the search, with the query it was made for. */
    val results: FilteredLibrary = FilteredLibrary(),
    val playlists: List<Playlist> = emptyList(),
    val sort: LibrarySort = LibrarySort.TITLE,
    val sourceMode: SourceMode = SourceMode.LOCAL,
    val nowPlayingId: String? = null,
)

/** The library's shelves and search. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel
@Inject
constructor(
    libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val preferences: PreferencesRepository,
    playback: PlaybackRepository,
    actions: TrackActions,
) : ViewModel(),
    TrackListActions by actions {

    /** The text typed into the search box. */
    val search = TextFieldState()

    private val settings = preferences.loadedSettings.map { it ?: DmtSettings() }

    private val results =
        combine(
            libraryRepository.library,
            snapshotFlow { search.text.toString() }
                .debounce { if (it.isEmpty()) Duration.ZERO else SEARCH_SETTLE },
            settings.map { it.librarySort }.distinctUntilChanged(),
        ) { library, query, sort ->
            Triple(library, query, sort)
        }
            .mapLatest { (library, query, sort) -> library.filtered(query, sort) }
            .flowOn(Dispatchers.Default)

    val state: StateFlow<LibraryUiState> =
        combine(
            libraryRepository.library,
            libraryRepository.scanning,
            results,
            playlistRepository.playlists,
            settings,
        ) { library, scanning, results, playlists, settings ->
            LibraryUiState(
                scanning = scanning,
                library = library,
                results = results,
                playlists = playlists,
                sort = settings.librarySort,
                sourceMode = settings.sourceMode,
            )
        }
            .combine(playback.nowPlaying.map { it.mediaId }.distinctUntilChanged()) { state, id ->
                state.copy(nowPlayingId = id)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                run {
                    val library = libraryRepository.library.value
                    val settings = preferences.loadedSettings.value ?: DmtSettings()
                    LibraryUiState(
                        scanning = libraryRepository.scanning.value,
                        library = library,
                        results = library.filtered(query = "", sort = settings.librarySort),
                        playlists = playlistRepository.playlists.value,
                        sort = settings.librarySort,
                        sourceMode = settings.sourceMode,
                        nowPlayingId = playback.nowPlaying.value.mediaId,
                    )
                },
            )

    fun clearSearch() = search.clearText()

    fun sortBy(sort: LibrarySort) {
        viewModelScope.launch { preferences.update { it.copy(librarySort = sort) } }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { playlistRepository.create(name) }
    }

    fun deletePlaylist(name: String) {
        viewModelScope.launch { playlistRepository.delete(name) }
    }
}
