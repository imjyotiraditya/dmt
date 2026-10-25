package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import dev.jyotiraditya.dmt.ui.theme.Ink
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Size of the grid the glyph is drawn in. */
private const val GRID = 24f

/** Radius of the arrow's circle in the grid. */
private const val RADIUS = 7.25f

/** Time for one full turn while a scan runs. */
private const val TURN_MS = 900

/** Time for the line to run from the arrow into the check. */
private const val CHECK_MS = 560

private val Center = Offset(GRID / 2, GRID / 2)

/** Where the arrow's tip sits on the circle, in degrees clockwise from three o'clock. */
private const val TIP_ANGLE = 220f

/**
 * The arrow's arc, counter-clockwise from its tail to its tip, then the rest of the circle down to
 * its bottom and the check's long arm.
 */
private val Line: Path =
    Path().apply {
        val circle = Rect(center = Center, radius = RADIUS)
        arcTo(circle, startAngleDegrees = 160f, sweepAngleDegrees = -300f, forceMoveTo = true)
        arcTo(circle, startAngleDegrees = -140f, sweepAngleDegrees = -120f, forceMoveTo = false)
        cubicTo(12.91f, 19.52f, 13.63f, 15.07f, 19f, 7.5f)
    }

/** The arrowhead at the tip, one arm up and one to the right. */
private val Head: Path =
    Path().apply {
        val tip = Center + Offset(cos(TIP_ANGLE.radians()), sin(TIP_ANGLE.radians())) * RADIUS
        moveTo(tip.x, tip.y - 4.5f)
        lineTo(tip.x, tip.y)
        lineTo(tip.x + 4.5f, tip.y)
    }

/** Length along [Line] where the arrow's arc ends. */
private val ArcEnd = RADIUS * 300f.radians()

/** Length along [Line] where the check begins, on the circle's lower left. */
private val CheckStart = RADIUS * 370f.radians()

/** A refresh arrow that turns while [scanning] and runs on into a check while [done]. */
@Composable
internal fun RescanGlyph(
    scanning: Boolean,
    done: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    size: Dp = 20.dp,
) {
    val turn = remember { Animatable(0f) }
    val check = remember { Animatable(0f) }
    LaunchedEffect(scanning, done) {
        if (scanning) {
            check.animateTo(0f, tween(CHECK_MS / 2, easing = FastOutSlowInEasing))
            while (true) {
                turn.animateTo(turn.value - 360f, tween(TURN_MS, easing = LinearEasing))
            }
        } else {
            // Eases into the next full turn before the check.
            val rest = floor(turn.value / 360f) * 360f
            val left = ((turn.value - rest) / 360f * TURN_MS * 1.5f).toInt()
            turn.animateTo(rest, tween(left, easing = LinearOutSlowInEasing))
            turn.snapTo(0f)
            check.animateTo(if (done) 1f else 0f, tween(CHECK_MS, easing = FastOutSlowInEasing))
        }
    }

    val measure = remember { PathMeasure().apply { setPath(Line, forceClosed = false) } }
    val visible = remember { Path() }
    Canvas(modifier = modifier.size(size)) {
        val progress = check.value
        visible.rewind()
        measure.getSegment(
            startDistance = lerp(0f, CheckStart, progress),
            stopDistance = lerp(ArcEnd, measure.length, progress),
            destination = visible,
        )
        val stroke = Stroke(width = 1.5f, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
        scale(scale = this.size.minDimension / GRID, pivot = Offset.Zero) {
            rotate(degrees = turn.value, pivot = Center) {
                drawPath(visible, tint, style = stroke)
                val head = (1f - progress * 4f).coerceIn(0f, 1f)
                if (head > 0f) drawPath(Head, tint, alpha = head, style = stroke)
            }
        }
    }
}

private fun Float.radians(): Float = Math.toRadians(toDouble()).toFloat()
