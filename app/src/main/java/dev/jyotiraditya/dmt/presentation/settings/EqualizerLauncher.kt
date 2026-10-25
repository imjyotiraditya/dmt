package dev.jyotiraditya.dmt.presentation.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Remembers a launcher for the equalizer app, or null if none is installed. */
@Composable
fun rememberEqualizerLauncher(): (() -> Unit)? {
    val context = LocalContext.current
    val intent = remember { equalizerIntent(context.packageName) }
    val installed = remember {
        context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
    }
    // Started for a result so the panel knows the calling app.
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    if (!installed) return null
    return {
        try {
            launcher.launch(intent)
        } catch (_: ActivityNotFoundException) {}
    }
}

/** The request for an equalizer panel for [packageName]'s music playback. */
private fun equalizerIntent(packageName: String) =
    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
