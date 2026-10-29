package dev.jyotiraditya.dmt.presentation.nowplaying

import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.LyricsSource

/** What the player sheet can do with what plays. */
interface NowPlayingActions {
    /** Seeks to [fraction] of the item playing. */
    fun seek(fraction: Float)

    fun jumpTo(index: Int)

    /** Moves the item at player index [from] to index [to]. */
    fun move(from: Int, to: Int)

    fun remove(index: Int)

    /** Removes everything queued after the item playing. */
    fun clearUpcoming()

    /** Starts the sleep timer for [minutes], or stops it at 0. */
    fun setSleep(minutes: Int)

    /** Stops playback when the song playing ends. */
    fun sleepAtSongEnd()

    fun setSpeed(speed: Float)

    /** Shows lyrics from [source]: an online one for this track only, others from now on. */
    fun pickLyricsSource(source: LyricsSource)

    /** Reads again which sources have lyrics, as files may have been added while it played. */
    fun refreshLyricsSources()

    fun updateSettings(transform: (DmtSettings) -> DmtSettings)
}
