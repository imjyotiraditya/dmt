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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.presentation.main.LocalContentBottom
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtState
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.ui.components.Block
import dev.jyotiraditya.dmt.ui.components.PageHead
import dev.jyotiraditya.dmt.ui.components.TextLink
import dev.jyotiraditya.dmt.ui.theme.Danger
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin

/** The page that adds a source, edits a saved one or signs a signed-out one back in. */
@Composable
fun SourceFormPage(state: DmtState, dispatch: (DmtAction) -> Unit) {
    val form = state.sourceForm
    val saved = state.settings.sources.firstOrNull { it.id == form.sourceId }
    val fields = rememberSourceFields(form.kind, saved)
    val connect = rememberSourceConnect(fields, dispatch)

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
                            stringResource(form.kind.title),
                        )

                    fields.editing -> stringResource(R.string.source_form_title_edit, saved.name)

                    else -> stringResource(R.string.source_form_title_sign_in, saved.name)
                },
        )
        Text(
            text = stringResource(form.kind.line),
            style = MaterialTheme.typography.bodyLarge,
            color = Ink,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        SourceFieldList(fields = fields, error = state.sourceError, onDone = connect)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Block(
                label =
                    stringResource(
                        when {
                            state.connecting -> R.string.source_form_connecting
                            fields.editing -> R.string.source_form_save
                            else -> R.string.source_form_connect
                        },
                    ),
                onClick = connect,
                enabled = !state.connecting,
            )
            TextLink(
                label = stringResource(R.string.source_form_cancel),
                onClick = { dispatch(DmtAction.Show(form.backTo)) },
            )
            if (saved != null) {
                Spacer(modifier = Modifier.weight(1f))
                TextLink(
                    label = stringResource(R.string.source_form_remove),
                    onClick = {
                        dispatch(DmtAction.Config(state.settings.without(saved.id)))
                        dispatch(DmtAction.Show(DmtView.SOURCES))
                    },
                    color = Danger,
                )
            }
        }
    }
}
