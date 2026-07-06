package dev.jyotiraditya.dmt.util

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.playback.PlaybackService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> ListenableFuture<T>.await(): T =
    suspendCancellableCoroutine { cont ->
        addListener(
            {
                runCatching { get() }.fold(cont::resume) { cont.resumeWithException(it) }
            },
            Runnable::run,
        )
        cont.invokeOnCancellation { cancel(false) }
    }

suspend fun Context.mediaController(): MediaController =
    MediaController.Builder(
        this,
        SessionToken(this, ComponentName(this, PlaybackService::class.java)),
    ).buildAsync().await()

private val albumArtBase: Uri = "content://media/external/audio/albumart".toUri()

fun Track.albumArtUri(): Uri = ContentUris.withAppendedId(albumArtBase, albumId)

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(albumArtUri())
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

fun Int.asKHz(): String = if (this % 1000 == 0) "${this / 1000}K" else "%.1fK".format(this / 1000f)

fun Long.asMB(): String = "%.1fMB".format(this / 1048576f)
