package dev.jyotiraditya.dmt.util

import androidx.media3.common.Player
import dev.jyotiraditya.dmt.domain.model.Track

fun Player.playTracks(tracks: List<Track>, index: Int) {
    setMediaItems(tracks.map { it.toQueueRequest() }, index, 0L)
    prepare()
    play()
}

fun Player.enqueue(tracks: List<Track>) {
    addMediaItems(tracks.map { it.toQueueRequest() })
    prepare()
}

fun Player.playNext(tracks: List<Track>) {
    val at = if (mediaItemCount == 0) 0 else currentMediaItemIndex + 1
    addMediaItems(at, tracks.map { it.toQueueRequest() })
    prepare()
}

fun Player.moveInQueue(from: Int, to: Int) {
    val valid = 0 until mediaItemCount
    if (from in valid && to in valid) moveMediaItem(from, to)
}

fun Player.removeFromQueue(index: Int) {
    if (index in 0 until mediaItemCount) removeMediaItem(index)
}

/** Removes every entry of [queue] after [position], last first so the indices stay valid. */
fun Player.clearUpcoming(queue: List<QueueEntry>, position: Int) {
    queue.drop(position + 1).map { it.index }.sortedDescending().forEach(::removeMediaItem)
}

fun Player.jumpTo(index: Int) {
    seekTo(index, 0L)
    prepare()
    play()
}
