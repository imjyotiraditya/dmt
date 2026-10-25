package dev.jyotiraditya.dmt.playback

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.jyotiraditya.dmt.library.MetadataReader
import dev.jyotiraditya.dmt.util.tag
import java.io.File
import kotlin.math.pow

private const val REPLAYGAIN_TRACK_GAIN = "REPLAYGAIN_TRACK_GAIN"
private const val R128_TRACK_GAIN = "R128_TRACK_GAIN"

/** R128 gains are stored in 1/256 dB steps. */
private const val R128_STEPS_PER_DB = 256f

/** R128 tags are referenced to -23 LUFS and ReplayGain tags to -18 LUFS. */
private const val R128_TO_REPLAYGAIN_OFFSET_DB = 5f

/** The gain in decibels the track at [path] asks to be played at, or null if it names none. */
@OptIn(UnstableApi::class)
internal suspend fun Context.trackGainDb(path: String): Float? {
    val tags = MetadataReader.readTags(this, Uri.fromFile(File(path)))
    val replayGain =
        tags
            .tag(REPLAYGAIN_TRACK_GAIN)
            ?.replace("dB", "", ignoreCase = true)
            ?.trim()
            ?.toFloatOrNull()
    val r128Gain =
        tags.tag(R128_TRACK_GAIN)?.toIntOrNull()?.let {
            it / R128_STEPS_PER_DB + R128_TO_REPLAYGAIN_OFFSET_DB
        }
    return replayGain ?: r128Gain
}

/** The player volume for a gain of [gainDb], capped at full volume. */
internal fun volumeForGain(gainDb: Float): Float =
    10.0.pow(gainDb / 20.0).toFloat().coerceIn(0f, 1f)
