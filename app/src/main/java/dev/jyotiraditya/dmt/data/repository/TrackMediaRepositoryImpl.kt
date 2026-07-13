package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.MediaFormatUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository
import dev.jyotiraditya.dmt.util.asKHz
import dev.jyotiraditya.dmt.util.asMB
import dev.jyotiraditya.dmt.util.codecLabel
import dev.jyotiraditya.dmt.util.heAacLabel
import dev.jyotiraditya.dmt.util.probeFrames
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val VBR_PROBE_SKIP = 4
private const val VBR_PROBE_FRAMES = 400

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

    @OptIn(UnstableApi::class)
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
                addAll(decoderSpecs(track.mime))
            }
        }
        var mime = track?.mime.orEmpty()
        var codec: String? = null
        var vbr = false
        var bitrate = track?.bitrate ?: 0
        var maxBitrate = 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null
        var gapless = false
        runCatching {
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)
            val format = extractor.getTrackFormat(0)
            format.getString(MediaFormat.KEY_MIME)?.let {
                if (mime.isEmpty() || it != MimeTypes.AUDIO_RAW) mime = it
            }
            if (mime == MimeTypes.AUDIO_AAC) {
                codec = format.heAacLabel()
            }
            if (mime == MimeTypes.AUDIO_AAC || mime == MimeTypes.AUDIO_MPEG) {
                val frames = extractor.probeFrames(VBR_PROBE_FRAMES)
                val steady = frames.drop(VBR_PROBE_SKIP)
                vbr = steady.size > 1 && steady.max() > steady.min() * 2
                if (vbr && (mime == MimeTypes.AUDIO_MPEG || bitrate <= 0) && extractor.sampleTime > 0) {
                    bitrate = (frames.sum() * 8_000_000L / extractor.sampleTime).toInt()
                }
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
            if (format.containsKey(MediaFormatUtil.KEY_MAX_BIT_RATE)) {
                maxBitrate = format.getInteger(MediaFormatUtil.KEY_MAX_BIT_RATE)
            }
            gapless = format.containsKey(MediaFormat.KEY_ENCODER_DELAY) ||
                    format.containsKey(MediaFormat.KEY_ENCODER_PADDING)
            extractor.release()
        }
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                        ?.toIntOrNull()?.takeIf { it > 0 }?.let { bits = it }
                    if (sampleRate == null) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                            ?.toIntOrNull()?.let { sampleRate = it }
                    }
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
                        value = codec ?: mime.codecLabel(),
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
                        label = if (vbr) "VBR" else "KBPS",
                        value = if (vbr && maxBitrate > bitrate) {
                            "${bitrate / 1000}/${maxBitrate / 1000}"
                        } else {
                            "${bitrate / 1000}"
                        },
                        hot = true,
                    ),
                )
            }
            if (gapless) {
                add(
                    Spec(
                        label = "GAPLESS",
                        value = "YES",
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
            addAll(decoderSpecs(mime))
        }
    }

    @OptIn(UnstableApi::class)
    private fun decoderSpecs(mime: String): List<Spec> = buildList {
        if (mime.isEmpty()) return@buildList
        val info = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .firstOrNull { info ->
                !info.isEncoder &&
                        info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }
        if (info != null) {
            add(
                Spec(
                    label = "DEC",
                    value = info.name,
                ),
            )
            add(
                Spec(
                    label = "HW",
                    value = if (info.isHardwareAccelerated) "YES" else "NO",
                ),
            )
            add(
                Spec(
                    label = "IMPL",
                    value = if (info.isVendor) "VENDOR" else "PLATFORM",
                ),
            )
            val type = info.supportedTypes.first { it.equals(mime, ignoreCase = true) }
            runCatching { info.getCapabilitiesForType(type) }.getOrNull()
                ?.maxSupportedInstances
                ?.takeIf { it > 0 }
                ?.let {
                    add(
                        Spec(
                            label = "INST",
                            value = "$it",
                        ),
                    )
                }
        } else if (FfmpegLibrary.isAvailable() && FfmpegLibrary.supportsFormat(mime)) {
            add(
                Spec(
                    label = "DEC",
                    value = "MEDIA3 FFMPEG",
                ),
            )
            add(
                Spec(
                    label = "HW",
                    value = "NO",
                ),
            )
            add(
                Spec(
                    label = "IMPL",
                    value = "BUNDLED",
                ),
            )
        }
    }

    override fun routeSpecs(): Flow<List<Spec>> = callbackFlow {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val callback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                trySend(currentRouteSpecs(audioManager))
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                trySend(currentRouteSpecs(audioManager))
            }
        }
        audioManager.registerAudioDeviceCallback(callback, null)
        trySend(currentRouteSpecs(audioManager))
        awaitClose { audioManager.unregisterAudioDeviceCallback(callback) }
    }

    private fun currentRouteSpecs(audioManager: AudioManager): List<Spec> {
        val outRateHz = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            ?.toIntOrNull()
        val outFrames = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            ?.toIntOrNull()
        return buildList {
            add(Spec(label = "API", value = "AUDIOTRACK"))
            add(Spec(label = "BIT", value = "16"))
            outRateHz?.let { add(Spec(label = "RATE", value = it.asKHz())) }
            if (outFrames != null && outRateHz != null) {
                val bufMs = outFrames * 1000f / outRateHz
                add(
                    Spec(
                        label = "BUF",
                        value = "$outFrames FRAMES / %.1fMS".format(bufMs),
                    ),
                )
            }
            add(Spec(label = "FLAGS", value = outputFlags()))
            addAll(deviceSpecs(audioManager))
        }
    }

    private fun outputFlags(): String {
        val packageManager = context.packageManager
        val flags = buildList {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY)) {
                add("LOW-LATENCY")
            }
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO)) {
                add("PRO-AUDIO")
            }
        }
        return if (flags.isEmpty()) "NONE" else flags.joinToString(" ")
    }

    private fun deviceSpecs(audioManager: AudioManager): List<Spec> {
        val device = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .maxByOrNull { it.routePriority() }
            ?: return listOf(Spec(label = "VIA", value = "UNKNOWN"))
        return buildList {
            add(Spec(label = "VIA", value = device.typeLabel()))
            device.productName?.toString()?.takeIf { it.isNotBlank() }?.let {
                add(Spec(label = "NAME", value = it))
            }
            device.sampleRates.takeIf { it.isNotEmpty() }?.let { rates ->
                add(
                    Spec(
                        label = "RATES",
                        value = rates.sorted().joinToString("/") { it.asKHz() },
                    ),
                )
            }
            device.encodings.toList().mapNotNull(::encodingLabel).distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { add(Spec(label = "ENC", value = it.joinToString(" "))) }
            device.channelCounts.maxOrNull()?.let {
                add(Spec(label = "CH", value = "$it"))
            }
        }
    }
}

private fun AudioDeviceInfo.routePriority(): Int =
    when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> 4
        AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET -> 3
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> 2
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 1
        else -> 0
    }

private fun AudioDeviceInfo.typeLabel(): String =
    when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLUETOOTH"

        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            -> "WIRED"

        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
            -> "USB"

        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"

        else -> productName.toString()
    }

private fun encodingLabel(encoding: Int): String? =
    when (encoding) {
        AudioFormat.ENCODING_PCM_8BIT -> "PCM8"
        AudioFormat.ENCODING_PCM_16BIT -> "PCM16"
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM24"
        AudioFormat.ENCODING_PCM_32BIT -> "PCM32"
        AudioFormat.ENCODING_PCM_FLOAT -> "FLOAT"
        else -> null
    }
