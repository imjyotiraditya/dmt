package dev.jyotiraditya.dmt.presentation.nowplaying.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Spec

/** A fact reported about a track or its output, keyed by the code the data layer uses. */
internal enum class SpecField(val code: String, @param:StringRes val label: Int) {
    FORMAT("FMT", R.string.spec_format),
    BIT_DEPTH("BIT", R.string.spec_bit_depth),
    SAMPLE_RATE("RATE", R.string.spec_sample_rate),
    CHANNELS("CH", R.string.spec_channels),
    BITRATE("KBPS", R.string.spec_bitrate),
    VARIABLE_BITRATE("VBR", R.string.spec_vbr),
    GAPLESS("GAPLESS", R.string.spec_gapless),
    SIZE("SIZE", R.string.spec_size),
    CODEC("DEC", R.string.spec_codec),
    HARDWARE("HW", R.string.spec_hardware),
    PROVIDER("IMPL", R.string.spec_provider),
    INSTANCE("INST", R.string.spec_instance),
    OUTPUT_PATH("API", R.string.spec_output_path),
    BUFFER("BUF", R.string.spec_buffer),
    MODE("FLAGS", R.string.spec_mode),
    ROUTE("VIA", R.string.spec_route),
    DEVICE("NAME", R.string.spec_device),
    SUPPORTED_RATES("RATES", R.string.spec_rates),
    ENCODINGS("ENC", R.string.spec_encodings),
    SPEED("SPEED", R.string.spec_speed),
    ;

    companion object {
        private val byCode = entries.associateBy { it.code }

        fun of(code: String): SpecField? = byCode[code]
    }
}

/** One fact ready to show; [changed] marks an output value that differs from the file's. */
@Immutable
internal data class Fact(val field: SpecField, val value: String, val changed: Boolean = false)

/** A stage the sound passes through on its way from the file to the speaker. */
@Immutable internal data class ChainStage(@param:StringRes val title: Int, val facts: List<Fact>)

private val SIZE_PATTERN = Regex("""([\d.]+)\s*([KMG]B)""", RegexOption.IGNORE_CASE)

private val FILE_FIELDS =
    setOf(
        SpecField.FORMAT,
        SpecField.BIT_DEPTH,
        SpecField.SAMPLE_RATE,
        SpecField.CHANNELS,
        SpecField.BITRATE,
        SpecField.VARIABLE_BITRATE,
        SpecField.GAPLESS,
        SpecField.SIZE,
    )
private val DECODER_FIELDS = setOf(SpecField.CODEC, SpecField.HARDWARE, SpecField.PROVIDER)
private val OUTPUT_FIELDS =
    setOf(
        SpecField.OUTPUT_PATH,
        SpecField.BIT_DEPTH,
        SpecField.SAMPLE_RATE,
        SpecField.BUFFER,
        SpecField.MODE,
    )
private val DEVICE_FIELDS =
    setOf(
        SpecField.ROUTE,
        SpecField.DEVICE,
        SpecField.SUPPORTED_RATES,
        SpecField.ENCODINGS,
        SpecField.CHANNELS,
    )

/** The stages from the track's facts ([tech]) to the output's ([route]), marking resampling. */
internal fun signalChain(tech: List<Spec>, route: List<Spec>, speed: Float): List<ChainStage> {
    val file = tech.facts(FILE_FIELDS)
    val fileValue = file.associate { it.field to it.value }
    val output =
        route.facts(OUTPUT_FIELDS).map { fact ->
            val source = fileValue[fact.field]
            val resampled =
                fact.field in setOf(SpecField.SAMPLE_RATE, SpecField.BIT_DEPTH) &&
                    source != null &&
                    source != fact.value
            if (resampled) fact.copy(value = "$source → ${fact.value}", changed = true) else fact
        }

    return buildList {
        if (file.isNotEmpty()) add(ChainStage(R.string.chain_file, file))
        tech
            .facts(DECODER_FIELDS)
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(R.string.chain_decoder, it)) }
        add(ChainStage(R.string.chain_speed, listOf(Fact(SpecField.SPEED, "%.2f×".format(speed)))))
        if (output.isNotEmpty()) add(ChainStage(R.string.chain_output, output))
        route
            .facts(DEVICE_FIELDS)
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(R.string.chain_device, it)) }
    }
}

private fun List<Spec>.facts(fields: Set<SpecField>): List<Fact> = mapNotNull { spec ->
    val field = SpecField.of(spec.label)?.takeIf { it in fields } ?: return@mapNotNull null
    Fact(field, formatValue(field, spec.value))
}

/** A raw value for display, with units spelled out and all-caps words title-cased. */
internal fun formatValue(field: SpecField, raw: String): String {
    val value = raw.trim()
    return when (field) {
        SpecField.FORMAT -> value.uppercase()

        SpecField.BIT_DEPTH -> if (value.all(Char::isDigit)) "$value-bit" else value.calmed()

        SpecField.SAMPLE_RATE ->
            value.removeSuffix("k").removeSuffix("K").let { if (it != value) "$it kHz" else value }

        SpecField.BITRATE -> "$value kbps"

        SpecField.CHANNELS ->
            when (value.uppercase()) {
                "ST",
                "2",
                -> "Stereo"

                "1",
                "MONO",
                -> "Mono"

                else -> "$value channels"
            }

        SpecField.SIZE ->
            SIZE_PATTERN.replace(value) {
                "${it.groupValues[1]} ${it.groupValues[2].uppercase()}"
            }

        SpecField.CODEC -> codecName(value)

        SpecField.OUTPUT_PATH ->
            if (
                value.equals(
                    "AUDIOTRACK",
                    ignoreCase = true,
                )
            ) {
                "AudioTrack"
            } else {
                value.calmed()
            }

        else -> value.calmed()
    }
}

/** The codec's maker and format, from a decoder id such as `c2.android.mp3.decoder`. */
private fun codecName(id: String): String {
    val lower = id.lowercase()
    return when {
        "ffmpeg" in lower -> "FFmpeg"

        lower.startsWith("c2.android.") ->
            "Android " + lower.removePrefix("c2.android.").substringBefore('.').uppercase()

        lower.startsWith("omx.google.") ->
            "Android " + lower.removePrefix("omx.google.").substringBefore('.').uppercase()

        lower.startsWith("c2.") || lower.startsWith("omx.") -> id

        else -> id.calmed()
    }
}

/** An all-capitals value such as `BUNDLED` as `Bundled`; anything else as it is. */
private fun String.calmed(): String = if (any(Char::isLetter) && this == uppercase()) {
    lowercase().split(' ').joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
} else {
    this
}
