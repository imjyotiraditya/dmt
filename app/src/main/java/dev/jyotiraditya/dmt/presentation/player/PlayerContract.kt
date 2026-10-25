package dev.jyotiraditya.dmt.presentation.player

import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.AudioQuality
import dev.jyotiraditya.dmt.domain.model.Cover
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.DmtStats
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Genre
import dev.jyotiraditya.dmt.domain.model.HomeShelves
import dev.jyotiraditya.dmt.domain.model.LibraryScan
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.SourceDraft
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.util.QueueEntry
import dev.jyotiraditya.lyrics.Lyrics

/** The source form in view: a new source of [kind], or the saved source [sourceId]. */
data class SourceForm(val kind: SourceMode, val sourceId: String? = null) {
    /** The page the form leaves to: the list of kinds for a new source, Sources otherwise. */
    val backTo: DmtView
        get() = if (sourceId == null) DmtView.SOURCE_KINDS else DmtView.SOURCES
}

enum class DmtView {
    LIBRARY,
    ALBUMS,
    ARTISTS,
    GENRES,
    FOLDERS,
    PLAYLISTS,
    SETTINGS,
    LIBRARY_FOLDERS,
    LICENSES,
    SOURCES,
    SOURCE_KINDS,
    SOURCE_FORM,
    PERMISSIONS,
}

data class DmtState(
    val hasPermission: Boolean = false,
    val scanning: Boolean = false,
    /** The last library scan that finished, or null before any. */
    val lastScan: LibraryScan? = null,
    val library: LibrarySnapshot = LibrarySnapshot(),
    val query: String = "",
    val filtered: List<Track> = emptyList(),
    val filteredAlbums: List<Album> = emptyList(),
    val filteredArtists: List<Artist> = emptyList(),
    val filteredFolders: List<Folder> = emptyList(),
    val filteredGenres: List<Genre> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val view: DmtView = DmtView.LIBRARY,
    val sourceForm: SourceForm = SourceForm(SourceMode.JELLYFIN),
    /** Whether a source is signing in from its form. */
    val connecting: Boolean = false,
    /** Why the source on the form could not sign in. */
    val sourceError: UiText? = null,
    /** The [Album.id] of the open album. */
    val openAlbum: String? = null,
    val openArtist: String? = null,
    val openFolder: String? = null,
    val openGenre: String? = null,
    val openPlaylist: String? = null,
    val nowPlayingId: String? = null,
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val queue: List<QueueEntry> = emptyList(),
    /** The playing item's index in the player's item list. */
    val queueIndex: Int = 0,
    val animatedArtUrl: String? = null,
    /** The playing item's place in [queue]. */
    val queuePosition: Int = 0,
    val album: String = "",
    val cover: Cover = Cover.Loading,
    val lyrics: Lyrics? = null,
    /** The source picked in the lyrics sheet. */
    val lyricsSource: LyricsSource = LyricsSource.DEFAULT,
    /** What each asked source has; online sources not yet asked are absent. */
    val lyricsSources: Map<LyricsSource, LyricsSourceStatus> = emptyMap(),
    /** The source of the lyrics on screen, or null when none are. */
    val lyricsShowingFrom: LyricsSource? = null,
    val lyricsSourcesOpen: Boolean = false,
    val lyricsFetching: Boolean = false,
    /** When the sleep timer ends, in epoch milliseconds, or 0 when it is off. */
    val sleepEndAt: Long = 0L,
    /** Whether the running sleep timer lets the song playing at [sleepEndAt] finish. */
    val sleepFinishSong: Boolean = false,
    val settings: DmtSettings = DmtSettings(),
    val settingsLoaded: Boolean = false,
    val stats: DmtStats = DmtStats(),
    val home: HomeShelves = HomeShelves(),
    val tech: List<Spec> = emptyList(),
    val quality: AudioQuality? = null,
    /** Where the audio goes: the output device and its format. */
    val route: List<Spec> = emptyList(),
    /** Why the track being played cannot play. */
    val fault: UiText? = null,
    val error: UiText? = null,
    val notice: UiText? = null,
) {
    val tracks: List<Track>
        get() = library.tracks

    val albums: List<Album>
        get() = library.albums

    val artists: List<Artist>
        get() = library.artists

    val folders: List<Folder>
        get() = library.folders

    val genres: List<Genre>
        get() = library.genres
}

val DmtState.sleepOn: Boolean
    get() = sleepEndAt > 0L

/** A number fixed per track, for placeholders that stay the same for it. */
val DmtState.nowPlayingSeed: Long
    get() = nowPlayingId?.hashCode()?.toLong() ?: 0L

/** The library track being played, or null when nothing is or it is not in the library. */
val DmtState.nowPlayingTrack: Track?
    get() = library.track(nowPlayingId)

sealed interface DmtAction {
    data class Permission(val granted: Boolean) : DmtAction

    data object Rescan : DmtAction

    data class Query(val value: String) : DmtAction

    data class Show(val view: DmtView) : DmtAction

    /** Opens the album with this [Album.id], or closes the open one. */
    data class OpenAlbum(val id: String?) : DmtAction

    data class OpenArtist(val name: String?) : DmtAction

    data class OpenFolder(val path: String?) : DmtAction

    data class OpenGenre(val name: String?) : DmtAction

    data class OpenPlaylist(val name: String?) : DmtAction

    data class CreatePlaylist(val name: String) : DmtAction

    data class DeletePlaylist(val name: String) : DmtAction

    data class AddToPlaylist(val name: String, val track: Track) : DmtAction

    data class RemoveFromPlaylist(val name: String, val path: String) : DmtAction

    data class PlayAt(val list: List<Track>, val index: Int) : DmtAction

    /** Plays [list] with shuffle on, from a random track. */
    data class ShufflePlay(val list: List<Track>) : DmtAction

    data class Enqueue(val list: List<Track>, val label: String) : DmtAction

    /** Inserts [list] right after the playing track. */
    data class PlayNext(val list: List<Track>, val label: String) : DmtAction

    /** Moves the item at player index [from] to index [to]. */
    data class MoveInQueue(val from: Int, val to: Int) : DmtAction

    /** Removes everything queued after the playing track. */
    data object ClearUpcoming : DmtAction

    data class Jump(val index: Int) : DmtAction

    data class Seek(val fraction: Float) : DmtAction

    data class RemoveAt(val index: Int) : DmtAction

    data object OpenLyricsSources : DmtAction

    data object CloseLyricsSources : DmtAction

    data class SetLyricsSource(val source: LyricsSource) : DmtAction

    /** Starts the sleep timer for [minutes], or turns it off at 0. */
    data class SetSleep(val minutes: Int) : DmtAction

    /** Stops playback when the playing song ends. */
    data object SleepAtSongEnd : DmtAction

    data class SetSpeed(val speed: Float) : DmtAction

    data class Config(val settings: DmtSettings) : DmtAction

    /** Opens the form for a new source of [kind], or for the saved source [sourceId]. */
    data class OpenSourceForm(val kind: SourceMode, val sourceId: String? = null) : DmtAction

    data class SaveSource(val draft: SourceDraft) : DmtAction
}
