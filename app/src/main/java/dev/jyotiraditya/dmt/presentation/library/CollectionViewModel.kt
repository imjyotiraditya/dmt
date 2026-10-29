package dev.jyotiraditya.dmt.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PlaybackRepository
import dev.jyotiraditya.dmt.data.repository.PlaylistRepository
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Genre
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.common.TrackActions
import dev.jyotiraditya.dmt.presentation.common.TrackListActions
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One collection of tracks opened from the library. */
sealed interface Collection {
    data class OfAlbum(val album: Album) : Collection

    data class OfArtist(val artist: Artist) : Collection

    data class OfGenre(val genre: Genre) : Collection

    data class OfFolder(val folder: Folder) : Collection

    data class OfPlaylist(val playlist: Playlist) : Collection
}

data class CollectionUiState(
    /** The collection, or null while the library does not hold it. */
    val collection: Collection? = null,
    /** Every track in the library, for picking what to add to a playlist. */
    val tracks: List<Track> = emptyList(),
    val nowPlayingId: String? = null,
)

/** The album, artist, genre, folder or playlist named by [destination]. */
@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel
@AssistedInject
constructor(
    @Assisted private val destination: Destination,
    libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    playback: PlaybackRepository,
    actions: TrackActions,
) : ViewModel(),
    TrackListActions by actions {

    @AssistedFactory
    interface Factory {
        fun create(destination: Destination): CollectionViewModel
    }

    val state: StateFlow<CollectionUiState> =
        combine(
            libraryRepository.library,
            playlistRepository.playlists,
            playback.nowPlaying.map { it.mediaId }.distinctUntilChanged(),
        ) { library, playlists, nowPlayingId ->
            CollectionUiState(
                collection = library.collectionOf(destination, playlists),
                tracks = library.tracks,
                nowPlayingId = nowPlayingId,
            )
        }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                libraryRepository.library.value.let { library ->
                    CollectionUiState(
                        collection =
                            library.collectionOf(destination, playlistRepository.playlists.value),
                        tracks = library.tracks,
                        nowPlayingId = playback.nowPlaying.value.mediaId,
                    )
                },
            )

    fun addToPlaylist(name: String, track: Track) {
        viewModelScope.launch { playlistRepository.addTrack(name, track) }
    }

    fun removeFromPlaylist(name: String, track: Track) {
        viewModelScope.launch { playlistRepository.removeTrack(name, track.path) }
    }
}

private fun LibrarySnapshot.collectionOf(
    destination: Destination,
    playlists: List<Playlist>,
): Collection? = when (destination) {
    is Destination.Album -> {
        val own =
            destination.artist
                ?.let { artist(it) }
                ?.discography
                ?.find { it.id == destination.id }
        (own ?: album(destination.id))?.let(Collection::OfAlbum)
    }

    is Destination.Artist -> artist(destination.name)?.let(Collection::OfArtist)

    is Destination.Genre -> genre(destination.name)?.let(Collection::OfGenre)

    is Destination.Folder -> folder(destination.path)?.let(Collection::OfFolder)

    is Destination.Playlist ->
        playlists.find { it.name == destination.name }?.let(Collection::OfPlaylist)

    else -> null
}
