package dev.jyotiraditya.dmt.util

import android.content.ComponentName
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.jyotiraditya.dmt.domain.model.LastSession
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.playback.PlaybackService
import kotlinx.coroutines.guava.await
import java.nio.ByteBuffer

const val QUEUE_CAP = 500
private const val QUEUE_LOOKBACK = 100

fun windowQueue(list: List<Track>, index: Int): Pair<List<Track>, Int> {
    if (list.size <= QUEUE_CAP) return list to index
    val start = (index - QUEUE_LOOKBACK)
        .coerceAtLeast(0)
        .coerceAtMost(list.size - QUEUE_CAP)
    return list.subList(start, start + QUEUE_CAP).toList() to (index - start)
}

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

fun MediaController.queueLabels(): List<String> =
    (0 until mediaItemCount).map { i ->
        getMediaItemAt(i).mediaMetadata.run { "$title · $artist" }
    }

fun Long.asTime(): String {
    val totalSeconds = (this / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

fun String.codecLabel(): String =
    when {
        contains("flac", true) -> "FLAC"
        contains("mpeg", true) || contains("mp3", true) -> "MP3"
        contains("mp4a", true) || contains("aac", true) || contains("mp4", true) -> "AAC"
        contains("opus", true) -> "OPUS"
        contains("vorbis", true) -> "VORBIS"
        contains("ogg", true) -> "OGG"
        contains("wav", true) -> "WAV"
        contains("raw", true) -> "PCM"
        contains("aiff", true) -> "AIFF"
        contains("dsd", true) -> "DSD"
        else -> substringAfterLast('/').uppercase().take(8)
    }

fun MediaFormat.heAacLabel(): String? =
    getByteBuffer("csd-0")?.heAacLabel()

private fun ByteBuffer.heAacLabel(): String? {
    val base = position()
    if (remaining() < 1) return null

    return when ((get(base).toInt() and 0xFF) ushr 3) {
        5 -> "HE-AAC"

        29 -> "HE-AACv2"

        2 -> {
            if (remaining() < 5) return null
            val sync = ((get(base + 2).toInt() and 0xFF) shl 3) or
                    ((get(base + 3).toInt() and 0xFF) ushr 5)
            val extType = get(base + 3).toInt() and 0x1F
            val sbr = get(base + 4).toInt() and 0x80 != 0
            if (sync == 0x2B7 && extType == 5 && sbr) "HE-AAC" else null
        }

        else -> null
    }
}

fun MediaExtractor.probeFrames(limit: Int): List<Int> = runCatching {
    selectTrack(0)
    buildList {
        while (size < limit) {
            val bytes = sampleSize.toInt()
            if (bytes <= 0) break
            add(bytes)
            if (!advance()) break
        }
    }
}.getOrDefault(emptyList())

fun Int.asKHz(): String = if (this % 1000 == 0) "${this / 1000}K" else "%.1fK".format(this / 1000.0)

fun Long.asMB(): String = "%.1fMB".format(this / 1048576f)
