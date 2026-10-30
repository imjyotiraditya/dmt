package dev.jyotiraditya.dmt.presentation.nowplaying

import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.listenTo
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.data.repository.AnimatedArtworkRepository
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import dev.jyotiraditya.dmt.data.repository.PlaybackRepository
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.data.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.domain.model.Cover
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.NowPlaying
import dev.jyotiraditya.dmt.domain.model.PlaybackFault
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.TrackTech
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.domain.usecase.GetLyricsUseCase
import dev.jyotiraditya.dmt.domain.usecase.GetTrackTechUseCase
import dev.jyotiraditya.dmt.presentation.main.Notices
import dev.jyotiraditya.dmt.util.playedAudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val COVER_PX = 1024

/** The item the player decoded last, and the audio format it played. */
private data class PlayedMedia(val uri: Uri?, val format: Format?)

/** The player sheet: what plays, its cover and lyrics, the queue, sleep timer and speed. */
@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NowPlayingViewModel
@Inject
constructor(
    private val playback: PlaybackRepository,
    private val libraryRepository: LibraryRepository,
    private val preferences: PreferencesRepository,
    private val getLyrics: GetLyricsUseCase,
    private val artwork: ArtworkRepository,
    private val animatedArtwork: AnimatedArtworkRepository,
    private val getTrackTech: GetTrackTechUseCase,
    trackMediaRepository: TrackMediaRepository,
    notices: Notices,
) : ViewModel(),
    NowPlayingActions {

    val player: StateFlow<Player?> = playback.player

    /** The item playing and its library track, changing only when either does. */
    private val playing: Flow<Pair<String?, Track?>> =
        playback.nowPlaying.map { it.mediaId to it.track }.distinctUntilChanged()

    private val cover: Flow<Cover> = playing.transformLatest { (_, track) ->
        if (track == null) {
            emit(Cover.Missing)
            return@transformLatest
        }
        val ready = artwork.cached(track, COVER_PX)?.let(Cover::Ready)
        emit(ready ?: Cover.Loading)
        emit(artwork.load(track, COVER_PX)?.let(Cover::Ready) ?: ready ?: Cover.Missing)
    }

    private val animatedArtUrl: Flow<String?> =
        combine(playing, preferences.settings.map { it.animatedArt }.distinctUntilChanged()) {
                (_, track),
                enabled,
            ->
            track.takeIf { enabled }
        }
            .transformLatest { track ->
                emit(null)
                track?.let {
                    emit(withContext(Dispatchers.IO) { animatedArtwork.streamUrlFor(it) })
                }
            }

    private val tech: Flow<TrackTech> =
        playback.player
            .flatMapLatest { it?.playedMedia() ?: flowOf(PlayedMedia(null, null)) }
            .distinctUntilChanged()
            .combine(playing) { media, (_, track) -> media to track }
            .mapLatest { (media, track) ->
                media.uri?.let { getTrackTech(it, track, media.format) } ?: TrackTech.None
            }

    private val route: Flow<List<Spec>> =
        trackMediaRepository.routeSpecs().distinctUntilChanged().flowOn(Dispatchers.IO)

    private val lyrics = MutableStateFlow(LyricsState())

    /** An online source picked for the item with this media id, until the item changes. */
    private val onlinePick = MutableStateFlow<OnlinePick?>(null)

    private val details =
        combine(cover, animatedArtUrl, tech, route) { cover, animatedArt, tech, route ->
            Details(cover, animatedArt, tech, route)
        }

    val state: StateFlow<NowPlayingUiState> =
        combine(
            playback.nowPlaying,
            details,
            lyrics,
            playback.sleep,
            preferences.settings,
        ) { playing, details, lyrics, sleep, settings ->
            NowPlayingUiState(
                nowPlayingId = playing.mediaId,
                track = playing.track,
                title = playing.title,
                artist = playing.artist,
                album = playing.album,
                durationMs = playing.durationMs,
                queue = playing.queue,
                queuePosition = playing.queuePosition.coerceAtLeast(0),
                fault = playing.faultText(details.tech),
                cover = details.cover,
                animatedArtUrl = details.animatedArtUrl,
                tech = details.tech.specs,
                quality = details.tech.quality,
                route = details.route,
                lyrics = lyrics,
                sleep = sleep,
                settings = settings,
            )
        }
            .combine(libraryRepository.library) { state, library -> state.copy(library = library) }
            .combine(notices.notice) { state, notice -> state.copy(notice = notice) }
            .combine(notices.error) { state, error -> state.copy(error = error) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NowPlayingUiState())

    init {
        viewModelScope.launch {
            combine(
                playing,
                preferences.settings.map { it.lyricsSource }.distinctUntilChanged(),
                onlinePick,
            ) { (id, track), saved, pick ->
                val picked = pick?.takeIf { it.forId == id }
                LyricsRequest(id, track, picked?.source ?: saved, picked?.attempt ?: 0)
            }
                .distinctUntilChanged()
                .collectLatest(::loadLyrics)
        }
    }

    override fun pickLyricsSource(source: LyricsSource) {
        if (source.online) {
            val id = state.value.nowPlayingId
            onlinePick.update { OnlinePick(id, source, (it?.attempt ?: 0) + 1) }
            return
        }
        onlinePick.value = null
        viewModelScope.launch { preferences.update { it.copy(lyricsSource = source) } }
    }

    override fun refreshLyricsSources() {
        val track = state.value.track ?: return
        viewModelScope.launch {
            val sources = getLyrics.availableSources(track).toSourceStatuses()
            lyrics.update { it.copy(sources = it.sources + sources) }
        }
    }

    override fun updateSettings(transform: (DmtSettings) -> DmtSettings) {
        viewModelScope.launch { preferences.update(transform) }
    }

    override fun seek(fraction: Float) = playback.seek(fraction)

    override fun jumpTo(index: Int) = playback.jumpTo(index)

    override fun move(from: Int, to: Int) = playback.move(from, to)

    override fun remove(index: Int) = playback.remove(index)

    override fun clearUpcoming() = playback.clearUpcoming()

    override fun setSleep(minutes: Int) = playback.setSleep(minutes)

    override fun sleepAtSongEnd() = playback.sleepAtSongEnd()

    override fun setSpeed(speed: Float) = playback.setSpeed(speed)

    /** The library artist of the track playing, or null when the library has none for it. */
    fun nowPlayingArtist(): String? {
        val track = state.value.track ?: return null
        val library = libraryRepository.library.value
        return (
            library.artist(track.artist)
                ?: library.artistOf(track)
                ?: library.artists.find { track.artist.startsWith(it.name) }
            )
            ?.name
    }

    private suspend fun loadLyrics(request: LyricsRequest) {
        val (id, track, source) = request
        // Lyrics of the same item stay on screen while another source is read.
        lyrics.update {
            if (it.forId == id) {
                it.copy(fetching = true, source = source)
            } else {
                LyricsState(forId = id, fetching = true, source = source)
            }
        }
        if (track == null) {
            lyrics.update { it.copy(fetching = false) }
            return
        }
        if (source.online) {
            val result = getLyrics.onlineLyrics(source, track)
            lyrics.update { current ->
                val sources = current.sources + (source to result.toSourceStatus())
                val found =
                    result.getOrNull()
                        ?: return@update current.copy(fetching = false, sources = sources)
                current.copy(
                    lyrics = found,
                    fetching = false,
                    sources = sources,
                    showingFrom = source,
                )
            }
            return
        }
        val result = Result.success(getLyrics(track, source))
        val found = result.getOrNull()
        val sources = getLyrics.availableSources(track).toSourceStatuses()
        lyrics.update {
            it.copy(
                lyrics = found,
                fetching = false,
                sources = it.sources + sources + (source to result.toSourceStatus()),
                showingFrom = source.takeIf { found != null },
            )
        }
    }

    /** An online [source] picked for the item [forId]; [attempt] makes a repeat pick ask again. */
    private data class OnlinePick(val forId: String?, val source: LyricsSource, val attempt: Int)

    private data class LyricsRequest(
        val id: String?,
        val track: Track?,
        val source: LyricsSource,
        val attempt: Int,
    )

    private data class Details(
        val cover: Cover,
        val animatedArtUrl: String?,
        val tech: TrackTech,
        val route: List<Spec>,
    )
}

/** Why this item cannot play, naming the format from [tech] when it is unsupported. */
private fun NowPlaying.faultText(tech: TrackTech): UiText? = when (val fault = fault) {
    PlaybackFault.Unsupported -> {
        val format = tech.specs.firstOrNull { it.label == "FMT" }?.value?.lowercase()
        UiText(R.string.playback_unsupported, format.orEmpty())
    }

    is PlaybackFault.Failed -> UiText(R.string.playback_error, fault.code.lowercase())

    null -> null
}

/** The item this player decodes and its audio format, again after every change to them. */
@OptIn(UnstableApi::class)
private fun Player.playedMedia(): Flow<PlayedMedia> = channelFlow {
    fun current() = PlayedMedia(
        uri = currentMediaItem?.localConfiguration?.uri,
        format = currentTracks.playedAudioFormat(),
    )
    send(current())
    listenTo(Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_TRACKS_CHANGED) {
        trySend(current())
    }
}
