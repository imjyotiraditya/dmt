package dev.jyotiraditya.dmt.ui

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import dev.jyotiraditya.dmt.data.Album
import dev.jyotiraditya.dmt.data.KEY_ACCENT
import dev.jyotiraditya.dmt.data.KEY_COLS
import dev.jyotiraditya.dmt.data.KEY_RAW
import dev.jyotiraditya.dmt.data.KEY_SPEED
import dev.jyotiraditya.dmt.data.KEY_SPECS
import dev.jyotiraditya.dmt.data.KEY_WAVE
import dev.jyotiraditya.dmt.data.MediaLibrary
import dev.jyotiraditya.dmt.data.dmtStore
import dev.jyotiraditya.dmt.data.Spec
import dev.jyotiraditya.dmt.data.Track
import dev.jyotiraditya.dmt.data.toAlbums
import dev.jyotiraditya.dmt.player.asKHz
import dev.jyotiraditya.dmt.player.asKbps
import dev.jyotiraditya.dmt.player.asMB
import dev.jyotiraditya.dmt.player.codecLabel
import dev.jyotiraditya.dmt.player.cycleRepeat
import dev.jyotiraditya.dmt.player.mediaController
import dev.jyotiraditya.dmt.player.queueLabels
import dev.jyotiraditya.dmt.playback.PlaybackService
import dev.jyotiraditya.dmt.player.await
import dev.jyotiraditya.dmt.player.toMediaItem
import dev.jyotiraditya.dmt.player.togglePlayPause
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DmtView { LIBRARY, ALBUMS, SETTINGS }

data class DmtSettings(
    val wave: Boolean = true,
    val cols: Int = 64,
    val listSpecs: Boolean = true,
    val accent: Int = 0,
    val rawArt: Boolean = false,
)

data class DmtState(
    val hasPermission: Boolean = false,
    val scanning: Boolean = true,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val query: String = "",
    val filtered: List<Track> = emptyList(),
    val filteredAlbums: List<Album> = emptyList(),
    val view: DmtView = DmtView.LIBRARY,
    val openAlbum: String? = null,
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
    val expanded: Boolean = false,
    val sleepMinutes: Int = 0,
    val sleepLeftMs: Long = 0L,
    val speed: Float = 1f,
    val settings: DmtSettings = DmtSettings(),
    val tech: List<Spec> = emptyList(),
    val error: String? = null,
    val notice: String? = null,
)

sealed interface DmtAction {
    data class Permission(val granted: Boolean) : DmtAction
    data object Rescan : DmtAction
    data class Query(val value: String) : DmtAction
    data class Show(val view: DmtView) : DmtAction
    data class OpenAlbum(val name: String?) : DmtAction
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
    data object CycleSleep : DmtAction
    data object CycleSpeed : DmtAction
    data class Config(val settings: DmtSettings) : DmtAction
}

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(
        DmtState(
            hasPermission = ContextCompat.checkSelfPermission(
                app, Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    )
    val state = _state.asStateFlow()

    private var controller: MediaController? = null
    private var noticeJob: Job? = null
    private var sleepEndAt: Long? = null

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dmtStore.data.first()
            val settings = DmtSettings(
                wave = prefs[KEY_WAVE] ?: true,
                cols = prefs[KEY_COLS] ?: 64,
                listSpecs = prefs[KEY_SPECS] ?: true,
                accent = prefs[KEY_ACCENT] ?: 0,
                rawArt = prefs[KEY_RAW] ?: false,
            )
            _state.update { it.copy(settings = settings) }
            applyIcon(settings.accent)
        }
        if (_state.value.hasPermission) scan()
        connect()
    }

    private fun filter(tracks: List<Track>, query: String): List<Track> =
        if (query.isBlank()) tracks else tracks.filter {
            it.title.contains(query, true) ||
                it.artist.contains(query, true) ||
                it.album.contains(query, true)
        }

    private fun filterAlbums(albums: List<Album>, query: String): List<Album> =
        if (query.isBlank()) albums else albums.filter {
            it.name.contains(query, true) || it.artist.contains(query, true)
        }

    fun dispatch(action: DmtAction) {
        val c = controller
        when (action) {
            is DmtAction.Permission -> {
                _state.update { it.copy(hasPermission = action.granted) }
                if (action.granted) scan()
            }

            DmtAction.Rescan -> scan()
            is DmtAction.Query -> _state.update {
                it.copy(
                    query = action.value,
                    filtered = filter(it.tracks, action.value),
                    filteredAlbums = filterAlbums(it.albums, action.value),
                )
            }
            is DmtAction.Show -> _state.update { it.copy(view = action.view) }
            is DmtAction.OpenAlbum -> _state.update { it.copy(openAlbum = action.name) }

            is DmtAction.PlayAt -> c?.run {
                _state.update { it.copy(error = null) }
                setMediaItems(action.list.map { it.toMediaItem() }, action.index, 0L)
                prepare()
                play()
            }

            is DmtAction.Enqueue -> c?.run {
                addMediaItems(action.list.map { it.toMediaItem() })
                prepare()
                notify("queued: ${action.label}")
            }

            is DmtAction.Jump -> c?.run {
                seekTo(action.index, 0L)
                prepare()
                play()
            }

            DmtAction.TogglePlay -> c?.togglePlayPause()
            DmtAction.Next -> c?.seekToNext()
            DmtAction.Prev -> c?.seekToPrevious()
            DmtAction.ToggleShuffle -> c?.run { shuffleModeEnabled = !shuffleModeEnabled }
            DmtAction.CycleRepeat -> c?.cycleRepeat()

            is DmtAction.Seek -> c?.run {
                val duration = _state.value.durationMs
                if (duration > 0) {
                    val target = (action.fraction * duration).toLong()
                    seekTo(target)
                    _state.update { it.copy(positionMs = target) }
                }
            }

            is DmtAction.Expand -> _state.update { it.copy(expanded = action.value) }

            is DmtAction.RemoveAt -> c?.run {
                if (action.index in 0 until mediaItemCount) removeMediaItem(action.index)
            }

            DmtAction.CycleSleep -> cycleSleep()
            DmtAction.CycleSpeed -> cycleSpeed()

            is DmtAction.Config -> {
                val old = _state.value.settings
                _state.update { it.copy(settings = action.settings) }
                viewModelScope.launch {
                    getApplication<Application>().dmtStore.edit {
                        it[KEY_WAVE] = action.settings.wave
                        it[KEY_COLS] = action.settings.cols
                        it[KEY_SPECS] = action.settings.listSpecs
                        it[KEY_ACCENT] = action.settings.accent
                        it[KEY_RAW] = action.settings.rawArt
                    }
                }
                if (old.cols != action.settings.cols) loadCover(c?.currentMediaItem)
                if (old.accent != action.settings.accent) applyIcon(action.settings.accent)
            }
        }
    }

    private fun connect() = viewModelScope.launch {
        val c = runCatching { getApplication<Application>().mediaController() }.getOrNull()
            ?: return@launch
        controller = c
        c.addListener(listener)
        syncFrom(c)
        restoreSleep(c)
        restoreSpeed(c)
        loadCover(c.currentMediaItem)
        loadTech(c.currentMediaItem)
        while (isActive) {
            val position = c.currentPosition.coerceAtLeast(0L)
            val duration = c.duration.takeIf { d -> d != C.TIME_UNSET }?.coerceAtLeast(0L) ?: 0L
            val index = c.currentMediaItemIndex
            val sleepLeft = sleepEndAt?.let { end ->
                (end - System.currentTimeMillis()).coerceAtLeast(0L)
            } ?: 0L
            val sleepExpired = sleepEndAt != null && sleepLeft == 0L
            if (sleepExpired) sleepEndAt = null
            _state.update {
                if (it.positionMs == position && it.durationMs == duration &&
                    it.queueIndex == index && it.sleepLeftMs == sleepLeft && !sleepExpired
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
            delay(if (c.isPlaying) 500 else 1500)
        }
    }

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _state.update { it.copy(nowPlayingId = mediaItem?.mediaId, error = null) }
            loadCover(mediaItem)
            loadTech(mediaItem)
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            _state.update {
                it.copy(
                    title = mediaMetadata.title?.toString() ?: "unknown",
                    artist = mediaMetadata.artist?.toString() ?: "unknown artist",
                    album = mediaMetadata.albumTitle?.toString().orEmpty(),
                )
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffle = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeat = repeatMode) }
        }

        override fun onPlaybackParametersChanged(
            playbackParameters: androidx.media3.common.PlaybackParameters,
        ) {
            _state.update { it.copy(speed = playbackParameters.speed) }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            controller?.let { c -> _state.update { it.copy(queue = c.queueLabels()) } }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(error = "playback error: ${error.errorCodeName.lowercase()}")
            }
        }
    }

    private fun syncFrom(c: MediaController) {
        _state.update {
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

    private fun scan() = viewModelScope.launch {
        _state.update { it.copy(scanning = true) }
        val tracks = withContext(Dispatchers.IO) {
            MediaLibrary.scan(getApplication())
        }
        _state.update {
            it.copy(
                scanning = false,
                tracks = tracks,
                filtered = filter(tracks, it.query),
                albums = tracks.toAlbums(),
                filteredAlbums = filterAlbums(tracks.toAlbums(), it.query),
            )
        }
    }

    private fun loadCover(mediaItem: MediaItem?) {
        val uri: Uri? = mediaItem?.localConfiguration?.uri
        viewModelScope.launch {
            val raw = uri?.let {
                withContext(Dispatchers.IO) {
                    runCatching {
                        getApplication<Application>().contentResolver
                            .loadThumbnail(it, Size(512, 512), null)
                    }.getOrNull()
                }
            }
            val cover = raw?.let {
                withContext(Dispatchers.IO) {
                    runCatching { it.toAsciiBitmap(_state.value.settings.cols) }.getOrNull()
                }
            }
            _state.update { it.copy(cover = cover, artRaw = raw) }
        }
    }

    private fun loadTech(mediaItem: MediaItem?) {
        val uri = mediaItem?.localConfiguration?.uri
        val id = mediaItem?.mediaId
        viewModelScope.launch {
            val tech = uri?.let {
                withContext(Dispatchers.IO) {
                    buildTech(it, _state.value.tracks.find { t -> t.id.toString() == id })
                }
            }.orEmpty()
            _state.update { it.copy(tech = tech) }
        }
    }

    private fun buildTech(uri: Uri, track: Track?): List<Spec> {
        val app = getApplication<Application>()
        var mime = track?.mime.orEmpty()
        var bitrate = track?.bitrate ?: 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(app, uri, null)
            val format = extractor.getTrackFormat(0)
            format.getString(MediaFormat.KEY_MIME)?.let {
                if (mime.isEmpty() || mime == "audio/?") mime = it
            }
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (bitrate <= 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            }
            extractor.release()
        }
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(app, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()?.takeIf { it > 0 }?.let { bits = it }
                if (sampleRate == null) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                        ?.toIntOrNull()?.let { sampleRate = it }
                }
                if (bitrate <= 0) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()?.let { bitrate = it }
                }
            }
        }
        return buildList {
            if (mime.isNotEmpty()) add(Spec("FMT", mime.codecLabel()))
            bits?.let { add(Spec("BIT", "$it")) }
            sampleRate?.let { add(Spec("RATE", it.asKHz())) }
            channels?.let { add(Spec("CH", if (it == 2) "ST" else "$it")) }
            if (bitrate > 0) add(Spec("KBPS", "${bitrate / 1000}", hot = true))
            track?.size?.takeIf { it > 0 }?.let { add(Spec("SIZE", it.asMB())) }
        }
    }

    private fun cycleSpeed() {
        val c = controller ?: return
        val steps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
        val currentIndex = steps.indexOfFirst { kotlin.math.abs(it - _state.value.speed) < 0.01f }
        val next = steps[(currentIndex + 1).mod(steps.size)]
        c.setPlaybackSpeed(next)
        viewModelScope.launch {
            getApplication<Application>().dmtStore.edit { it[KEY_SPEED] = next }
        }
    }

    private fun cycleSleep() {
        val c = controller ?: return
        val next = when (_state.value.sleepMinutes) {
            0 -> 15
            15 -> 30
            30 -> 60
            else -> 0
        }
        val endAt = if (next == 0) 0L else System.currentTimeMillis() + next * 60_000L
        c.sendCustomCommand(
            PlaybackService.CMD_SLEEP_SET,
            bundleOf(PlaybackService.KEY_END_AT to endAt)
        )
        sleepEndAt = endAt.takeIf { it > 0L }
        _state.update {
            it.copy(
                sleepMinutes = next,
                sleepLeftMs = if (next == 0) 0L else next * 60_000L,
            )
        }
    }

    private suspend fun restoreSpeed(c: MediaController) {
        val saved = getApplication<Application>().dmtStore.data.first()[KEY_SPEED] ?: 1f
        if (kotlin.math.abs(c.playbackParameters.speed - saved) > 0.01f) {
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
                _state.update { it.copy(sleepMinutes = step, sleepLeftMs = left) }
            }
        }
    }

    private fun applyIcon(accent: Int) {
        val app = getApplication<Application>()
        val aliases = listOf("LauncherOrange", "LauncherMoss", "LauncherSteel", "LauncherMono")
        val target = accent % aliases.size
        aliases.forEachIndexed { index, alias ->
            val component = ComponentName(app, "dev.jyotiraditya.dmt.$alias")
            val desired = if (index == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            val current = app.packageManager.getComponentEnabledSetting(component)
            val effective = if (current == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                if (index == 0) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }
            } else {
                current
            }
            if (effective != desired) {
                app.packageManager.setComponentEnabledSetting(
                    component,
                    desired,
                    PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    private fun notify(message: String) {
        noticeJob?.cancel()
        _state.update { it.copy(notice = message) }
        noticeJob = viewModelScope.launch {
            delay(2000)
            _state.update { it.copy(notice = null) }
        }
    }

    override fun onCleared() {
        controller?.release()
        controller = null
    }
}
