package dev.jyotiraditya.dmt.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.get
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import android.graphics.Color as PlatformColor

private const val SAMPLE_PX = 28
private const val HUE_BINS = 24

/** This bitmap in a config whose pixels can be read and scaled. */
fun Bitmap.software(): Bitmap =
    if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this

/** This bitmap scaled down so its longer side is at most [max] pixels. */
fun Bitmap.fitWithin(max: Int): Bitmap {
    val soft = software()
    val longer = maxOf(soft.width, soft.height)
    if (longer <= max) return soft
    val scale = max.toFloat() / longer
    return soft.scale(
        width = (soft.width * scale).toInt().coerceAtLeast(1),
        height = (soft.height * scale).toInt().coerceAtLeast(1),
    )
}

/** A grey as bright as a cover with no clear colour. */
internal fun neutralField(brightness: Float): Color {
    val grey = brightness.coerceIn(0.14f, 0.82f)
    return Color(grey, grey, grey)
}

/** The cover's dominant hue as a fill colour, or a grey when it has none. */
suspend fun Bitmap.fieldColor(): Color = withContext(Dispatchers.Default) {
    val sample = software().scale(SAMPLE_PX, SAMPLE_PX)
    val weight = FloatArray(HUE_BINS)
    val sumR = FloatArray(HUE_BINS)
    val sumG = FloatArray(HUE_BINS)
    val sumB = FloatArray(HUE_BINS)
    val hsv = FloatArray(3)
    var total = 0f
    var brightness = 0f
    for (y in 0 until sample.height) {
        for (x in 0 until sample.width) {
            val p = sample[x, y]
            PlatformColor.colorToHSV(p, hsv)
            val s = hsv[1]
            val v = hsv[2]
            brightness += v
            if (v < 0.15f || s < 0.14f) continue
            // Favour saturated mid-tones over pale or dark pixels.
            val w = s * s * v * (1.2f - abs(v - 0.62f))
            val bin = ((hsv[0] / 360f) * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
            weight[bin] += w
            sumR[bin] += PlatformColor.red(p) * w
            sumG[bin] += PlatformColor.green(p) * w
            sumB[bin] += PlatformColor.blue(p) * w
            total += w
        }
    }
    if (total < 5f) return@withContext neutralField(brightness / (sample.width * sample.height))

    // Count neighbouring hues too, so a hue split across two bins can still win.
    val best =
        (0 until HUE_BINS).maxBy {
            weight[it] +
                0.5f * (weight[(it + 1) % HUE_BINS] + weight[(it + HUE_BINS - 1) % HUE_BINS])
        }
    val w = weight[best].coerceAtLeast(0.0001f)
    val mixed =
        PlatformColor.rgb(
            (sumR[best] / w).toInt().coerceIn(0, 255),
            (sumG[best] / w).toInt().coerceIn(0, 255),
            (sumB[best] / w).toInt().coerceIn(0, 255),
        )
    PlatformColor.colorToHSV(mixed, hsv)
    hsv[1] = hsv[1].coerceIn(0.35f, 0.72f)
    hsv[2] = hsv[2].coerceIn(0.32f, 0.72f)
    Color(PlatformColor.HSVToColor(hsv))
}
