package dev.jyotiraditya.dmt.util

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.asCredit
import dev.jyotiraditya.dmt.playback.PlaybackService
import kotlinx.coroutines.guava.await
import kotlin.time.Duration.Companion.milliseconds

data class ResolvedQueue(val tracks: List<Track>, val index: Int, val positionMs: Long)

/**
 * The saved queue with tracks no longer in [tracks] dropped; starts over at the first track when
 * the saved one is gone.
 */
fun LastSession.resolveQueue(tracks: List<Track>): ResolvedQueue? {
    val byId = tracks.associateBy { it.id }
    val existing = queueIds.mapNotNull { byId[it] }
    if (existing.isEmpty()) return null

    val savedCurrentId = queueIds.getOrNull(index)
    var startIndex = existing.indexOfFirst { it.id == savedCurrentId }
    var position = positionMs
    if (startIndex < 0) {
        startIndex = 0
        position = 0L
    }
    return ResolvedQueue(existing, startIndex, position)
}

suspend fun Context.mediaController(): MediaController = MediaController.Builder(
    this,
    SessionToken(this, ComponentName(this, PlaybackService::class.java)),
)
    .buildAsync()
    .await()

/** An id-only item that the service resolves to the full track. */
fun Track.toQueueRequest(): MediaItem = MediaItem.Builder().setMediaId(id.toString()).build()

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(uri)
    .apply {
        if (clipStartMs != null || clipEndMs != null) {
            setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clipStartMs ?: 0L)
                    .apply { clipEndMs?.let { setEndPositionMs(it) } }
                    .build(),
            )
        }
    }
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(coverUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .build(),
    )
    .build()

/** Moves repeat from off to all to one and back to off. */
fun Player.cycleRepeat() {
    repeatMode =
        when (repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
}

/** The first value of tag [name], ignoring case. */
fun Map<String, List<String>>.tag(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

/** A queued item; [index] is its place in the player's item list, not the play order. */
data class QueueEntry(val index: Int, val mediaId: String, val title: String, val artist: String)

/** The queue in play order, following shuffle. */
fun Player.queueEntries(): List<QueueEntry> {
    val timeline = currentTimeline
    if (timeline.isEmpty) return emptyList()

    val window = Timeline.Window()

    return buildList {
        var index = timeline.getFirstWindowIndex(shuffleModeEnabled)

        while (index != C.INDEX_UNSET) {
            timeline.getWindow(index, window)

            val item = window.mediaItem
            add(
                QueueEntry(
                    index = index,
                    mediaId = item.mediaId,
                    title = item.mediaMetadata.title?.toString().orEmpty(),
                    artist = item.mediaMetadata.artist?.toString().orEmpty().asCredit(),
                ),
            )

            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
        }
    }
}

data class QueueSnapshot(val entries: List<QueueEntry>, val position: Int)

fun Player.queueWithPosition(): QueueSnapshot {
    val entries = queueEntries()
    return QueueSnapshot(entries, entries.indexOfFirst { it.index == currentMediaItemIndex })
}

@OptIn(UnstableApi::class)
fun Long.asTime(): String = Util.getStringForTime(coerceAtLeast(0))

fun Long.asHoursMinutes(): String {
    val minutes = coerceAtLeast(0).milliseconds.inWholeMinutes
    val hours = minutes / 60
    return if (hours > 0) "${hours}h ${minutes % 60}m" else "${minutes}m"
}

fun String.codecLabel(): String = when {
    contains("flac", true) -> "FLAC"
    contains("mpeg", true) || contains("mp3", true) -> "MP3"
    contains("mp4a", true) || contains("aac", true) || contains("mp4", true) -> "AAC"
    contains("opus", true) -> "OPUS"
    contains("vorbis", true) -> "VORBIS"
    contains("ogg", true) -> "OGG"
    contains("wavpack", true) -> "WAVPACK"
    contains("wav", true) -> "WAV"
    contains("ape", true) -> "APE"
    contains("tta", true) -> "TTA"
    contains("tak", true) -> "TAK"
    contains("raw", true) -> "PCM"
    contains("aiff", true) -> "AIFF"
    contains("dsd", true) -> "DSD"
    contains("dts", true) -> "DTS"
    contains("ac4", true) -> "AC-4"
    contains("eac3", true) -> "E-AC-3"
    contains("ac3", true) -> "AC-3"
    contains("true-hd", true) -> "TRUEHD"
    else -> substringAfterLast('/').uppercase().take(8)
}

@OptIn(UnstableApi::class)
fun Format.heAacLabel(): String? = when (MimeTypes.getEncoding(sampleMimeType.orEmpty(), codecs)) {
    C.ENCODING_AAC_HE_V2 -> "HE-AACv2"
    C.ENCODING_AAC_HE_V1 -> "HE-AAC"
    else -> null
}

fun Int.asKHz(): String {
    if (this % 1000 == 0) return "${this / 1000}K"
    val tenths = Math.round(this / 100.0)
    return "${tenths / 10}.${tenths % 10}K"
}

fun Long.asMB(): String {
    val tenths = Math.round(this / 1048576.0 * 10)
    return "${tenths / 10}.${tenths % 10}MB"
}

/** The format of the audio track being played. */
fun Tracks.playedAudioFormat(): Format? =
    groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }?.getTrackFormat(0)
