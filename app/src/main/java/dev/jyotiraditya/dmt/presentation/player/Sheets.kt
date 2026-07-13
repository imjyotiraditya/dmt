package dev.jyotiraditya.dmt.presentation.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.Spec
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiGreen
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.ui.theme.TuiRed
import dev.jyotiraditya.dmt.util.asTime

private val TRACK_SPEC_LABELS = setOf("FMT", "BIT", "RATE", "CH", "KBPS", "VBR", "GAPLESS", "SIZE")
private val DECODER_SPEC_LABELS = setOf("DEC", "HW", "IMPL", "INST")
private val OUTPUT_ROUTE_LABELS = setOf("API", "BIT", "RATE", "BUF", "FLAGS")
private val DEVICE_ROUTE_LABELS = setOf("VIA", "NAME", "RATES", "ENC", "CH")
private val CONNECTOR_DASH = floatArrayOf(3f, 7f)
private const val CHAIN_LABEL_WIDTH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuiSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TuiBg,
        shape = RectangleShape,
        dragHandle = null,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(TuiLine),
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                content()
                Spacer(modifier = Modifier.height(18.dp))
            }
        }
    }
}

@Composable
fun SheetHeader(
    title: String,
    meta: String? = null,
    actions: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(LocalAccent.current),
            )
            Text(
                text = " $title",
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                maxLines = 1,
                modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
            )
        }
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = TuiFaint,
            )
        }
        actions()
    }
}

@Composable
fun QueueList(
    state: DmtState,
    dispatch: (DmtAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current

    LazyColumn(modifier = modifier) {
        itemsIndexed(state.queue) { index, label ->
            val current = index == state.queueIndex
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .tuiClickable { dispatch(DmtAction.Jump(index)) }
                    .padding(vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (current) accent else TuiFaint),
                )
                Text(
                    text = " %02d ".format(index + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = TuiFaint,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (current) TuiBright else TuiDim,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = TuiFaint,
                    modifier = Modifier
                        .tuiClickable { dispatch(DmtAction.RemoveAt(index)) }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
fun InfoContent(state: DmtState) {
    val track: Track? = state.tracks.find { it.id.toString() == state.nowPlayingId }

    InfoRow(
        label = stringResource(R.string.info_title),
        value = state.title,
    )
    InfoRow(
        label = stringResource(R.string.info_artist),
        value = state.artist.lowercase(),
    )
    if (state.album.isNotBlank()) {
        InfoRow(
            label = stringResource(R.string.info_album),
            value = state.album.lowercase(),
        )
    }
    InfoRow(
        label = stringResource(R.string.info_duration),
        value = state.durationMs.asTime(),
    )
    state.tech.forEach { spec ->
        InfoRow(
            label = spec.label.lowercase(),
            value = spec.value.lowercase(),
        )
    }
    track?.let {
        InfoRow(
            label = stringResource(R.string.info_uri),
            value = it.uri.toString(),
        )
    }
}

@Composable
fun ChainContent(state: DmtState) {
    val stages = remember(state.tech, state.speed, state.route) { chainStages(state) }

    Column(
        modifier = Modifier
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
    ) {
        stages.forEachIndexed { index, stage ->
            ChainStageRow(
                stage = stage,
                last = index == stages.lastIndex,
            )
        }
    }
}

@Composable
private fun ChainStageRow(stage: ChainStage, last: Boolean) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(8.dp)
                    .background(stage.color),
            )
            if (!last) {
                ChainConnector(
                    modifier = Modifier
                        .width(8.dp)
                        .weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stage.name,
                style = MaterialTheme.typography.labelMedium,
                color = TuiBright,
            )
            stage.specs.forEach { spec ->
                ChainSpecRow(spec)
            }
        }
    }
}

@Composable
private fun ChainSpecRow(spec: Spec) {
    Row(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = spec.label.lowercase().padEnd(CHAIN_LABEL_WIDTH),
            style = MaterialTheme.typography.bodyMedium,
            color = TuiDim,
        )
        Text(
            text = spec.value.lowercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = if (spec.hot) LocalAccent.current else TuiFg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ChainConnector(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val x = size.width / 2f
        val inset = 3.dp.toPx()
        val tipY = size.height - inset
        drawLine(
            color = TuiFaint,
            start = Offset(x, inset),
            end = Offset(x, tipY),
            pathEffect = PathEffect.dashPathEffect(CONNECTOR_DASH),
        )
        drawLine(
            color = TuiFaint,
            start = Offset(x - inset, tipY - inset),
            end = Offset(x, tipY),
        )
        drawLine(
            color = TuiFaint,
            start = Offset(x + inset, tipY - inset),
            end = Offset(x, tipY),
        )
    }
}

private data class ChainStage(
    val name: String,
    val color: Color,
    val specs: List<Spec>,
)

private fun chainStages(state: DmtState): List<ChainStage> {
    val trackRate = state.tech.firstOrNull { it.label == "RATE" }?.value
    val trackBits = state.tech.firstOrNull { it.label == "BIT" }?.value
    val output = state.route.filter { it.label in OUTPUT_ROUTE_LABELS }.map { spec ->
        when {
            spec.label == "RATE" && trackRate != null && trackRate != spec.value ->
                spec.copy(value = "$trackRate -> ${spec.value}", hot = true)

            spec.label == "BIT" && trackBits != null && trackBits != spec.value ->
                spec.copy(value = "$trackBits -> ${spec.value}", hot = true)

            else -> spec
        }
    }
    val device = state.route.filter { it.label in DEVICE_ROUTE_LABELS }

    return buildList {
        state.tech.filter { it.label in TRACK_SPEC_LABELS }
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(name = "track", color = TuiAccent, specs = it)) }
        state.tech.filter { it.label in DECODER_SPEC_LABELS }
            .takeIf { it.isNotEmpty() }
            ?.let { add(ChainStage(name = "decoder", color = TuiGreen, specs = it)) }
        add(
            ChainStage(
                name = "dsp",
                color = TuiFg,
                specs = listOf(Spec(label = "SPEED", value = "%.2fx".format(state.speed))),
            ),
        )
        if (output.isNotEmpty()) {
            add(ChainStage(name = "output", color = TuiRed, specs = output))
        }
        if (device.isNotEmpty()) {
            add(ChainStage(name = "device", color = TuiDim, specs = device))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 9.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
                modifier = Modifier.padding(end = 16.dp),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = TuiFg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TuiLine),
        )
    }
}
