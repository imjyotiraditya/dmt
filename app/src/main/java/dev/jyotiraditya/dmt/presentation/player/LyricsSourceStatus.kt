package dev.jyotiraditya.dmt.presentation.player

import dev.jyotiraditya.dmt.domain.model.LyricsSource
import dev.jyotiraditya.dmt.domain.model.LyricsTiming
import dev.jyotiraditya.dmt.domain.model.timing
import dev.jyotiraditya.lyrics.Lyrics

/** What a lyrics source has for the track being played. */
sealed interface LyricsSourceStatus {
    data class Found(val timing: LyricsTiming) : LyricsSourceStatus

    data object Fetching : LyricsSourceStatus

    data object Missing : LyricsSourceStatus

    /** The online source could not be reached. */
    data object Unreachable : LyricsSourceStatus
}

/**
 * The status of each source from the timings of the sources holding lyrics; a phone source without
 * lyrics is missing, an online one is left out until it is asked.
 */
internal fun Map<LyricsSource, LyricsTiming>.toSourceStatuses():
    Map<LyricsSource, LyricsSourceStatus> =
    LyricsSource.entries
        .filter { it in this || !it.online }
        .associateWith { source ->
            this[source]?.let(LyricsSourceStatus::Found) ?: LyricsSourceStatus.Missing
        }

internal fun Result<Lyrics?>.toSourceStatus(): LyricsSourceStatus = fold(
    onSuccess = { lyrics ->
        lyrics?.let { LyricsSourceStatus.Found(it.timing) } ?: LyricsSourceStatus.Missing
    },
    onFailure = { LyricsSourceStatus.Unreachable },
)
