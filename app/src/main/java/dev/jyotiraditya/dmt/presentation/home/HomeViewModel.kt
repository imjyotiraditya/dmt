package dev.jyotiraditya.dmt.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PlaybackRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.DmtStats
import dev.jyotiraditya.dmt.domain.model.HomeShelves
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.homeShelves
import dev.jyotiraditya.dmt.presentation.common.TrackActions
import dev.jyotiraditya.dmt.presentation.common.TrackListActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SHELF_SIZE = 12

data class HomeUiState(
    val scanning: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val shelves: HomeShelves = HomeShelves(),
    /** The most played albums, or the newest until any have been played. */
    val onRepeat: List<Album> = emptyList(),
    /** Whether [onRepeat] holds played albums rather than the newest. */
    val played: Boolean = false,
    val stats: DmtStats = DmtStats(),
    val sourceMode: SourceMode = SourceMode.LOCAL,
    val nowPlayingId: String? = null,
)

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    libraryRepository: LibraryRepository,
    preferences: PreferencesRepository,
    playback: PlaybackRepository,
    actions: TrackActions,
) : ViewModel(),
    TrackListActions by actions {

    val state: StateFlow<HomeUiState> =
        combine(
            libraryRepository.library,
            libraryRepository.scanning,
            preferences.stats,
            preferences.loadedSettings.map { it?.sourceMode }.distinctUntilChanged(),
            playback.nowPlaying.map { it.mediaId }.distinctUntilChanged(),
            ::stateOf,
        )
            .flowOn(Dispatchers.Default)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                stateOf(
                    libraryRepository.library.value,
                    libraryRepository.scanning.value,
                    preferences.stats.value,
                    preferences.loadedSettings.value?.sourceMode,
                    playback.nowPlaying.value.mediaId,
                ),
            )

    fun shuffleAll() = shuffle(state.value.tracks)
}

private fun stateOf(
    library: LibrarySnapshot,
    scanning: Boolean,
    stats: DmtStats,
    sourceMode: SourceMode?,
    nowPlayingId: String?,
): HomeUiState {
    val shelves = homeShelves(library.tracks, library.albums, library.artists, stats.counts)
    return HomeUiState(
        scanning = scanning,
        tracks = library.tracks,
        shelves = shelves,
        onRepeat =
            shelves.albums.ifEmpty {
                library.albums
                    .sortedByDescending { album -> album.tracks.maxOf { it.dateAdded } }
                    .take(SHELF_SIZE)
            },
        played = shelves.albums.isNotEmpty(),
        stats = stats,
        sourceMode = sourceMode ?: SourceMode.LOCAL,
        nowPlayingId = nowPlayingId,
    )
}
