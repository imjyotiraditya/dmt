package dev.jyotiraditya.dmt.presentation.common

import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.data.repository.PlaybackRepository
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.presentation.main.Notices
import javax.inject.Inject

/** What any list of tracks offers: play, shuffle, queue them, or play them next. */
interface TrackListActions {
    fun play(tracks: List<Track>, index: Int)

    fun shuffle(tracks: List<Track>)

    /** Adds [tracks] to the end of the queue, naming them [label] in the notice. */
    fun enqueue(tracks: List<Track>, label: String)

    /** Plays [tracks] after the one playing, naming them [label] in the notice. */
    fun playNext(tracks: List<Track>, label: String)
}

/** [TrackListActions] on the shared player, confirming queue changes with a notice. */
class TrackActions
@Inject
constructor(
    private val playback: PlaybackRepository,
    private val notices: Notices,
) : TrackListActions {

    override fun play(tracks: List<Track>, index: Int) = playback.play(tracks, index)

    override fun shuffle(tracks: List<Track>) = playback.shufflePlay(tracks)

    override fun enqueue(tracks: List<Track>, label: String) {
        if (playback.enqueue(tracks)) notices.notify(UiText(R.string.notice_queued, label))
    }

    override fun playNext(tracks: List<Track>, label: String) {
        if (playback.playNext(tracks)) notices.notify(UiText(R.string.notice_playing_next, label))
    }
}
