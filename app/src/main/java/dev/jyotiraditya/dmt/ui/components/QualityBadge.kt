package dev.jyotiraditya.dmt.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.AudioQuality

/** A tag naming a track's audio quality, printed reversed: [field] text on [ink]. */
@Composable
fun QualityBadge(quality: AudioQuality, ink: Color, field: Color, modifier: Modifier = Modifier) {
    Tag(text = stringResource(quality.label), color = field, field = ink, modifier = modifier)
}

@get:StringRes
private val AudioQuality.label: Int
    get() =
        when (this) {
            AudioQuality.LOSSLESS -> R.string.quality_lossless
            AudioQuality.HI_RES_LOSSLESS -> R.string.quality_hi_res
            AudioQuality.DOLBY -> R.string.quality_dolby
            AudioQuality.DOLBY_ATMOS -> R.string.quality_dolby_atmos
        }
