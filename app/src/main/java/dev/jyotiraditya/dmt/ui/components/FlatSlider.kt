package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface2

private val Thumb = 28.dp
private val Bar = 10.dp

/** A thick flat slider with a square thumb, and tappable [marks] beneath it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlatSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    label: String,
    modifier: Modifier = Modifier,
    marks: List<Pair<Float, String>> = emptyList(),
) {
    val accent = LocalAccent.current
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            thumb = { Box(modifier = Modifier.size(Thumb).background(Ink)) },
            track = { slider ->
                Canvas(modifier = Modifier.fillMaxWidth().height(Bar)) {
                    drawRect(Surface2)
                    drawRect(
                        accent.fill,
                        size = Size(size.width * slider.coercedValueAsFraction, size.height),
                    )
                }
            },
        )
        if (marks.isNotEmpty()) {
            Marks(marks = marks, range = range, value = value, onPick = onValueChange)
        }
    }
}

/** Labels placed under their values on the bar, kept inside its edges. */
@Composable
private fun Marks(
    marks: List<Pair<Float, String>>,
    range: ClosedFloatingPointRange<Float>,
    value: Float,
    onPick: (Float) -> Unit,
) {
    val span = range.endInclusive - range.start
    Layout(
        content = {
            marks.forEach { (mark, text) ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (mark == value) Ink else Muted,
                    maxLines = 1,
                    modifier =
                        Modifier.pressable(onClick = { onPick(mark) }, label = text)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val inset = Thumb.toPx() / 2
        val travel = constraints.maxWidth - 2 * inset
        layout(constraints.maxWidth, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { i, placeable ->
                val centre = inset + travel * (marks[i].first - range.start) / span
                val x = (centre - placeable.width / 2).toInt()
                placeable.place(x.coerceIn(0, constraints.maxWidth - placeable.width), 0)
            }
        }
    }
}
