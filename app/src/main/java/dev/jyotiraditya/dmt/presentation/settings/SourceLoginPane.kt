package dev.jyotiraditya.dmt.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.Caption
import dev.jyotiraditya.dmt.core.common.TuiKey
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.DmtView
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import dev.jyotiraditya.dmt.ui.theme.TuiFg
import dev.jyotiraditya.dmt.ui.theme.TuiLine
import dev.jyotiraditya.dmt.util.localNetworkPermission

@Composable
fun SourceLoginPane(mode: SourceMode, dispatch: (DmtAction) -> Unit) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showMissing by remember { mutableStateOf(false) }

    val submitLogin = {
        dispatch(DmtAction.SourceLogin(mode, url.trim(), username.trim(), password))
    }
    val networkPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        submitLogin()
    }
    val connect = {
        val permission = localNetworkPermission
        if (url.isBlank() || username.isBlank()) {
            showMissing = true
        } else if (permission != null) {
            showMissing = false
            networkPermissionLauncher.launch(permission)
        } else {
            showMissing = false
            submitLogin()
        }
    }

    Column {
        Caption(stringResource(R.string.source_login_title, mode.label))

        LoginField(
            label = stringResource(R.string.source_url_label),
            value = url,
            hint = stringResource(R.string.source_url_hint),
            onValueChange = { url = it },
        )
        LoginField(
            label = stringResource(R.string.source_user_label),
            value = username,
            hint = stringResource(R.string.source_user_hint),
            onValueChange = { username = it },
        )
        LoginField(
            label = stringResource(R.string.source_pass_label),
            value = password,
            hint = stringResource(R.string.source_pass_hint),
            onValueChange = { password = it },
            keyboardType = KeyboardType.Password,
            mask = true,
        )

        Row(modifier = Modifier.padding(top = 18.dp)) {
            TuiKey(
                label = "[ ${stringResource(R.string.source_login_cancel)} ]",
                onClick = { dispatch(DmtAction.Show(DmtView.SOURCES)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            TuiKey(
                label = "[ ${stringResource(R.string.source_connect)} ]",
                bright = true,
                onClick = connect,
            )
        }

        if (showMissing) {
            Text(
                text = stringResource(R.string.source_login_missing),
                style = MaterialTheme.typography.labelSmall,
                color = LocalAccent.current,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    mask: Boolean = false,
) {
    val accent = LocalAccent.current

    Column(modifier = Modifier.padding(bottom = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
        ) {
            Text(
                text = label.padEnd(6),
                style = MaterialTheme.typography.labelMedium,
                color = TuiDim,
            )
            Text(
                text = " > ",
                style = MaterialTheme.typography.bodyLarge,
                color = accent,
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TuiFg,
                    fontFeatureSettings = if (mask) "calt off" else null,
                ),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (mask) {
                    PasswordVisualTransformation('*')
                } else {
                    VisualTransformation.None
                },
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TuiDim.copy(alpha = 0.55f),
                        )
                    }
                    inner()
                },
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
