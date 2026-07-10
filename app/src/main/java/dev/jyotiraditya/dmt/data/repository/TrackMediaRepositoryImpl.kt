package dev.jyotiraditya.dmt.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.media3.common.MimeTypes
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
        var codec: String? = null
        var vbr = false
        var bitrate = track?.bitrate ?: 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var bits: Int? = null
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
}
