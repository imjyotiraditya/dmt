package dev.jyotiraditya.dmt.presentation.main

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.AsciiCover
import dev.jyotiraditya.dmt.core.common.PermissionGrants
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.core.common.generateAsciiPlaceholder
import dev.jyotiraditya.dmt.core.common.generateCoverPlaceholder
import dev.jyotiraditya.dmt.core.common.isLandscapeWindow
import dev.jyotiraditya.dmt.core.common.rememberPermissionGrants
import dev.jyotiraditya.dmt.core.common.toAsciiBitmap
import dev.jyotiraditya.dmt.core.common.tuiClickable
import dev.jyotiraditya.dmt.domain.model.DmtSettings
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.settings.FILES_ENTRY
import dev.jyotiraditya.dmt.presentation.settings.PERMISSION_REGISTRY
import dev.jyotiraditya.dmt.presentation.settings.PermissionEntry
import dev.jyotiraditya.dmt.presentation.settings.PermissionRow
import dev.jyotiraditya.dmt.presentation.settings.SettingRow
import dev.jyotiraditya.dmt.presentation.settings.SyncPermissionState
import dev.jyotiraditya.dmt.presentation.settings.markWidth
import dev.jyotiraditya.dmt.presentation.settings.nextCoverCols
import dev.jyotiraditya.dmt.ui.theme.TuiAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFaint
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.util.audioPermission

private const val BAR_CELLS = 12
private const val ART_SEED = 9_180_301L
private const val ART_COLS = 52
private const val SAMPLE_PX = 256
private const val PREVIEW_HEIGHT_SHARE = 0.5f
private const val ART_HEIGHT_SHARE = 0.55f

private enum class SetupStep(@param:StringRes val label: Int) {
    HELLO(R.string.setup_step_hello),
    ACCESS(R.string.setup_step_access),
    SOURCE(R.string.setup_step_source),
    LOOK(R.string.setup_step_look),
    READY(R.string.setup_step_ready),
}

@Composable
fun SetupScreen(state: DmtState, dispatch: (DmtAction) -> Unit) {
    var step by remember { mutableStateOf(SetupStep.HELLO) }
    val steps = SetupStep.entries
    val index = steps.indexOf(step)
    val grants = rememberPermissionGrants(PERMISSION_REGISTRY.map { it.permission })
    SyncPermissionState(grants, state, dispatch)

    val content: @Composable () -> Unit = {
        when (step) {
            SetupStep.HELLO -> HelloStep()
            SetupStep.ACCESS -> AccessStep(grants)
            SetupStep.SOURCE -> SourceStep(state, dispatch)
            SetupStep.LOOK -> LookStep(state, dispatch)
            SetupStep.READY -> ReadyStep(state, grants)
        }
    }
    val footer: @Composable () -> Unit = {
        HorizontalDivider(color = TuiLine)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            if (step == SetupStep.HELLO) {
                Spacer(modifier = Modifier)
            } else {
                TuiKey(label = stringResource(R.string.setup_back)) {
                    step = steps[index - 1]
                }
            }
            TuiKey(
                label = stringResource(
                    if (step == SetupStep.READY) R.string.setup_start else R.string.setup_next,
                ),
                bright = true,
            ) {
                if (step == SetupStep.READY) {
                    dispatch(DmtAction.Config(state.settings.copy(setupDone = true)))
                } else {
                    step = steps[index + 1]
                }
            }
        }
    }

    val root = Modifier
        .fillMaxSize()
        .background(TuiBg)
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(horizontal = 16.dp)

    if (isLandscapeWindow()) {
        Row(modifier = root) {
            SetupRail(index = index, total = steps.size, label = step.label)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.weight(1f)) { content() }
                footer()
            }
        }
    } else {
        Column(modifier = root) {
            Titlebar(label = stringResource(R.string.page_setup))
            StepHeader(index = index, total = steps.size, label = step.label)
            Box(modifier = Modifier.weight(1f)) { content() }
            footer()
        }
    }
}

@Composable
private fun SetupRail(index: Int, total: Int, @StringRes label: Int) {
    Column(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .fillMaxHeight(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            BrandMark()
        }
        HorizontalDivider(color = TuiLine, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.page_setup),
            style = MaterialTheme.typography.titleMedium,
            color = TuiBright,
        )
        Text(
            text = stepCounter(index, total) + stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = progressBar(index, total),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
        )
    }
}

private fun stepCounter(index: Int, total: Int): String = "%02d/%02d  ".format(index + 1, total)

@Composable
private fun progressBar(index: Int, total: Int): AnnotatedString =
    buildAnnotatedString {
        val cells = (index + 1) * BAR_CELLS / total
        withStyle(SpanStyle(color = TuiAccent)) { append("█".repeat(cells)) }
        withStyle(SpanStyle(color = TuiFaint)) { append("░".repeat(BAR_CELLS - cells)) }
    }

@Composable
private fun StepHeader(index: Int, total: Int, @StringRes label: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
    ) {
        Text(
            text = stepCounter(index, total) + stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = progressBar(index, total),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun StepBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TuiDim,
        modifier = Modifier.padding(bottom = 14.dp),
    )
}

@Composable
private fun HelloStep() {
    val context = LocalContext.current
    val art = remember { generateAsciiPlaceholder(context, seed = ART_SEED, cols = ART_COLS) }

    if (isLandscapeWindow()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            AsciiCover(
                cover = art,
                playing = true,
                fitHeight = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
            ) {
                HelloText()
            }
        }
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val artMax = maxHeight * ART_HEIGHT_SHARE

            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                AsciiCover(
                    cover = art,
                    playing = true,
                    fitHeight = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = artMax),
                )
                Column(modifier = Modifier.padding(top = 20.dp)) {
                    HelloText()
                }
            }
        }
    }
}

@Composable
private fun HelloText() {
    Text(
        text = stringResource(R.string.setup_hello_tagline),
        style = MaterialTheme.typography.titleMedium,
        color = TuiBright,
    )
    Text(
        text = stringResource(R.string.setup_hello_body),
        style = MaterialTheme.typography.labelMedium,
        color = TuiDim,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun AccessStep(grants: PermissionGrants) {
    val entries = PERMISSION_REGISTRY + FILES_ENTRY
    val landscape = isLandscapeWindow()

    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepBody(stringResource(R.string.setup_access_body))

        if (landscape) {
            val half = (entries.size + 1) / 2
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    entries.take(half).forEach { AccessRow(it, grants) }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    entries.drop(half).forEach { AccessRow(it, grants) }
                }
            }
        } else {
            entries.forEach { AccessRow(it, grants) }
        }

        if (!grants.isGranted(audioPermission)) {
            Text(
                text = stringResource(R.string.setup_access_required),
                style = MaterialTheme.typography.labelSmall,
                color = TuiAccent,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun AccessRow(entry: PermissionEntry, grants: PermissionGrants) {
    val isFiles = entry.isFilesAccess
    val granted = if (isFiles) grants.filesAccess else grants.isGranted(entry.permission)

    PermissionRow(
        entry = entry,
        granted = granted,
        actionLabel = stringResource(
            when {
                isFiles && granted -> R.string.perm_revoke
                isFiles -> R.string.grant
                else -> grants.actionLabel(entry.permission)
            },
        ),
        onAction = {
            if (isFiles) grants.openFilesAccess() else grants.act(entry.permission)
        },
        showWhenOff = false,
    )
}

@Composable
private fun SourceStep(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepBody(stringResource(R.string.setup_source_body))
        ChoiceRow(
            label = SourceMode.LOCAL.label,
            why = stringResource(R.string.setup_source_local_why),
            selected = settings.sourceMode == SourceMode.LOCAL,
        ) {
            dispatch(DmtAction.Config(settings.copy(sourceMode = SourceMode.LOCAL)))
        }
        ChoiceRow(
            label = SourceMode.JELLYFIN.label,
            why = stringResource(R.string.setup_source_jellyfin_why),
            selected = settings.sourceMode == SourceMode.JELLYFIN,
        ) {
            dispatch(DmtAction.Config(settings.copy(sourceMode = SourceMode.JELLYFIN)))
        }
    }
}

@Composable
private fun ChoiceRow(label: String, why: String, selected: Boolean, onSelect: () -> Unit) {
    val mark = if (selected) "(*) " else "( ) "

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tuiClickable(onSelect),
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = if (selected) TuiAccent else TuiFaint)) {
                        append(mark)
                    }
                    withStyle(SpanStyle(color = if (selected) TuiBright else TuiFg)) {
                        append(label)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = why,
                style = MaterialTheme.typography.labelSmall,
                color = TuiDim,
                modifier = Modifier.padding(start = markWidth(mark), top = 2.dp),
            )
        }
        HorizontalDivider(color = TuiLine)
    }
}

@Composable
private fun LookStep(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val settings = state.settings
    val context = LocalContext.current
    val sample = remember { generateCoverPlaceholder(seed = ART_SEED, size = SAMPLE_PX) }
    val preview = remember(sample, settings.cols) { sample.toAsciiBitmap(context, settings.cols) }

    if (isLandscapeWindow()) {
        Row(modifier = Modifier.fillMaxSize()) {
            LookPreview(
                sample = sample,
                preview = preview,
                settings = settings,
                fitHeight = true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                StepBody(stringResource(R.string.setup_look_body))
                LookRows(settings, dispatch)
            }
        }
    } else {
        BoxWithConstraints {
            val previewMax = maxHeight * PREVIEW_HEIGHT_SHARE

            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                StepBody(stringResource(R.string.setup_look_body))
                LookPreview(
                    sample = sample,
                    preview = preview,
                    settings = settings,
                    fitHeight = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = previewMax)
                        .padding(bottom = 14.dp),
                )
                LookRows(settings, dispatch)
            }
        }
    }
}

@Composable
private fun LookPreview(
    sample: Bitmap,
    preview: Bitmap,
    settings: DmtSettings,
    fitHeight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        if (settings.rawArt) {
            Image(
                bitmap = remember(sample) { sample.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.aspectRatio(1f),
            )
        } else {
            AsciiCover(
                cover = preview,
                playing = true,
                wave = settings.wave,
                fitHeight = fitHeight,
            )
        }
    }
}

@Composable
private fun LookRows(settings: DmtSettings, dispatch: (DmtAction) -> Unit) {
    val on = stringResource(R.string.on)
    val off = stringResource(R.string.off)

    SettingRow(
        label = stringResource(R.string.set_detail),
        value = pluralStringResource(R.plurals.set_detail_value, settings.cols, settings.cols),
    ) {
        dispatch(DmtAction.Config(settings.copy(cols = nextCoverCols(settings.cols))))
    }
    SettingRow(
        label = stringResource(R.string.set_wave),
        value = if (settings.wave) on else off,
    ) {
        dispatch(DmtAction.Config(settings.copy(wave = !settings.wave)))
    }
    SettingRow(
        label = stringResource(R.string.set_raw),
        value = if (settings.rawArt) on else off,
    ) {
        dispatch(DmtAction.Config(settings.copy(rawArt = !settings.rawArt)))
    }
}

@Composable
private fun ReadyStep(state: DmtState, grants: PermissionGrants) {
    val settings = state.settings
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepBody(stringResource(R.string.setup_ready_body))
        SummaryRow(
            label = stringResource(R.string.perm_audio_label),
            value = stringResource(
                if (grants.isGranted(audioPermission)) {
                    R.string.setup_granted
                } else {
                    R.string.setup_missing
                },
            ),
        )
        SummaryRow(
            label = stringResource(R.string.setup_step_source),
            value = settings.sourceMode.label,
        )
        SummaryRow(
            label = stringResource(R.string.set_detail),
            value = pluralStringResource(R.plurals.set_detail_value, settings.cols, settings.cols),
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TuiFg,
        )
        Text(
            text = ".".repeat(200),
            style = MaterialTheme.typography.labelSmall,
            color = TuiFaint,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = TuiBright,
        )
    }
}
