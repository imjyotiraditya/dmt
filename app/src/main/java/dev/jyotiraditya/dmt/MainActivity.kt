package dev.jyotiraditya.dmt

import android.content.Intent
import android.graphics.Color
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.presentation.main.DmtScreen
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.PlayerEffect
import dev.jyotiraditya.dmt.presentation.player.PlayerViewModel
import dev.jyotiraditya.dmt.ui.theme.DMTTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            DMTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by playerViewModel.state.collectAsState()
                    val writeLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.StartIntentSenderForResult(),
                    ) { result ->
                        playerViewModel.onIntent(
                            DmtAction.EmbedLyrics(result.resultCode == RESULT_OK),
                        )
                    }

                    LaunchedEffect(Unit) {
                        playerViewModel.effects.collect { effect ->
                            when (effect) {
                                is PlayerEffect.OpenEqualizer -> openEqualizer(
                                    effect.audioSessionId,
                                )

                                is PlayerEffect.RequestWrite -> writeLauncher.launch(
                                    IntentSenderRequest.Builder(effect.intentSender).build(),
                                )
                            }
                        }
                    }

                    DmtScreen(
                        state = state,
                        dispatch = playerViewModel::onIntent,
                    )
                }
            }
        }
    }

    private fun openEqualizer(audioSessionId: Int) {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
        val handler = packageManager.queryIntentActivities(intent, 0)
            .firstOrNull { it.activityInfo.packageName != "com.android.musicfx" }
        if (handler != null) {
            intent.setClassName(handler.activityInfo.packageName, handler.activityInfo.name)
            startActivity(intent)
        } else {
            playerViewModel.onIntent(DmtAction.NoEqualizer)
        }
    }
}
