package dev.jyotiraditya.dmt.ui

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor

private const val RAMP =
    " .'`^\":;Il!i~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"

fun Bitmap.toAsciiBitmap(cols: Int = 64): Bitmap {
    val safe = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this
    val rows = (cols.toFloat() * safe.height / safe.width * 0.6f).roundToInt().coerceIn(8, 96)
    val small = Bitmap.createScaledBitmap(safe, cols, rows, true)
    val paint = Paint().apply {
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textSize = 24f
    }
    val advance = paint.measureText("M")
    val lineH = paint.textSize
    val out = Bitmap.createBitmap(
        (cols * advance).roundToInt(),
        (rows * lineH).roundToInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = AndroidCanvas(out)
    val hsv = FloatArray(3)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val pixel = small.getPixel(x, y)
            val lum = (0.299f * AndroidColor.red(pixel) +
                0.587f * AndroidColor.green(pixel) +
                0.114f * AndroidColor.blue(pixel)) / 255f
            val symbol = RAMP[(lum * (RAMP.length - 1)).roundToInt().coerceIn(0, RAMP.length - 1)]
            if (symbol == ' ') continue
            AndroidColor.colorToHSV(pixel, hsv)
            hsv[1] = (hsv[1] * 1.25f).coerceAtMost(1f)
            hsv[2] = 0.3f + hsv[2] * 0.7f
            paint.color = AndroidColor.HSVToColor(hsv)
            canvas.drawText(symbol.toString(), x * advance, (y + 1) * lineH - lineH * 0.22f, paint)
        }
    }
    return out
}

fun generateAsciiPlaceholder(seed: Long, cols: Int = 64): Bitmap {
    val rows = (cols * 0.6f).roundToInt()
    val random = Random(seed)
    val f1 = 0.10f + random.nextFloat() * 0.25f
    val f2 = 0.10f + random.nextFloat() * 0.25f
    val f3 = 0.05f + random.nextFloat() * 0.15f
    val p1 = random.nextFloat() * 6.28f
    val p2 = random.nextFloat() * 6.28f
    val p3 = random.nextFloat() * 6.28f
    val hue = random.nextFloat() * 360f

    val paint = Paint().apply {
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textSize = 24f
    }
    val advance = paint.measureText("M")
    val lineH = paint.textSize
    val out = Bitmap.createBitmap(
        (cols * advance).roundToInt(),
        (rows * lineH).roundToInt(),
        Bitmap.Config.ARGB_8888
    )
    val canvas = AndroidCanvas(out)
    val hsv = FloatArray(3)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val v = (
                sin(x * f1 + p1) +
                    sin(y * f2 + p2) +
                    sin((x + y) * f3 + p3) +
                    3f
                ) / 6f
            val symbol = RAMP[(v * (RAMP.length - 1)).roundToInt().coerceIn(0, RAMP.length - 1)]
            if (symbol == ' ') continue
            hsv[0] = hue
            hsv[1] = 0.30f
            hsv[2] = 0.20f + v * 0.45f
            paint.color = AndroidColor.HSVToColor(hsv)
            canvas.drawText(symbol.toString(), x * advance, (y + 1) * lineH - lineH * 0.22f, paint)
        }
    }
    return out
}

@Composable
fun AsciiCover(
    cover: Bitmap,
    playing: Boolean,
    wave: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val image = remember(cover) { cover.asImageBitmap() }
    val aspect = cover.width.toFloat() / cover.height
    if (playing && wave) {
        WaveCover(image, aspect, modifier)
    } else {
        Canvas(modifier = modifier.fillMaxWidth().aspectRatio(aspect)) {
            drawImage(
                image = image,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium
            )
        }
    }
}

@Composable
private fun WaveCover(image: ImageBitmap, aspect: Float, modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 4400
                0f at 0
                1f at 3000 using LinearEasing
                1f at 4400
            }
        ),
        label = "phase"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspect)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawImage(
            image = image,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium
        )
        val cx = (-0.6f + phase * 2.8f) * size.width
        val band = size.width * 0.22f
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.55f),
                    Color.Transparent,
                ),
                start = Offset(cx - band, 0f),
                end = Offset(cx + band, size.height * 0.45f)
            ),
            blendMode = BlendMode.SrcAtop
        )
    }
}
