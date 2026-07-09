package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.AudioJourney
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine

/**
 * The Track -> Decoder -> Processing -> Output -> Output Device signal flow. Sections with
 * nothing accurate to report (decoder unresolved, no output device found) are simply omitted
 * rather than shown with placeholder values — we never fabricate a reading.
 */
@Composable
fun AudioInspectorContent(journey: AudioJourney) {
    var showAdvanced by remember { mutableStateOf(false) }

    JourneySection(stringResource(R.string.ai_track), journey.track, first = true)
    JourneySection(stringResource(R.string.ai_decoder), journey.decoder)

    if (journey.processing.isEmpty()) {
        JourneyArrow()
        SectionCaption(stringResource(R.string.ai_processing))
        Text(
            text = stringResource(R.string.ai_no_processing),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    } else {
        JourneySection(stringResource(R.string.ai_processing), journey.processing)
    }

    JourneySection(stringResource(R.string.ai_output), journey.output)
    JourneySection(stringResource(R.string.ai_output_device), journey.outputDevice)

    if (journey.advanced.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(1.dp)
                .background(TuiLine),
        )
        Text(
            text = "${if (showAdvanced) "\u2212" else "+"} ${stringResource(R.string.ai_advanced)}",
            style = MaterialTheme.typography.labelMedium,
            color = TuiFg,
            modifier = Modifier
                .fillMaxWidth()
                .tuiClickable { showAdvanced = !showAdvanced }
                .padding(vertical = 10.dp),
        )
        if (showAdvanced) {
            journey.advanced.forEach { spec ->
                InfoRow(label = spec.label.lowercase(), value = spec.value)
            }
        }
    }
}

@Composable
private fun JourneySection(title: String, specs: List<Spec>, first: Boolean = false) {
    if (specs.isEmpty()) return
    if (!first) JourneyArrow()
    SectionCaption(title)
    specs.forEach { spec ->
        InfoRow(label = spec.label.lowercase(), value = spec.value.lowercase())
    }
}

@Composable
private fun SectionCaption(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TuiFg,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

@Composable
private fun JourneyArrow() {
    Text(
        text = "\u2193",
        style = MaterialTheme.typography.bodyMedium,
        color = TuiFaint,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    )
}
