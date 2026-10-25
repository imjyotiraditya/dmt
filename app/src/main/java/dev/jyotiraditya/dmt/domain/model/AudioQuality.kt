package dev.jyotiraditya.dmt.domain.model

/** A class of audio quality worth a badge on the track that has it. */
enum class AudioQuality {
    LOSSLESS,
    HI_RES_LOSSLESS,
    DOLBY,
    DOLBY_ATMOS,
    ;

    companion object {
        /** The highest sample rate still counted as standard resolution. */
        private const val STANDARD_RATE_HZ = 48_000

        private val LOSSLESS_CODECS =
            listOf("flac", "alac", "wav", "pcm", "raw", "aiff", "ape", "tta", "tak")
        private val DOLBY_CODECS =
            listOf("ac3", "ac-3", "eac3", "ec-3", "ac4", "ac-4", "truehd", "true-hd", "mlp")

        /** The quality of [codec], a MIME type or codec name, or null for plain lossy stereo. */
        fun of(codec: String, sampleRateHz: Int?): AudioQuality? {
            val name = codec.lowercase()
            return when {
                "joc" in name || "ec+3" in name -> DOLBY_ATMOS

                DOLBY_CODECS.any { it in name } -> DOLBY

                "dsd" in name -> HI_RES_LOSSLESS

                LOSSLESS_CODECS.any { it in name } ->
                    if ((sampleRateHz ?: 0) > STANDARD_RATE_HZ) HI_RES_LOSSLESS else LOSSLESS

                else -> null
            }
        }
    }
}

data class TrackTech(val specs: List<Spec>, val quality: AudioQuality?) {
    companion object {
        val None = TrackTech(emptyList(), null)
    }
}
