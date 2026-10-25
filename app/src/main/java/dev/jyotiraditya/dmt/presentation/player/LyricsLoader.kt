package dev.jyotiraditya.dmt.presentation.player

import androidx.media3.common.MediaItem
import dev.jyotiraditya.dmt.core.base.StateStore
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.usecase.GetLyricsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Loads the lyrics of the track being played, and which sources have them, into [store]. */
internal class LyricsLoader(
    private val getLyrics: GetLyricsUseCase,
    private val preferences: PreferencesRepository,
    private val scope: CoroutineScope,
    private val store: StateStore<DmtState>,
) {
    private var job: Job? = null

    /** Reads the lyrics of [mediaItem] from the saved source. */
    fun load(mediaItem: MediaItem?) {
        val forId = mediaItem?.mediaId
        store.reduce { it.copy(lyricsFetching = true) }
        job?.cancel()
        job = scope.launch {
            val track = store.current.library.track(forId)
            val preferred = savedSource()
            val result = Result.success(track?.let { getLyrics(it, preferred) })
            val sources = track?.let { getLyrics.availableSources(it) }.orEmpty().toSourceStatuses()
            store.reduce { it.withLyrics(forId, preferred, result, sources) }
        }
    }

    fun openSources() {
        store.reduce {
            it.copy(lyricsSource = it.settings.lyricsSource, lyricsSourcesOpen = true)
        }
        // Lyrics files added while the track plays show up when the sheet opens.
        val (id, track) = nowPlaying() ?: return
        scope.launch {
            val sources = getLyrics.availableSources(track).toSourceStatuses()
            store.reduce { it.withLyricsSources(id, sources) }
        }
    }

    /** Shows lyrics from [source]; an online pick is for this track only, others are saved. */
    fun choose(source: LyricsSource, mediaItem: MediaItem?) {
        if (source.online) {
            store.reduce { it.copy(lyricsSource = source) }
            fetch(source)
            return
        }
        store.reduce {
            it.copy(lyricsSource = source, settings = it.settings.copy(lyricsSource = source))
        }
        scope.launch { preferences.update { it.copy(lyricsSource = source) } }
        load(mediaItem)
    }

    /** The saved source, read from storage until the settings have loaded. */
    private suspend fun savedSource(): LyricsSource {
        val state = store.current
        return if (state.settingsLoaded) {
            state.settings.lyricsSource
        } else {
            preferences.settings.first().lyricsSource
        }
    }

    /** The media id and track being played, or null when nothing in the library plays. */
    private fun nowPlaying(): Pair<String, Track>? {
        val state = store.current
        val id = state.nowPlayingId ?: return null
        return state.library.track(id)?.let { id to it }
    }

    private fun fetch(source: LyricsSource) {
        val (id, track) = nowPlaying() ?: return

        store.reduce { it.withLyricsFetching(source) }
        job?.cancel()
        job = scope.launch {
            val result = getLyrics.onlineLyrics(source, track)
            store.reduce { it.withFetchedLyrics(id, source, result) }
        }
    }
}
