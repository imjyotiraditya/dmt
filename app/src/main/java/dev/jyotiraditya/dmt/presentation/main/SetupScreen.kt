package dev.jyotiraditya.dmt.presentation.main

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.PermissionGrants
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.common.rememberPermissionGrants
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.settings.FILES_ENTRY
import dev.jyotiraditya.dmt.presentation.settings.PERMISSION_REGISTRY
import dev.jyotiraditya.dmt.presentation.settings.PermissionRow
import dev.jyotiraditya.dmt.presentation.settings.SourceFieldList
import dev.jyotiraditya.dmt.presentation.settings.SourceFields
import dev.jyotiraditya.dmt.presentation.settings.SourceSleeve
import dev.jyotiraditya.dmt.presentation.settings.SyncPermissionState
import dev.jyotiraditya.dmt.presentation.settings.icon
import dev.jyotiraditya.dmt.presentation.settings.line
import dev.jyotiraditya.dmt.presentation.settings.rememberSourceConnect
import dev.jyotiraditya.dmt.presentation.settings.rememberSourceFields
import dev.jyotiraditya.dmt.presentation.settings.setupDetail
import dev.jyotiraditya.dmt.presentation.settings.title
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.motion.EaseOut
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.poster
import dev.jyotiraditya.dmt.ui.theme.rowFill
import dev.jyotiraditya.dmt.util.audioPermission

private enum class Step {
    HELLO,
    ACCESS,
    SOURCE,
    SIGN_IN,
    READY,
}

/** First-run setup: a greeting, permissions, the music source and its sign-in, then a summary. */
@Composable
fun SetupScreen(state: DmtState, dispatch: (DmtAction) -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    var kind by rememberSaveable { mutableStateOf(SourceMode.LOCAL) }
    val steps = Step.entries.filter { it != Step.SIGN_IN || kind != SourceMode.LOCAL }
    val step = steps[index]
    val grants = rememberPermissionGrants(PERMISSION_REGISTRY.mapNotNull { it.permission })
    SyncPermissionState(grants, state, dispatch)

    val fields = rememberSourceFields(kind, saved = null)
    val connect = rememberSourceConnect(fields, dispatch)
    val server = state.settings.server?.takeIf { it.kind == kind }
    LaunchedEffect(server != null) {
        if (server != null && step == Step.SIGN_IN) index++
    }

    Column(
        modifier =
            Modifier.fillMaxSize().background(Ground).windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Margin, vertical = 14.dp),
        ) {
            Wordmark()
            Spacer(modifier = Modifier.weight(1f))
            StageWords(current = step)
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (fadeIn() + slideInHorizontally { if (forward) it / 5 else -it / 5 }) togetherWith
                    fadeOut()
            },
            label = "setupStep",
            modifier = Modifier.weight(1f),
        ) { shown ->
            when (shown) {
                Step.HELLO -> Hello()
                Step.ACCESS -> Access(grants)
                Step.SOURCE -> Source(kind, onPick = { kind = it })
                Step.SIGN_IN -> SignIn(fields, state.sourceError, server?.name, connect)
                Step.READY -> Ready(server?.name ?: stringResource(kind.title), grants)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.padding(horizontal = Margin, vertical = 16.dp),
        ) {
            val last = step == Step.READY
            val signingIn = step == Step.SIGN_IN && server == null
            Block(
                label =
                    stringResource(
                        when {
                            step == Step.HELLO -> R.string.setup_begin
                            last -> R.string.setup_start
                            signingIn && state.connecting -> R.string.source_form_connecting
                            signingIn -> R.string.source_form_connect
                            else -> R.string.setup_next
                        },
                    ),
                icon = if (last) DmtIcons.Play else null,
                enabled = !(signingIn && state.connecting),
                onClick = {
                    when {
                        last -> {
                            val done = state.settings.copy(setupDone = true)
                            val source =
                                if (server == null) done.copy(activeSource = null) else done
                            dispatch(DmtAction.Config(source))
                        }

                        signingIn -> connect()

                        else -> index++
                    }
                },
            )
            if (index > 0) {
                TextLink(
                    label = stringResource(R.string.setup_back),
                    onClick = { index-- },
                )
            }
        }
    }
}

@Composable
private fun Hello() {
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize().padding(horizontal = Margin, vertical = 24.dp),
    ) {
        RisingLine(stringResource(R.string.brand_dear_music), poster(0.145f), Ink, delayMs = 0)
        RisingLine(stringResource(R.string.brand_thanks), poster(0.145f), Muted, delayMs = 110)
        Text(
            text = stringResource(R.string.setup_hello_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

/** Setup's stages as words in the top bar, the current one in ink. */
@Composable
private fun StageWords(current: Step) {
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Stage.entries.forEach { stage ->
            Text(
                text = stringResource(stage.label),
                style = MaterialTheme.typography.labelLarge,
                color = if (current in stage.steps) Ink else Muted,
            )
        }
    }
}

/** A stage of setup and the steps it covers. */
private enum class Stage(@param:StringRes val label: Int, val steps: Set<Step>) {
    HELLO(R.string.setup_stage_hello, setOf(Step.HELLO)),
    ACCESS(R.string.setup_stage_access, setOf(Step.ACCESS)),
    SOURCE(R.string.setup_stage_source, setOf(Step.SOURCE, Step.SIGN_IN)),
    READY(R.string.setup_stage_ready, setOf(Step.READY)),
}

/** A step's title and what it asks. */
@Composable
private fun StepTitle(text: String, body: String) {
    PageHead(title = text, modifier = Modifier.padding(horizontal = Margin))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyLarge,
        color = Ink,
        modifier = Modifier.padding(horizontal = Margin).padding(bottom = 20.dp),
    )
}

@Composable
private fun Access(grants: PermissionGrants) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            stringResource(R.string.setup_access_title),
            stringResource(R.string.setup_access_body),
        )
        (PERMISSION_REGISTRY + FILES_ENTRY).forEachIndexed { index, entry ->
            PermissionRow(entry, grants, fill = index.rowFill, showWhenOff = false)
        }
        if (!grants.isGranted(audioPermission)) {
            Text(
                text = stringResource(R.string.setup_access_required),
                style = MaterialTheme.typography.bodySmall,
                color = Ink,
                modifier = Modifier.padding(horizontal = Margin, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun Source(picked: SourceMode, onPick: (SourceMode) -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            stringResource(R.string.setup_source_title),
            stringResource(R.string.setup_source_body),
        )
        SourceMode.entries.forEachIndexed { index, mode ->
            SourceSleeve(
                icon = mode.icon,
                label = stringResource(mode.title),
                detail = stringResource(mode.setupDetail),
                chosen = picked == mode,
                onClick = { onPick(mode) },
                fill = index.rowFill,
            )
        }
    }
}

/** Signs in to the server picked in setup, or shows the one already signed in to as [signedIn]. */
@Composable
private fun SignIn(fields: SourceFields, error: UiText?, signedIn: String?, connect: () -> Unit) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        StepTitle(
            stringResource(R.string.source_form_title_connect, stringResource(fields.kind.title)),
            if (signedIn != null) {
                stringResource(R.string.setup_signed_in, signedIn)
            } else {
                stringResource(fields.kind.line)
            },
        )
        if (signedIn == null) {
            SourceFieldList(
                fields = fields,
                error = error,
                onDone = connect,
                modifier = Modifier.padding(horizontal = Margin),
            )
        }
    }
}

@Composable
private fun Ready(source: String, grants: PermissionGrants) {
    Column(
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier.fillMaxSize().padding(horizontal = Margin, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.setup_ready_title),
            style = poster(0.2f),
            color = Ink,
        )
        Text(
            text =
                stringResource(
                    if (grants.isGranted(audioPermission)) {
                        R.string.setup_ready_body
                    } else {
                        R.string.setup_access_required
                    },
                ),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = stringResource(R.string.setup_ready_source, source),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/** A headline line that slides up into place the first time it is shown. */
@Composable
private fun RisingLine(
    text: String,
    style: TextStyle,
    color: Color,
    delayMs: Int,
    modifier: Modifier = Modifier,
) {
    val rise = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        rise.animateTo(0f, tween(RISE_MS, delayMillis = delayMs, easing = EaseOut))
    }
    Box(modifier = modifier.clipToBounds()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            modifier = Modifier.graphicsLayer { translationY = size.height * rise.value },
        )
    }
}

private const val RISE_MS = 900
