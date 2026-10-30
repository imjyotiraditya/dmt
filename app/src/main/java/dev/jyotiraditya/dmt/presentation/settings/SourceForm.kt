package dev.jyotiraditya.dmt.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.navigation.Destination
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.theme.Danger
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin

/**
 * The page that adds a source, edits a saved one or signs a signed-out one back in; [onDone] runs
 * once it is saved or removed.
 */
@Composable
fun SourceFormPage(
    form: Destination.SourceForm,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    viewModel: SourceFormViewModel =
        hiltViewModel<SourceFormViewModel, SourceFormViewModel.Factory>(
            creationCallback = { it.create(form) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    val saved = state.saved
    val signIn = state.signIn
    val fields = rememberSourceFields(form.kind, saved)
    val connect = rememberSourceConnect(fields, viewModel::save)

    Column(
        modifier =
            Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = Margin)
                .padding(bottom = LocalContentBottom.current),
    ) {
        PageHead(
            title =
                when {
                    saved == null ->
                        stringResource(
                            R.string.source_form_title_connect,
                            stringResource(form.kind.display.title),
                        )

                    fields.editing -> stringResource(R.string.source_form_title_edit, saved.name)

                    else -> stringResource(R.string.source_form_title_sign_in, saved.name)
                },
        )
        Text(
            text = stringResource(form.kind.display.line),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        SourceFieldList(fields = fields, error = signIn.error, onDone = connect)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Block(
                label =
                    stringResource(
                        when {
                            signIn.connecting -> R.string.source_form_connecting
                            fields.editing -> R.string.source_form_save
                            else -> R.string.source_form_connect
                        },
                    ),
                onClick = connect,
                enabled = !signIn.connecting,
            )
            TextLink(label = stringResource(R.string.source_form_cancel), onClick = onCancel)
            if (saved != null) {
                Spacer(modifier = Modifier.weight(1f))
                TextLink(
                    label = stringResource(R.string.source_form_remove),
                    onClick = viewModel::remove,
                    color = Danger,
                )
            }
        }
    }
}
