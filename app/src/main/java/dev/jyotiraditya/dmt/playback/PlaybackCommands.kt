package dev.jyotiraditya.dmt.playback

import android.os.Bundle
import androidx.media3.session.SessionCommand

/** The custom commands [PlaybackService] accepts and the keys of their extras. */
object PlaybackCommands {
    /** When the sleep timer ends, in epoch milliseconds; 0 when it is off. */
    const val KEY_END_AT = "end_at"

    /** Whether the sleep timer lets the playing song finish first. */
    const val KEY_FINISH_SONG = "finish_song"

    /** Starts the sleep timer from [KEY_END_AT] and [KEY_FINISH_SONG], or stops it. */
    val SLEEP_SET = command("SLEEP_SET")

    /** Returns the sleep timer's [KEY_END_AT] and [KEY_FINISH_SONG]. */
    val SLEEP_GET = command("SLEEP_GET")

    val TOGGLE_SHUFFLE = command("TOGGLE_SHUFFLE")

    val CYCLE_REPEAT = command("CYCLE_REPEAT")

    private fun command(name: String) =
        SessionCommand("dev.jyotiraditya.dmt.command.$name", Bundle.EMPTY)
}
