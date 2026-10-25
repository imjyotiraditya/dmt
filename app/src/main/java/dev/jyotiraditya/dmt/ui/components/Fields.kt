package dev.jyotiraditya.dmt.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.ui.icons.DmtIcons
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Muted
import dev.jyotiraditya.dmt.ui.theme.Surface

/** A labelled one-line field on a flat filled block that sends input exactly as typed. */
@Composable
fun FilledField(
    label: String,
    state: TextFieldState,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onDone: (() -> Unit)? = null,
) {
    TextField(
        state = state,
        label = { Text(label) },
        placeholder = { Text(hint, maxLines = 1) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions =
            KeyboardOptions(
                capitalization = capitalization,
                autoCorrectEnabled = false,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
        onKeyboardAction = onDone?.let { done -> KeyboardActionHandler { done() } },
        colors = flatFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A labelled password field on a flat filled block, hidden until its eye is tapped. */
@Composable
fun PasswordField(
    label: String,
    state: TextFieldState,
    hint: String,
    modifier: Modifier = Modifier,
    onDone: (() -> Unit)? = null,
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    SecureTextField(
        state = state,
        label = { Text(label) },
        placeholder = { Text(hint, maxLines = 1) },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        trailingIcon = {
            IconTap(
                icon = if (shown) DmtIcons.Hide else DmtIcons.Show,
                description =
                    stringResource(
                        if (shown) R.string.field_password_hide else R.string.field_password_show,
                    ),
                onClick = { shown = !shown },
                size = 40.dp,
                iconSize = 22.dp,
            )
        },
        textObfuscationMode =
            if (shown) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        onKeyboardAction = onDone?.let { done -> KeyboardActionHandler { done() } },
        colors = flatFieldColors(),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Field colours for a filled block with no underline. */
@Composable
private fun flatFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedTextColor = Ink,
    unfocusedTextColor = Ink,
    focusedContainerColor = Surface,
    unfocusedContainerColor = Surface,
    cursorColor = Ink,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    focusedLabelColor = Ink,
    unfocusedLabelColor = Ink,
    focusedPlaceholderColor = Muted,
    unfocusedPlaceholderColor = Muted,
)
