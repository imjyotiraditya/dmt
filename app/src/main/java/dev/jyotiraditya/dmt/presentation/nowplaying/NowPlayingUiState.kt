package dev.jyotiraditya.dmt.presentation.nowplaying

import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.model.Track
import dev.jyotiraditya.dmt.domain.model.AudioQuality
import dev.jyotiraditya.dmt.domain.model.Cover
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.QueueEntry
import dev.jyotiraditya.dmt.domain.model.SleepState
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.lyrics.Lyrics

/** The lyrics of the track playing, and what each source has for it. */
data class LyricsState(
    /** The media id of the item these lyrics belong to. */
    val forId: String? = null,
    val lyrics: Lyrics? = null,
    val fetching: Boolean = false,
    /** The source picked for this track. */
    val source: LyricsSource = LyricsSource.DEFAULT,
    /** What each asked source has; online sources not yet asked are absent. */
    val sources: Map<LyricsSource, LyricsSourceStatus> = emptyMap(),
    /** The source of the lyrics on screen, or null when none are. */
    val showingFrom: LyricsSource? = null,
)

data class NowPlayingUiState(
    val nowPlayingId: String? = null,
    /** The library track playing, or null when nothing is or the library does not hold it. */
    val track: Track? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val queue: List<QueueEntry> = emptyList(),
    /** The playing item's place in [queue]. */
    val queuePosition: Int = 0,
    /** Why the item playing cannot play. */
    val fault: UiText? = null,
    val cover: Cover = Cover.Loading,
    val animatedArtUrl: String? = null,
    val tech: List<Spec> = emptyList(),
    val quality: AudioQuality? = null,
    /** Where the audio goes: the output device and its format. */
    val route: List<Spec> = emptyList(),
    val lyrics: LyricsState = LyricsState(),
    val sleep: SleepState = SleepState(),
    val settings: DmtSettings = DmtSettings(),
    val library: LibrarySnapshot = LibrarySnapshot(),
    val notice: UiText? = null,
    val error: UiText? = null,
)

/** A number fixed per track, for placeholders that stay the same for it. */
val NowPlayingUiState.seed: Long
    get() = nowPlayingId?.hashCode()?.toLong() ?: 0L
