package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.components.DmtIcon
import dev.jyotiraditya.dmt.ui.components.background
import dev.jyotiraditya.dmt.ui.components.pressable
import dev.jyotiraditya.dmt.ui.theme.Dark
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface
import dev.jyotiraditya.dmt.ui.theme.Surface2

/**
 * A source with its [icon], as a sleeve that pulls out in the accent with its record while
 * [chosen].
 */
@Composable
fun SourceSleeve(
    @DrawableRes icon: Int,
    label: String,
    detail: String,
    chosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Surface,
    muted: Boolean = false,
    spinning: Boolean = false,
    actions: (@Composable (ink: Color) -> Unit)? = null,
) {
    val accent = LocalAccent.current
    val field by animateColorAsState(if (chosen) accent.fill else fill, label = "sleeveField")
    val ink =
        when {
            chosen -> accent.onFill
            muted -> Muted
            else -> Ink
        }
    val pulled =
        animateFloatAsState(
            targetValue = if (chosen) 1f else 0f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            label = "sleevePulled",
        )
    val out by remember { derivedStateOf { pulled.value > 0f } }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clipToBounds()
                .background { field }
                .pressable(onClick = onClick, sink = 0.985f, label = label)
                .animateContentSize(),
    ) {
        if (out) {
            Record(
                spinning = spinning,
                modifier =
                    Modifier.align(Alignment.CenterEnd).size(150.dp).graphicsLayer {
                        translationX = size.width * (1.05f - 0.65f * pulled.value)
                    },
            )
        }
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier =
                Modifier.heightIn(min = if (chosen) 168.dp else 0.dp)
                    .padding(horizontal = Margin, vertical = 16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DmtIcon(icon = icon, tint = ink, size = 26.dp)
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = ink,
                    maxLines = if (chosen) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(if (chosen) 0.7f else 1f).padding(top = 4.dp),
                )
            }
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    actions(ink)
                }
            }
        }
    }
}

/** A black record with two sheens and a label in the accent, turning while [spinning]. */
@Composable
private fun Record(spinning: Boolean, modifier: Modifier = Modifier) {
    val label = LocalAccent.current.highlight
    val turn = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (!spinning) return@LaunchedEffect
        val from = turn.value % 360f
        turn.snapTo(from)
        turn.animateTo(
            from + 360f,
            infiniteRepeatable(tween(durationMillis = 1800, easing = LinearEasing)),
        )
    }
    Canvas(modifier = modifier.graphicsLayer { rotationZ = turn.value }) {
        val radius = size.minDimension / 2
        drawCircle(color = Dark, radius = radius)
        drawArc(color = Surface2, startAngle = -90f, sweepAngle = 40f, useCenter = true)
        drawArc(color = Surface2, startAngle = 90f, sweepAngle = 40f, useCenter = true)
        drawCircle(color = label, radius = radius * 0.32f)
        drawCircle(color = Dark, radius = radius * 0.06f)
    }
}
