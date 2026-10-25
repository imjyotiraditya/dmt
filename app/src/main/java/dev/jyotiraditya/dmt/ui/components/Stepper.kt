package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Surface2

/** A value between minus and plus blocks that step once per tap and repeat while held. */
@Composable
fun Stepper(
    value: String,
    onStep: (Int) -> Unit,
    lessLabel: String,
    moreLabel: String,
    modifier: Modifier = Modifier,
    canLess: Boolean = true,
    canMore: Boolean = true,
    valueLabel: String? = null,
    onValueClick: (() -> Unit)? = null,
) {
    val step by rememberUpdatedState(onStep)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        StepBlock(icon = DmtIcons.Minus, label = lessLabel, enabled = canLess) { step(-1) }
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            color = Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier =
                Modifier.weight(1f)
                    .then(
                        if (onValueClick != null) {
                            Modifier.pressable(onClick = onValueClick, label = valueLabel)
                        } else {
                            Modifier
                        },
                    ),
        )
        StepBlock(icon = DmtIcons.Plus, label = moreLabel, enabled = canMore) { step(1) }
    }
}

@Composable
private fun StepBlock(icon: Int, label: String, enabled: Boolean, onStep: () -> Unit) {
    IconTap(
        icon = icon,
        description = label,
        onClick = onStep,
        onHold = onStep,
        enabled = enabled,
        size = 56.dp,
        modifier = Modifier.background(Surface2),
    )
}
