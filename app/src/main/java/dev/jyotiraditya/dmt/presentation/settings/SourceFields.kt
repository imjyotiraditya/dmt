package dev.jyotiraditya.dmt.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.common.asString
import dev.jyotiraditya.dmt.data.remote.jellyfin.normalizeServerUrl
import dev.jyotiraditya.dmt.domain.model.ServerSource
import dev.jyotiraditya.dmt.domain.model.SourceDraft
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.ui.components.FilledField
import dev.jyotiraditya.dmt.ui.components.PasswordField
import dev.jyotiraditya.dmt.ui.theme.Danger
import dev.jyotiraditya.dmt.util.localNetworkPermission

/** What is typed into a source form; [saved] is the source being edited, if any. */
@Stable
class SourceFields(
    val kind: SourceMode,
    val saved: ServerSource?,
    name: String = saved?.name.orEmpty(),
    url: String = saved?.url.orEmpty(),
    username: String = saved?.username.orEmpty(),
) {
    val name = TextFieldState(name)
    val url = TextFieldState(url)
    val username = TextFieldState(username)
    val password = TextFieldState()
    var showMissing by mutableStateOf(false)

    val editing: Boolean
        get() = saved?.signedIn == true

    /** Whether a password is needed; a saved sign-in is kept while the address and username are. */
    val needsPassword: Boolean
        get() = !editing || url.text.trim() != saved?.url || username.text.trim() != saved.username

    val complete: Boolean
        get() =
            url.text.isNotBlank() &&
                username.text.isNotBlank() &&
                (!needsPassword || password.text.isNotEmpty())

    fun draft() = SourceDraft(
        id = saved?.id,
        kind = kind,
        name = name.text.toString(),
        url = url.text.toString(),
        username = username.text.toString(),
        password = password.text.toString(),
    )
}

/** Remembers the fields for [saved], or for a new source of [kind]; the password is not saved. */
@Composable
fun rememberSourceFields(kind: SourceMode, saved: ServerSource?): SourceFields = rememberSaveable(
    kind,
    saved?.id,
    saver =
        listSaver(
            save = {
                listOf(
                    it.name.text.toString(),
                    it.url.text.toString(),
                    it.username.text.toString(),
                )
            },
            restore = { SourceFields(kind, saved, it[0], it[1], it[2]) },
        ),
) {
    SourceFields(kind, saved)
}

/** Returns an action that checks [fields], asks for local network access if needed, and saves. */
@Composable
fun rememberSourceConnect(fields: SourceFields, dispatch: (DmtAction) -> Unit): () -> Unit {
    val submit = { dispatch(DmtAction.SaveSource(fields.draft())) }
    val networkPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { submit() }
    return {
        fields.showMissing = !fields.complete
        if (fields.complete) {
            val permission = localNetworkPermission
            if (permission != null) networkPermission.launch(permission) else submit()
        }
    }
}

/** The name, address, username and password fields, and why the last attempt failed. */
@Composable
fun SourceFieldList(
    fields: SourceFields,
    error: UiText?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        val address = fields.url.text
        FilledField(
            label = stringResource(R.string.source_form_name_label),
            state = fields.name,
            hint =
                address
                    .takeIf { it.isNotBlank() }
                    ?.let { normalizeServerUrl(it.toString()).toUri().host }
                    ?: stringResource(R.string.source_form_name_hint),
            capitalization = KeyboardCapitalization.Words,
        )
        FilledField(
            label = stringResource(R.string.source_form_address_label),
            state = fields.url,
            hint = stringResource(fields.kind.urlHint),
            keyboardType = KeyboardType.Uri,
        )
        FilledField(
            label = stringResource(R.string.source_form_username_label),
            state = fields.username,
            hint = stringResource(R.string.source_form_username_hint),
        )
        PasswordField(
            label = stringResource(R.string.source_form_password_label),
            state = fields.password,
            hint =
                stringResource(
                    if (fields.needsPassword) {
                        R.string.source_form_password_hint
                    } else {
                        R.string.source_form_password_keep
                    },
                ),
            onDone = onDone,
        )
        val message =
            if (fields.showMissing) {
                stringResource(R.string.source_form_error_missing)
            } else {
                error?.asString()
            }
        if (message != null) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Danger,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
