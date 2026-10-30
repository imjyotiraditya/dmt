package dev.jyotiraditya.dmt.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PlaylistRepository
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.presentation.common.TrackActions
import dev.jyotiraditya.dmt.presentation.common.TrackListActions
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The long-press menu of a single track: queue it, add it to a playlist, or go to its album. */
@HiltViewModel
class TrackMenuViewModel
@Inject
constructor(
    private val libraryRepository: LibraryRepository,
    private val playlistRepository: PlaylistRepository,
    actions: TrackActions,
) : ViewModel(),
    TrackListActions by actions {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists

    fun albumOf(track: Track): Album? = libraryRepository.library.value.albumOf(track)

    fun artistOf(track: Track): Artist? = libraryRepository.library.value.artistOf(track)

    fun playNext(track: Track) = playNext(listOf(track), track.title)

    fun enqueue(track: Track) = enqueue(listOf(track), track.title)

    fun addToPlaylist(name: String, track: Track) {
        viewModelScope.launch { playlistRepository.addTrack(name, track) }
    }
}
