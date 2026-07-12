package dev.jyotiraditya.dmt.core.common

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
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import java.util.Locale
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor

private const val RAMP =
    " .'`^\":;Il!i~+_-?][}{1)(|/tfjrxnuvczXYUJCLQ0OZmwqpdbkhao*#MW&8%B@\$"

private const val CELL_ASPECT = 0.6f
private const val GLYPH_SIZE = 24f
private const val BASELINE_SHIFT = 0.22f

private fun gammaLift(channel: Int): Int {
    val normalized = channel / 255f
    val lifted = 255f * normalized.pow(0.7f)

    return lifted.roundToInt()
}

private fun symbolFor(intensity: Float): Char {
    val lastIndex = RAMP.length - 1
    val index = (intensity * lastIndex)
        .roundToInt()
        .coerceIn(0, lastIndex)

    return RAMP[index]
}

private fun renderAsciiGrid(
    cols: Int,
    rows: Int,
    cell: (x: Int, y: Int, paint: Paint) -> Char,
): Bitmap {
    val paint = Paint().apply {
        typeface = Typeface.MONOSPACE
        isAntiAlias = true
        textSize = GLYPH_SIZE
    }
    val advance = paint.measureText("M")
    val lineH = paint.textSize
    val width = (cols * advance).roundToInt()
    val height = (rows * lineH).roundToInt()

    val out = createBitmap(width, height)
    val canvas = AndroidCanvas(out)
    val glyph = CharArray(1)

    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val symbol = cell(x, y, paint)
            if (symbol == ' ') continue

            glyph[0] = symbol
            val textX = x * advance
            val baselineY = (y + 1) * lineH - lineH * BASELINE_SHIFT
            canvas.drawText(glyph, 0, 1, textX, baselineY, paint)
        }
    }

    return out
}

fun Bitmap.toAsciiBitmap(cols: Int = 96): Bitmap {
    val mutable = false
    val safe = if (config == Bitmap.Config.HARDWARE) {
        copy(Bitmap.Config.ARGB_8888, mutable)
    } else {
        this
    }
    val rawRows = cols.toFloat() * safe.height / safe.width * CELL_ASPECT
    val rows = rawRows.roundToInt().coerceIn(8, 96)
    val small = safe
        .scale(width = cols * 2, height = rows * 2)
        .scale(width = cols, height = rows)

    return renderAsciiGrid(cols, rows) { x, y, paint ->
        val pixel = small[x, y]
        val red = gammaLift(AndroidColor.red(pixel))
        val green = gammaLift(AndroidColor.green(pixel))
        val blue = gammaLift(AndroidColor.blue(pixel))
        val lum = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
        val symbol = symbolFor(lum)

        if (symbol != ' ') {
            paint.color = AndroidColor.rgb(red, green, blue)
        }

        symbol
    }
}

fun asciiDebugInfo(raw: Bitmap?, ascii: Bitmap): String {
    val paint = Paint().apply {
        typeface = Typeface.MONOSPACE
        textSize = GLYPH_SIZE
    }
    val advance = paint.measureText("M") / paint.textSize
    val src = raw?.let { "${it.width}x${it.height}" } ?: "none"

    return "adv=%.3f want=%.3f src=%s out=%dx%d".format(
        Locale.US,
        advance,
        CELL_ASPECT,
        src,
        ascii.width,
        ascii.height,
    )
}

fun generateAsciiPlaceholder(seed: Long, cols: Int = 96): Bitmap {
    val rows = (cols * CELL_ASPECT).roundToInt()
    val random = Random(seed)
    val f1 = 0.10f + random.nextFloat() * 0.25f
    val f2 = 0.10f + random.nextFloat() * 0.25f
    val f3 = 0.05f + random.nextFloat() * 0.15f
    val p1 = random.nextFloat() * 6.28f
    val p2 = random.nextFloat() * 6.28f
    val p3 = random.nextFloat() * 6.28f
    val hue = random.nextFloat() * 360f
    val hsv = FloatArray(3)

    return renderAsciiGrid(cols, rows) { x, y, paint ->
        val v = (
                sin(x * f1 + p1) +
                        sin(y * f2 + p2) +
                        sin((x + y) * f3 + p3) +
                        3f
                ) / 6f
        val symbol = symbolFor(v)

        if (symbol != ' ') {
            hsv[0] = hue
            hsv[1] = 0.30f
            hsv[2] = 0.20f + v * 0.45f
            paint.color = AndroidColor.HSVToColor(hsv)
        }

        symbol
    }
}

@Composable
fun AsciiCover(
    cover: Bitmap,
    playing: Boolean,
    modifier: Modifier = Modifier,
    wave: Boolean = true,
) {
    val image = remember(cover) { cover.asImageBitmap() }
    val aspect = cover.width.toFloat() / cover.height
    if (playing && wave) {
        WaveCover(image, aspect, modifier)
    } else {
        Canvas(modifier = modifier.aspectRatio(aspect)) {
            drawImage(
                image = image,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

@Composable
private fun WaveCover(
    image: ImageBitmap,
    aspect: Float,
    modifier: Modifier,
) {
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
            },
        ),
        label = "phase",
    )
    Canvas(
        modifier = modifier
            .aspectRatio(aspect)
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawImage(
            image = image,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
            filterQuality = FilterQuality.Medium,
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
                end = Offset(cx + band, size.height * 0.45f),
            ),
            blendMode = BlendMode.SrcAtop,
        )
    }
}
