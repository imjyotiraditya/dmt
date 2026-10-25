package dev.jyotiraditya.dmt.presentation.nowplaying.lyrics

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import dev.jyotiraditya.lyrics.LyricWord

/** Width of the soft edge at the sweep front, as a fraction of the font size. */
private const val FEATHER_EM = 0.55f

/** Text that fills with [sung] as [position] moves through its [words], read while drawing. */
@Composable
internal fun SweepText(
    text: String,
    words: List<LyricWord>,
    position: State<Long>,
    style: TextStyle,
    sung: Color,
    unsung: Color,
    modifier: Modifier = Modifier,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val path =
        remember(layout, words) {
            layout?.let { SweepPath.of(text, words, LayoutGeometry(it)) } ?: SweepPath.Empty
        }

    BasicText(
        text = text,
        style = style.copy(color = unsung),
        onTextLayout = { layout = it },
        modifier =
            modifier.drawWithCache {
                val feather = style.fontSize.toPx() * FEATHER_EM
                // One gradient per direction, moved to the front with a translation each frame.
                val fadeOut =
                    Brush.horizontalGradient(
                        listOf(Color.Black, Color.Transparent),
                        0f,
                        feather,
                        TileMode.Clamp,
                    )
                val fadeIn =
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color.Black),
                        0f,
                        feather,
                        TileMode.Clamp,
                    )
                val layerPaint = Paint()
                onDrawWithContent {
                    drawContent()
                    val current = layout ?: return@onDrawWithContent
                    val front = path.frontAt(position.value) ?: return@onDrawWithContent
                    val rtl =
                        current.getParagraphDirection(current.getLineStart(front.line)) ==
                            ResolvedTextDirection.Rtl
                    drawSung(
                        current,
                        front,
                        sung,
                        if (rtl) fadeIn else fadeOut,
                        feather,
                        layerPaint,
                    )
                }
            },
    )
}

/** Draws every line above the front whole, and the front line masked up to it. */
private fun DrawScope.drawSung(
    layout: TextLayoutResult,
    front: SweepFront,
    color: Color,
    mask: Brush,
    feather: Float,
    layerPaint: Paint,
) {
    if (front.line > 0) {
        clipRect(bottom = layout.getLineBottom(front.line - 1)) { drawText(layout, color = color) }
    }
    val band = Rect(0f, layout.getLineTop(front.line), size.width, layout.getLineBottom(front.line))
    drawIntoCanvas { canvas ->
        canvas.saveLayer(band, layerPaint)
        clipRect(band.left, band.top, band.right, band.bottom) { drawText(layout, color = color) }
        translate(left = front.x - feather / 2) {
            drawRect(
                brush = mask,
                topLeft = Offset(band.left - front.x - feather, band.top),
                size = Size(band.width + 2 * feather + size.width, band.height),
                blendMode = BlendMode.DstIn,
            )
        }
        canvas.restore()
    }
}

private class LayoutGeometry(private val layout: TextLayoutResult) : TextGeometry {
    override val lineCount: Int
        get() = layout.lineCount

    override fun lineOf(offset: Int): Int = layout.getLineForOffset(offset)

    override fun lineStart(line: Int): Int = layout.getLineStart(line)

    override fun lineVisibleEnd(line: Int): Int = layout.getLineEnd(line, visibleEnd = true)

    override fun extent(start: Int, end: Int): Extent {
        // A selection path would run on to the line's full width at its end.
        var left = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        for (offset in start until end) {
            val box = layout.getBoundingBox(offset)
            left = minOf(left, box.left)
            right = maxOf(right, box.right)
        }
        val rtl = layout.getParagraphDirection(start) == ResolvedTextDirection.Rtl
        return if (rtl) Extent(right, left) else Extent(left, right)
    }
}
