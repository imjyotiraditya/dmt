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

fun LastSession.resolveQueue(tracks: List<Track>): Triple<List<Track>, Int, Long>? {
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
    return Triple(existing, startIndex, position)
}

suspend fun Context.mediaController(): MediaController =
    MediaController.Builder(
        this,
        SessionToken(this, ComponentName(this, PlaybackService::class.java)),
    ).buildAsync().await()

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
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

fun MediaController.togglePlayPause() {
    if (isPlaying) {
        pause()
    } else {
        if (playbackState == Player.STATE_ENDED) seekToDefaultPosition()
        play()
    }
}

fun MediaController.cycleRepeat() {
    repeatMode = when (repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }
}

data class QueueEntry(val index: Int, val label: String)

fun MediaController.queueEntries(): List<QueueEntry> {
    val timeline = currentTimeline
    if (timeline.isEmpty) return emptyList()

    val window = Timeline.Window()

    return buildList {
        var index = timeline.getFirstWindowIndex(shuffleModeEnabled)

        while (index != C.INDEX_UNSET) {
            timeline.getWindow(index, window)

            val label = window.mediaItem.mediaMetadata.run { "$title · ${artist?.toString().orEmpty().asCredit()}" }
            add(QueueEntry(index, label))

            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
        }
    }
}

fun MediaController.queueWithPosition(): Pair<List<QueueEntry>, Int> {
    val entries = queueEntries()
    return entries to entries.indexOfFirst { it.index == currentMediaItemIndex }
}

@OptIn(UnstableApi::class)
fun Long.asTime(): String =
    Util.getStringForTime(coerceAtLeast(0))

fun String.codecLabel(): String =
    when {
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
fun Format.heAacLabel(): String? =
    when (MimeTypes.getEncoding(sampleMimeType.orEmpty(), codecs)) {
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

fun Tracks.playedAudioFormat(): Format? =
    groups
        .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
        ?.getTrackFormat(0)
