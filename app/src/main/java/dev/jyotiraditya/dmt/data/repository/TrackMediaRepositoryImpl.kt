package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.AudioJourney
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.asKHz
import dev.jyotiraditya.dmt.util.asMB
import dev.jyotiraditya.dmt.util.asTime
import dev.jyotiraditya.dmt.util.codecLabel
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackMediaRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : TrackMediaRepository {

    override fun loadArt(uri: Uri): Bitmap? =
        if (uri.scheme == "http" || uri.scheme == "https") {
            runCatching {
                URL(uri.toString()).openStream().use(BitmapFactory::decodeStream)
            }.getOrNull()
        } else {
            runCatching {
                context.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            }.recoverCatching {
                context.contentResolver.openInputStream(uri).use(BitmapFactory::decodeStream)
            }.getOrNull()
        }

    override fun techSpecs(uri: Uri, track: Track?): List<Spec> {
        if (track?.source == TrackSource.JELLYFIN) {
            return buildList {
                if (track.mime.isNotEmpty()) {
                    add(Spec(label = "FMT", value = track.mime.codecLabel()))
                }
                if (track.bitrate > 0) {
                    add(Spec(label = "KBPS", value = "${track.bitrate / 1000}", hot = true))
                }
                track.size.takeIf { it > 0 }?.let { add(Spec(label = "SIZE", value = it.asMB())) }
            }
        }
        var mime = track?.mime.orEmpty()
        var bitrate = track?.bitrate ?: 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            val format = extractor.getTrackFormat(0)
            format.getString(MediaFormat.KEY_MIME)?.let {
                if (mime.isEmpty() || mime == "audio/?") mime = it
            }
            if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            }
            if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            }
            if (bitrate <= 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
            }
            extractor.release()
        }
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                    ?.toIntOrNull()?.takeIf { it > 0 }?.let { bits = it }
                if (sampleRate == null) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                        ?.toIntOrNull()?.let { sampleRate = it }
                }
                if (bitrate <= 0) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                        ?.toIntOrNull()?.let { bitrate = it }
                }
            }
        }
        return buildList {
            if (mime.isNotEmpty()) {
                add(
                    Spec(
                        label = "FMT",
                        value = mime.codecLabel(),
                    ),
                )
            }
            bits?.let {
                add(
                    Spec(
                        label = "BIT",
                        value = "$it",
                    ),
                )
            }
            sampleRate?.let {
                add(
                    Spec(
                        label = "RATE",
                        value = it.asKHz(),
                    ),
                )
            }
            channels?.let {
                add(
                    Spec(
                        label = "CH",
                        value = if (it == 2) "ST" else "$it",
                    ),
                )
            }
            if (bitrate > 0) {
                add(
                    Spec(
                        label = "KBPS",
                        value = "${bitrate / 1000}",
                        hot = true,
                    ),
                )
            }
            track?.size?.takeIf { it > 0 }?.let {
                add(
                    Spec(
                        label = "SIZE",
                        value = it.asMB(),
                    ),
                )
            }
        }
    }

    override fun audioJourney(
        uri: Uri,
        track: Track?,
        speed: Float,
        audioSessionId: Int,
    ): AudioJourney {
        var mime = track?.mime.orEmpty()
        var bitrate = track?.bitrate ?: 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null
        var hasGaplessData = false

        if (track?.source != TrackSource.JELLYFIN) {
            runCatching {
                val extractor = MediaExtractor()
                extractor.setDataSource(context, uri, null)
                val format = extractor.getTrackFormat(0)
                format.getString(MediaFormat.KEY_MIME)?.let {
                    if (mime.isEmpty() || mime == "audio/?") mime = it
                }
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                if (bitrate <= 0 && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    hasGaplessData = format.containsKey(MediaFormat.KEY_ENCODER_DELAY) ||
                        format.containsKey(MediaFormat.KEY_ENCODER_PADDING)
                }
                extractor.release()
            }
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                        ?.toIntOrNull()?.takeIf { it > 0 }?.let { bits = it }
                    if (sampleRate == null) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                            ?.toIntOrNull()?.let { sampleRate = it }
                    }
                    if (bitrate <= 0) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.toIntOrNull()?.let { bitrate = it }
                    }
                }
            }
        }

        val trackSpecs = buildList {
            if (mime.isNotEmpty()) add(Spec("FORMAT", mime.codecLabel()))
            if (bitrate > 0) add(Spec("BITRATE", "${bitrate / 1000} kbps", hot = true))
            sampleRate?.let { add(Spec("SAMPLE RATE", it.asKHz())) }
            bits?.let { add(Spec("BIT DEPTH", "${it}-bit")) }
            channels?.let { add(Spec("CHANNELS", if (it == 2) "stereo" else "$it ch")) }
            if (hasGaplessData) add(Spec("GAPLESS", "encoded"))
        }

        val decoderSpecs = decoderInfo(mime)?.let { (name, hardware) ->
            buildList {
                add(Spec("DECODER", name))
                add(Spec("TYPE", if (hardware) "hardware" else "software"))
            }
        }.orEmpty()

        val processingSpecs = buildList {
            if (kotlin.math.abs(speed - 1f) > 0.01f) {
                add(Spec("SPEED", "%.2fx".format(speed), hot = true))
            }
        }

        val outputSpecs = buildOutputSpecs()
        val outputDeviceSpecs = buildOutputDeviceSpecs()

        val advancedSpecs = buildList {
            track?.path?.takeIf { it.isNotBlank() }?.let { add(Spec("PATH", it)) }
            track?.durationMs?.takeIf { it > 0 }?.let { add(Spec("DURATION", it.asTime())) }
            track?.size?.takeIf { it > 0 }?.let { add(Spec("FILE SIZE", it.asMB())) }
            if (mime.isNotEmpty()) add(Spec("MIME TYPE", mime))
            if (audioSessionId != 0) add(Spec("AUDIO SESSION", "$audioSessionId"))
        }

        return AudioJourney(
            track = trackSpecs,
            decoder = decoderSpecs,
            processing = processingSpecs,
            output = outputSpecs,
            outputDevice = outputDeviceSpecs,
            advanced = advancedSpecs,
        )
    }

    /** First platform decoder that declares support for [mime], and whether it's HW-accelerated. */
    private fun decoderInfo(mime: String): Pair<String, Boolean>? {
        if (mime.isBlank()) return null
        return runCatching {
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .asSequence()
                .filter { !it.isEncoder }
                .firstOrNull { info ->
                    runCatching { info.getCapabilitiesForType(mime) }.getOrNull() != null
                }
                ?.let { info ->
                    val hardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        false
                    }
                    info.name to hardware
                }
        }.getOrNull()
    }

    /** Native output sample rate this device actually renders at — real, not per-track. */
    private fun buildOutputSpecs(): List<Spec> {
        val audioManager = context.getSystemService<AudioManager>() ?: return emptyList()
        val rate = runCatching {
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        }.getOrNull()
        return buildList {
            add(Spec("AUDIOTRACK", "PCM"))
            rate?.let { add(Spec("OUTPUT RATE", it.asKHz())) }
        }
    }

    /** Best-effort detection of the currently active output route. No fabricated codec info. */
    private fun buildOutputDeviceSpecs(): List<Spec> {
        val audioManager = context.getSystemService<AudioManager>() ?: return emptyList()
        val devices = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull().orEmpty()
        if (devices.isEmpty()) return emptyList()

        val priority = listOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        )
        val active = priority.firstNotNullOfOrNull { type -> devices.firstOrNull { it.type == type } }
            ?: devices.first()

        val label = when (active.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "bluetooth"
            AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "usb dac"
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            -> "wired headphones"
            AudioDeviceInfo.TYPE_HDMI -> "hdmi"
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "speaker"
            else -> "unknown"
        }
        return buildList {
            add(Spec("DEVICE", label, hot = true))
            active.productName?.toString()?.takeIf { it.isNotBlank() && it != "?" }?.let {
                add(Spec("NAME", it))
            }
        }
    }
}
