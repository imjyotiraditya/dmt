package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.decoder.ffmpeg.FfmpegLibrary
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.jyotiraditya.dmt.domain.model.AudioQuality
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.model.TrackSource
import dev.jyotiraditya.dmt.domain.model.TrackTech
import dev.jyotiraditya.dmt.library.MetadataReader
import dev.jyotiraditya.dmt.util.asKHz
import dev.jyotiraditya.dmt.util.asMB
import dev.jyotiraditya.dmt.util.codecLabel
import dev.jyotiraditya.dmt.util.heAacLabel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Reports a track's format and decoder, and where the device sends its audio. */
@Singleton
class TrackMediaRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** The format of [track]: from its server for remote tracks, else from [played] or the file. */
    @OptIn(UnstableApi::class)
    suspend fun techSpecs(uri: Uri, track: Track?, played: Format?): TrackTech {
        if (track != null && track.source != TrackSource.LOCAL) {
            val specs = buildList {
                if (track.mime.isNotEmpty()) {
                    add(Spec(label = "FMT", value = track.mime.codecLabel()))
                }
                track.bitDepth?.let { add(Spec(label = "BIT", value = "$it")) }
                track.sampleRateHz?.let { add(Spec(label = "RATE", value = it.asKHz())) }
                if (track.bitrate > 0) {
                    add(Spec(label = "KBPS", value = "${track.bitrate / 1000}", hot = true))
                }
                track.size.takeIf { it > 0 }?.let { add(Spec(label = "SIZE", value = it.asMB())) }
                addAll(decoderSpecs(track.mime))
            }
            return TrackTech(specs, AudioQuality.of(track.mime, track.sampleRateHz))
        }
        val format = played ?: MetadataReader.readFormat(context, uri)
        val mime =
            format?.sampleMimeType?.takeIf { it != MimeTypes.AUDIO_RAW } ?: track?.mime.orEmpty()
        val codec = if (mime == MimeTypes.AUDIO_AAC) format?.heAacLabel() else null
        val bitrate = format?.averageBitrate?.takeIf { it > 0 } ?: track?.bitrate ?: 0
        val maxBitrate = format?.peakBitrate?.takeIf { it > 0 } ?: 0
        val vbr = maxBitrate > bitrate && bitrate > 0
        val sampleRate = format?.sampleRate?.takeIf { it > 0 }
        val channels = format?.channelCount?.takeIf { it > 0 }
        val bits =
            format
                ?.pcmEncoding
                ?.takeIf { it != Format.NO_VALUE && it != C.ENCODING_INVALID }
                ?.let { Util.getByteDepth(it) * C.BITS_PER_BYTE }
        val gapless = (format?.encoderDelay ?: 0) > 0 || (format?.encoderPadding ?: 0) > 0

        val specs = buildList {
            if (mime.isNotEmpty()) add(Spec("FMT", codec ?: mime.codecLabel()))
            bits?.let { add(Spec("BIT", "$it")) }
            sampleRate?.let { add(Spec("RATE", it.asKHz())) }
            channels?.let { add(Spec("CH", if (it == 2) "ST" else "$it")) }
            if (vbr) {
                add(Spec("VBR", "${bitrate / 1000}/${maxBitrate / 1000}", hot = true))
            } else if (bitrate > 0) {
                add(Spec("KBPS", "${bitrate / 1000}", hot = true))
            }
            if (gapless) add(Spec("GAPLESS", "YES"))
            track?.size?.takeIf { it > 0 }?.let { add(Spec("SIZE", it.asMB())) }
            addAll(decoderSpecs(mime))
        }
        return TrackTech(specs, AudioQuality.of(mime + " " + format?.codecs.orEmpty(), sampleRate))
    }

    @OptIn(UnstableApi::class)
    private fun decoderSpecs(mime: String): List<Spec> = buildList {
        if (mime.isEmpty()) return@buildList
        val info =
            MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mime, ignoreCase = true) }
            }
        if (info != null) {
            add(Spec("DEC", info.name))
            add(Spec("HW", if (info.isHardwareAccelerated) "YES" else "NO"))
            add(Spec("IMPL", if (info.isVendor) "VENDOR" else "PLATFORM"))
            val type = info.supportedTypes.first { it.equals(mime, ignoreCase = true) }
            runCatching { info.getCapabilitiesForType(type) }
                .getOrNull()
                ?.maxSupportedInstances
                ?.takeIf { it > 0 }
                ?.let { add(Spec("INST", "$it")) }
        } else if (FfmpegLibrary.isAvailable() && FfmpegLibrary.supportsFormat(mime)) {
            add(Spec("DEC", "MEDIA3 FFMPEG"))
            add(Spec("HW", "NO"))
            add(Spec("IMPL", "BUNDLED"))
        } else {
            add(Spec("DEC", "NONE", hot = true))
        }
    }

    /** The output path and device, again whenever a device connects or disconnects. */
    fun routeSpecs(): Flow<List<Spec>> = callbackFlow {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val callback =
            object : AudioDeviceCallback() {
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
        val outRateHz =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        val outFrames =
            audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull()
        return buildList {
            add(Spec(label = "API", value = "AUDIOTRACK"))
            add(Spec(label = "BIT", value = "16"))
            outRateHz?.let { add(Spec(label = "RATE", value = it.asKHz())) }
            if (outFrames != null && outRateHz != null) {
                val bufMs = outFrames * 1000f / outRateHz
                add(Spec("BUF", "$outFrames FRAMES / %.1fMS".format(Locale.ROOT, bufMs)))
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

    /** The device music plays through, or the likeliest output before Android 13. */
    private fun routedDevice(audioManager: AudioManager): AudioDeviceInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            audioManager.getAudioDevicesForAttributes(MUSIC).firstOrNull()?.let {
                return it
            }
        }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).maxByOrNull {
            it.routePriority()
        }
    }

    private fun deviceSpecs(audioManager: AudioManager): List<Spec> {
        val device = routedDevice(audioManager) ?: return listOf(Spec("VIA", "UNKNOWN"))
        return buildList {
            add(Spec(label = "VIA", value = device.typeLabel()))
            device.productName
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    add(Spec(label = "NAME", value = it))
                }
            device.sampleRates
                .takeIf { it.isNotEmpty() }
                ?.let { rates ->
                    add(
                        Spec(
                            label = "RATES",
                            value = rates.sorted().joinToString("/") { it.asKHz() },
                        ),
                    )
                }
            device.encodings
                .toList()
                .mapNotNull(::encodingLabel)
                .distinct()
                .takeIf { it.isNotEmpty() }
                ?.let { add(Spec(label = "ENC", value = it.joinToString(" "))) }
            device.channelCounts.maxOrNull()?.let {
                add(Spec(label = "CH", value = "$it"))
            }
        }
    }
}

private val MUSIC =
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

private fun AudioDeviceInfo.routePriority(): Int = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    -> 4

    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    -> 3

    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    -> 2

    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> 1

    else -> 0
}

private fun AudioDeviceInfo.typeLabel(): String = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    -> "BLUETOOTH"

    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    -> "WIRED"

    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    -> "USB"

    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"

    else -> productName.toString()
}

private fun encodingLabel(encoding: Int): String? = when (encoding) {
    AudioFormat.ENCODING_PCM_8BIT -> "PCM8"
    AudioFormat.ENCODING_PCM_16BIT -> "PCM16"
    AudioFormat.ENCODING_PCM_24BIT_PACKED -> "PCM24"
    AudioFormat.ENCODING_PCM_32BIT -> "PCM32"
    AudioFormat.ENCODING_PCM_FLOAT -> "FLOAT"
    else -> null
}
