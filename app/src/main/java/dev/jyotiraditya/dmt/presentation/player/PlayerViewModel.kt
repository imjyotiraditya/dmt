package dev.jyotiraditya.dmt.presentation.player

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.base.BaseViewModel
import dev.jyotiraditya.dmt.core.common.generateAsciiPlaceholder
import dev.jyotiraditya.dmt.core.common.toAsciiBitmap
import dev.jyotiraditya.dmt.domain.model.Album
import dev.jyotiraditya.dmt.domain.model.Artist
import dev.jyotiraditya.dmt.domain.model.Folder
import dev.jyotiraditya.dmt.domain.model.LibrarySort
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.PlaylistRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import dev.jyotiraditya.dmt.domain.repository.StatsRepository
import dev.jyotiraditya.dmt.domain.usecase.EmbedLyricsUseCase
import dev.jyotiraditya.dmt.domain.usecase.GetCoverArtUseCase
import dev.jyotiraditya.dmt.domain.usecase.GetLyricsUseCase
import dev.jyotiraditya.dmt.domain.usecase.GetRouteSpecsUseCase
import dev.jyotiraditya.dmt.domain.usecase.GetTrackTechUseCase
import dev.jyotiraditya.dmt.domain.usecase.JellyfinLoginUseCase
import dev.jyotiraditya.dmt.domain.usecase.ScanLibraryUseCase
import dev.jyotiraditya.dmt.playback.PlaybackService
import dev.jyotiraditya.dmt.util.DispatcherProvider
import dev.jyotiraditya.dmt.util.QUEUE_CAP
import dev.jyotiraditya.dmt.util.audioPermission
import dev.jyotiraditya.dmt.util.await
import dev.jyotiraditya.dmt.util.cycleRepeat
import dev.jyotiraditya.dmt.util.mediaController
import dev.jyotiraditya.dmt.util.queueLabels
import dev.jyotiraditya.dmt.util.toMediaItem
import dev.jyotiraditya.dmt.util.togglePlayPause
import dev.jyotiraditya.dmt.util.windowQueue
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val SPEED_STEPS = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
private val SLEEP_STEPS = listOf(0, 15, 30, 60)

private data class FilteredLibrary(
    val tracks: List<Track>,
    val albums: List<Album>,
    val artists: List<Artist>,
    val folders: List<Folder>,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val statsRepository: StatsRepository,
    private val scanLibrary: ScanLibraryUseCase,
    private val jellyfinLogin: JellyfinLoginUseCase,
    private val getLyrics: GetLyricsUseCase,
    private val embedLyrics: EmbedLyricsUseCase,
    private val getCoverArt: GetCoverArtUseCase,
    private val getTrackTech: GetTrackTechUseCase,
    private val getRouteSpecs: GetRouteSpecsUseCase,
    private val playlistRepository: PlaylistRepository,
    private val dispatchers: DispatcherProvider,
) : BaseViewModel<DmtAction, DmtState, PlayerEffect>(
    DmtState(
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            audioPermission,
        ) == PackageManager.PERMISSION_GRANTED,
    ),
) {

    private var controller: MediaController? = null
    private var pendingEmbed: Pair<Track, String>? = null
    private var noticeJob: Job? = null
    private var sleepEndAt: Long? = null
    private var sessionRestored = false

    init {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            reduce { it.copy(settings = settings) }
        }
        viewModelScope.launch {
            statsRepository.stats.collect { stats ->
                reduce { if (it.stats == stats) it else it.copy(stats = stats) }
            }
        }
        viewModelScope.launch {
            getRouteSpecs().collect { route ->
                reduce { if (it.route == route) it else it.copy(route = route) }
            }
        }
        if (currentState.hasPermission) scan()
        connect()
    }

    private fun filter(tracks: List<Track>, query: String, sort: LibrarySort): List<Track> =
        if (query.isBlank()) {
            tracks
        } else {
            tracks.filter {
                it.title.contains(query, true) ||
                        it.artist.contains(query, true) ||
                        it.album.contains(query, true)
            }
        }.sortedWith(sort.comparator)

    private fun filterAlbums(albums: List<Album>, query: String): List<Album> =
        if (query.isBlank()) {
            albums
        } else {
            albums.filter {
                it.name.contains(query, true) || it.artist.contains(query, true)
            }
        }

    private fun filterArtists(artists: List<Artist>, query: String): List<Artist> =
        if (query.isBlank()) {
            artists
        } else {
            artists.filter { it.name.contains(query, true) }
        }

    private fun mutatePlaylists(block: () -> Unit = {}) {
        viewModelScope.launch(dispatchers.io) {
            block()
            val playlists = playlistRepository.load(currentState.tracks)
            reduce { it.copy(playlists = playlists) }
        }
    }

    private fun filterFolders(folders: List<Folder>, query: String): List<Folder> =
        if (query.isBlank()) {
            folders
        } else {
            folders.filter { it.name.contains(query, true) }
        }

    override fun onIntent(intent: DmtAction) {
        val c = controller
        when (intent) {
            is DmtAction.Permission -> {
                reduce { it.copy(hasPermission = intent.granted) }
                if (intent.granted) scan()
            }

            DmtAction.Rescan -> scan()
            is DmtAction.Query -> reduce {
                it.copy(
                    query = intent.value,
                    filtered = filter(it.tracks, intent.value, it.settings.librarySort),
                    filteredAlbums = filterAlbums(it.albums, intent.value),
                    filteredArtists = filterArtists(it.artists, intent.value),
                    filteredFolders = filterFolders(it.folders, intent.value),
                )
            }

            is DmtAction.Show -> {
                reduce { it.copy(view = intent.view, error = null) }
            }

            is DmtAction.OpenAlbum -> reduce { it.copy(openAlbum = intent.name) }
            is DmtAction.OpenArtist -> reduce { it.copy(openArtist = intent.name) }
            is DmtAction.OpenFolder -> reduce { it.copy(openFolder = intent.path) }
            is DmtAction.OpenPlaylist -> reduce { it.copy(openPlaylist = intent.name) }

            is DmtAction.CreatePlaylist -> mutatePlaylists {
                playlistRepository.create(intent.name)
            }

            is DmtAction.DeletePlaylist -> {
                reduce { it.copy(openPlaylist = null) }
                mutatePlaylists { playlistRepository.delete(intent.name) }
            }

            is DmtAction.AddToPlaylist -> mutatePlaylists {
                playlistRepository.addTrack(intent.name, intent.track)
            }

            is DmtAction.RemoveFromPlaylist -> mutatePlaylists {
                playlistRepository.removeTrack(intent.name, intent.path)
            }

            is DmtAction.PlayAt -> c?.run {
                reduce { it.copy(error = null) }
                val (queue, startIndex) = windowQueue(intent.list, intent.index)
                setMediaItems(
                    queue.map { it.toMediaItem() },
                    startIndex,
                    0L,
                )
                prepare()
                play()
            }

            is DmtAction.Enqueue -> c?.run {
                addMediaItems(intent.list.take(QUEUE_CAP).map { it.toMediaItem() })
                prepare()
                notify(context.getString(R.string.queued, intent.label))
            }

            is DmtAction.Jump -> c?.run {
                seekTo(intent.index, 0L)
                prepare()
                play()
            }

            DmtAction.TogglePlay -> c?.togglePlayPause()
            DmtAction.Next -> c?.seekToNext()
            DmtAction.Prev -> c?.seekToPrevious()
            DmtAction.ToggleShuffle -> c?.run { shuffleModeEnabled = !shuffleModeEnabled }
            DmtAction.CycleRepeat -> c?.cycleRepeat()

            is DmtAction.Seek -> c?.run {
                val duration = currentState.durationMs
                if (duration > 0) {
                    val target = (intent.fraction * duration).toLong()
                    seekTo(target)
                    reduce { it.copy(positionMs = target) }
                }
            }

            is DmtAction.Expand -> reduce { it.copy(expanded = intent.value) }

            is DmtAction.RemoveAt -> c?.run {
                if (intent.index in 0 until mediaItemCount) removeMediaItem(intent.index)
            }

            DmtAction.FetchLyrics -> fetchOnlineLyrics()
            is DmtAction.EmbedLyrics -> embedPendingLyrics(intent.granted)
            DmtAction.CycleSleep -> cycleSleep()
            DmtAction.CycleSpeed -> cycleSpeed()
            DmtAction.OpenEqualizer -> openEqualizer()
            DmtAction.NoEqualizer -> notify(context.getString(R.string.no_eq))

            is DmtAction.Config -> {
                val old = currentState.settings
                reduce { it.copy(settings = intent.settings) }
                if (old.librarySort != intent.settings.librarySort) {
                    reduce {
                        it.copy(
                            filtered = filter(it.tracks, it.query, intent.settings.librarySort),
                        )
                    }
                }
                if (old.sourceMode != intent.settings.sourceMode) {
                    c?.run {
                        stop()
                        clearMediaItems()
                    }
                }
                viewModelScope.launch {
                    settingsRepository.save(intent.settings)
                    if (old.sourceMode != intent.settings.sourceMode ||
                        old.blockedFolders != intent.settings.blockedFolders
                    ) {
                        scan()
                    }
                }
                if (old.cols != intent.settings.cols) loadCover(c?.currentMediaItem)
            }

            is DmtAction.ShowLogin ->
                reduce {
                    it.copy(
                        view = DmtView.SOURCE_LOGIN,
                        loginSource = intent.mode,
                        error = null,
                    )
                }

            is DmtAction.SourceLogin -> when (intent.mode) {
                SourceMode.JELLYFIN -> loginToJellyfin(intent)
                SourceMode.LOCAL -> Unit
            }
        }
    }

    private fun loginToJellyfin(intent: DmtAction.SourceLogin) =
        viewModelScope.launch {
            reduce { it.copy(scanning = true, error = null) }
            jellyfinLogin(intent.url, intent.username, intent.password)
                .onSuccess {
                    val settings = settingsRepository.settings.first()
                    reduce { it.copy(settings = settings, view = DmtView.LIBRARY) }
                    scan()
                }
                .onFailure {
                    reduce {
                        it.copy(
                            scanning = false,
                            error = context.getString(R.string.source_login_failed),
                        )
                    }
                }
        }

    private fun connect() =
        viewModelScope.launch {
            val c = runCatching { context.mediaController() }.getOrNull()
                ?: return@launch
            controller = c
            c.addListener(listener)
            syncFrom(c)
            restoreSleep(c)
            restoreSpeed(c)
            loadCover(c.currentMediaItem)
            loadTech(c.currentMediaItem)
            loadLyrics(c.currentMediaItem)
            restoreSession()
            while (isActive) {
                val position = c.currentPosition.coerceAtLeast(0L)
                val duration = c.duration.takeIf { d -> d != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
                val index = c.currentMediaItemIndex
                val sleepLeft = sleepEndAt?.let { end ->
                    (end - System.currentTimeMillis()).coerceAtLeast(0L)
                } ?: 0L
                val sleepExpired = sleepEndAt != null && sleepLeft == 0L
                if (sleepExpired) sleepEndAt = null
                reduce {
                    if (it.positionMs == position &&
                        it.durationMs == duration &&
                        it.queueIndex == index &&
                        it.sleepLeftMs == sleepLeft &&
                        !sleepExpired
                    ) {
                        it
                    } else {
                        it.copy(
                            positionMs = position,
                            durationMs = duration,
                            queueIndex = index,
                            sleepLeftMs = sleepLeft,
                            sleepMinutes = if (sleepExpired) 0 else it.sleepMinutes,
                        )
                    }
                }
                delay((if (c.isPlaying) 500 else 1500).milliseconds)
            }
        }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            reduce {
                it.copy(
                    nowPlayingId = mediaItem?.mediaId,
                    lyrics = null,
                    error = null,
                )
            }
            loadCover(mediaItem)
            loadTech(mediaItem)
            loadLyrics(mediaItem)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            reduce {
                it.copy(
                    title = mediaMetadata.title?.toString() ?: "unknown",
                    artist = mediaMetadata.artist?.toString() ?: "unknown artist",
                    album = mediaMetadata.albumTitle?.toString().orEmpty(),
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            reduce { it.copy(isPlaying = isPlaying) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            reduce { it.copy(shuffle = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            reduce { it.copy(repeat = repeatMode) }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            reduce { it.copy(speed = playbackParameters.speed) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            controller?.let { c -> reduce { it.copy(queue = c.queueLabels()) } }
        }

        override fun onPlayerError(error: PlaybackException) {
            reduce {
                val name = error.errorCodeName.lowercase()
                it.copy(error = context.getString(R.string.playback_error, name))
            }
        }
    }

    private fun syncFrom(c: MediaController) {
        reduce {
            it.copy(
                nowPlayingId = c.currentMediaItem?.mediaId,
                title = c.mediaMetadata.title?.toString() ?: "unknown",
                artist = c.mediaMetadata.artist?.toString() ?: "unknown artist",
                isPlaying = c.isPlaying,
                shuffle = c.shuffleModeEnabled,
                repeat = c.repeatMode,
                album = c.mediaMetadata.albumTitle?.toString().orEmpty(),
                speed = c.playbackParameters.speed,
                queue = c.queueLabels(),
            )
        }
    }

    private fun scan() =
        viewModelScope.launch {
            reduce { it.copy(scanning = true) }
            val query = currentState.query
            val library = runCatching { scanLibrary() }.getOrElse {
                reduce { state ->
                    state.copy(
                        scanning = false,
                        tracks = emptyList(),
                        albums = emptyList(),
                        artists = emptyList(),
                        folders = emptyList(),
                        filtered = emptyList(),
                        filteredAlbums = emptyList(),
                        filteredArtists = emptyList(),
                        filteredFolders = emptyList(),
                        error = context.getString(
                            R.string.scan_failed,
                            state.settings.sourceMode.label,
                        ),
                    )
                }
                return@launch
            }
            val (filteredTracks, filteredAlbums, filteredArtists, filteredFolders) = withContext(
                dispatchers.default,
            ) {
                FilteredLibrary(
                    tracks = filter(library.tracks, query, currentState.settings.librarySort),
                    albums = filterAlbums(library.albums, query),
                    artists = filterArtists(library.artists, query),
                    folders = filterFolders(library.folders, query),
                )
            }
            reduce {
                it.copy(
                    scanning = false,
                    tracks = library.tracks,
                    albums = library.albums,
                    artists = library.artists,
                    folders = library.folders,
                    filtered = filteredTracks,
                    filteredAlbums = filteredAlbums,
                    filteredArtists = filteredArtists,
                    filteredFolders = filteredFolders,
                    error = null,
                )
            }
            mutatePlaylists()
            restoreSession()
        }

    private fun restoreSession() {
        if (sessionRestored) return
        val c = controller ?: return
        val tracks = currentState.tracks
        if (tracks.isEmpty()) return
        if (c.mediaItemCount > 0) {
            sessionRestored = true
            return
        }
        sessionRestored = true
        viewModelScope.launch {
            val session = settingsRepository.lastSession() ?: return@launch

            val byId = tracks.associateBy { it.id }
            val existing = session.queueIds.mapNotNull { byId[it] }
            if (existing.isEmpty()) return@launch

            val savedCurrentId = session.queueIds.getOrNull(session.index)
            var index = existing.indexOfFirst { it.id == savedCurrentId }
            var position = session.positionMs
            if (index < 0) {
                index = 0
                position = 0L
            }
            val (queue, startIndex) = windowQueue(existing, index)
            c.setMediaItems(
                queue.map { it.toMediaItem() },
                startIndex,
                position,
            )
            c.prepare()
        }
    }

    private fun fetchOnlineLyrics() {
        val id = currentState.nowPlayingId ?: return
        if (currentState.lyricsFetching || currentState.lyrics != null) return
        val track = currentState.tracks.find { it.id.toString() == id } ?: return

        reduce { it.copy(lyricsFetching = true) }
        viewModelScope.launch {
            val text = getLyrics.onlineText(track)
            val lyrics = text?.let { getLyrics.parse(it) }

            reduce {
                if (it.nowPlayingId != id) {
                    it.copy(lyricsFetching = false)
                } else {
                    it.copy(lyricsFetching = false, lyrics = lyrics)
                }
            }
            if (lyrics == null) {
                notify(context.getString(R.string.no_lyrics_found))
                return@launch
            }

            val intentSender = embedLyrics.writeRequest(track)
            if (intentSender == null) {
                notify(context.getString(R.string.lyrics_embed_unsupported))
                return@launch
            }
            pendingEmbed = track to text
            sendEffect(PlayerEffect.RequestWrite(intentSender))
        }
    }

    private fun embedPendingLyrics(granted: Boolean) {
        val (track, text) = pendingEmbed ?: return
        pendingEmbed = null
        if (!granted) return

        viewModelScope.launch {
            val done = embedLyrics(track, text)
            notify(
                context.getString(
                    if (done) R.string.lyrics_embedded else R.string.lyrics_embed_failed,
                ),
            )
        }
    }

    private fun loadLyrics(mediaItem: MediaItem?) {
        val forId = mediaItem?.mediaId
        reduce { it.copy(lyricsFetching = true) }
        viewModelScope.launch {
            val track = currentState.tracks.find { it.id.toString() == forId }
            val lyrics = track?.let { getLyrics(it) }
            reduce {
                if (it.nowPlayingId != forId) {
                    it
                } else {
                    it.copy(lyrics = lyrics, lyricsFetching = false)
                }
            }
        }
    }

    private fun loadCover(mediaItem: MediaItem?) {
        val uri: Uri? = mediaItem?.mediaMetadata?.artworkUri
        val forId = mediaItem?.mediaId
        viewModelScope.launch {
            val raw = uri?.let { getCoverArt(it) }
            val cover = withContext(dispatchers.io) {
                raw?.let { art ->
                    runCatching {
                        art.toAsciiBitmap(context, currentState.settings.cols)
                    }.getOrNull()
                } ?: mediaItem?.let {
                    generateAsciiPlaceholder(
                        context = context,
                        seed = forId?.toLongOrNull() ?: forId.hashCode().toLong(),
                        cols = currentState.settings.cols,
                    )
                }
            }
            reduce {
                if (it.nowPlayingId != forId) it else it.copy(cover = cover, artRaw = raw)
            }
        }
    }

    private fun loadTech(mediaItem: MediaItem?) {
        val uri = mediaItem?.localConfiguration?.uri
        val id = mediaItem?.mediaId
        viewModelScope.launch {
            val track = currentState.tracks.find { t -> t.id.toString() == id }
            val tech = uri?.let { getTrackTech(it, track) }.orEmpty()
            reduce {
                if (it.nowPlayingId != id) it else it.copy(tech = tech)
            }
        }
    }

    private fun openEqualizer() {
        val c = controller ?: return
        viewModelScope.launch {
            val sessionId = runCatching {
                c.sendCustomCommand(PlaybackService.CMD_AUDIO_SESSION, Bundle.EMPTY)
                    .await()
                    .extras
                    .getInt(PlaybackService.KEY_AUDIO_SESSION)
            }.getOrDefault(0)
            sendEffect(PlayerEffect.OpenEqualizer(sessionId))
        }
    }

    private fun cycleSpeed() {
        val c = controller ?: return
        val currentIndex = SPEED_STEPS.indexOfFirst { abs(it - currentState.speed) < 0.01f }
        val next = SPEED_STEPS[(currentIndex + 1).mod(SPEED_STEPS.size)]
        c.setPlaybackSpeed(next)
        viewModelScope.launch {
            settingsRepository.saveSpeed(next)
        }
    }

    private fun cycleSleep() {
        val c = controller ?: return
        val currentIndex = SLEEP_STEPS.indexOf(currentState.sleepMinutes)
        val next = SLEEP_STEPS[(currentIndex + 1).mod(SLEEP_STEPS.size)]
        val endAt = if (next == 0) 0L else System.currentTimeMillis() + next * 60_000L
        c.sendCustomCommand(
            PlaybackService.CMD_SLEEP_SET,
            Bundle().apply { putLong(PlaybackService.KEY_END_AT, endAt) },
        )
        sleepEndAt = endAt.takeIf { it > 0L }
        reduce {
            it.copy(
                sleepMinutes = next,
                sleepLeftMs = if (next == 0) 0L else next * 60_000L,
            )
        }
    }

    private suspend fun restoreSpeed(c: MediaController) {
        val saved = settingsRepository.savedSpeed()
        if (abs(c.playbackParameters.speed - saved) > 0.01f) {
            c.setPlaybackSpeed(saved)
        }
    }

    private suspend fun restoreSleep(c: MediaController) {
        runCatching {
            val result = c.sendCustomCommand(PlaybackService.CMD_SLEEP_GET, Bundle.EMPTY).await()
            val endAt = result.extras.getLong(PlaybackService.KEY_END_AT)
            if (endAt > System.currentTimeMillis()) {
                sleepEndAt = endAt
                val left = endAt - System.currentTimeMillis()
                val step = when {
                    left <= 15 * 60_000L -> 15
                    left <= 30 * 60_000L -> 30
                    else -> 60
                }
                reduce { it.copy(sleepMinutes = step, sleepLeftMs = left) }
            }
        }
    }

    private fun notify(message: String) {
        noticeJob?.cancel()
        reduce { it.copy(notice = message) }
        noticeJob = viewModelScope.launch {
            delay(2.seconds)
            reduce { it.copy(notice = null) }
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
    }
}
