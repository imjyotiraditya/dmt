package dev.jyotiraditya.dmt.presentation.player

import android.content.IntentSender
import android.graphics.Bitmap
import androidx.media3.common.Player
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.DmtStats
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.Lyrics
import dev.jyotiraditya.dmt.domain.model.Playlist
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track

enum class DmtView {
    LIBRARY, ALBUMS, ARTISTS, FOLDERS, PLAYLISTS, SETTINGS, STATS, BLOCKLIST, SOURCES, SOURCE_LOGIN
}

data class DmtState(
    val hasPermission: Boolean = false,
    val scanning: Boolean = false,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val query: String = "",
    val filtered: List<Track> = emptyList(),
    val filteredAlbums: List<Album> = emptyList(),
    val filteredArtists: List<Artist> = emptyList(),
    val filteredFolders: List<Folder> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val view: DmtView = DmtView.LIBRARY,
    val loginSource: SourceMode = SourceMode.JELLYFIN,
    val openAlbum: String? = null,
    val openArtist: String? = null,
    val openFolder: String? = null,
    val openPlaylist: String? = null,
    val nowPlayingId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<String> = emptyList(),
    val queueIndex: Int = 0,
    val album: String = "",
    val cover: Bitmap? = null,
    val artRaw: Bitmap? = null,
    val lyrics: Lyrics? = null,
    val lyricsFetching: Boolean = false,
    val expanded: Boolean = false,
    val sleepMinutes: Int = 0,
    val sleepLeftMs: Long = 0L,
    val speed: Float = 1f,
    val settings: DmtSettings = DmtSettings(),
    val stats: DmtStats = DmtStats(),
    val tech: List<Spec> = emptyList(),
    val route: List<Spec> = emptyList(),
    val error: String? = null,
    val notice: String? = null,
)

sealed interface DmtAction {
    data class Permission(val granted: Boolean) : DmtAction
    data object Rescan : DmtAction
    data class Query(val value: String) : DmtAction
    data class Show(val view: DmtView) : DmtAction
    data class OpenAlbum(val name: String?) : DmtAction
    data class OpenArtist(val name: String?) : DmtAction
    data class OpenFolder(val path: String?) : DmtAction
    data class OpenPlaylist(val name: String?) : DmtAction
    data class CreatePlaylist(val name: String) : DmtAction
    data class DeletePlaylist(val name: String) : DmtAction
    data class AddToPlaylist(val name: String, val track: Track) : DmtAction
    data class RemoveFromPlaylist(val name: String, val path: String) : DmtAction
    data class PlayAt(val list: List<Track>, val index: Int) : DmtAction
    data class Enqueue(val list: List<Track>, val label: String) : DmtAction
    data class Jump(val index: Int) : DmtAction
    data object TogglePlay : DmtAction
    data object Next : DmtAction
    data object Prev : DmtAction
    data object ToggleShuffle : DmtAction
    data object CycleRepeat : DmtAction
    data class Seek(val fraction: Float) : DmtAction
    data class Expand(val value: Boolean) : DmtAction
    data class RemoveAt(val index: Int) : DmtAction
    data object FetchLyrics : DmtAction
    data class EmbedLyrics(val granted: Boolean) : DmtAction
    data object CycleSleep : DmtAction
    data object CycleSpeed : DmtAction
    data object OpenEqualizer : DmtAction
    data object NoEqualizer : DmtAction
    data class Config(val settings: DmtSettings) : DmtAction
    data class ShowLogin(val mode: SourceMode) : DmtAction
    data class SourceLogin(
        val mode: SourceMode,
        val url: String,
        val username: String,
        val password: String,
    ) : DmtAction
}

sealed interface PlayerEffect {
    data class OpenEqualizer(val audioSessionId: Int) : PlayerEffect
    data class RequestWrite(val intentSender: IntentSender) : PlayerEffect
}
