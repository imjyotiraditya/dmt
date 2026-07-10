package dev.jyotiraditya.dmt

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.presentation.main.DmtScreen
import dev.jyotiraditya.dmt.presentation.main.SplashOverlay
import dev.jyotiraditya.dmt.presentation.player.DmtAction
import dev.jyotiraditya.dmt.presentation.player.PlayerEffect
import dev.jyotiraditya.dmt.presentation.player.PlayerViewModel
import dev.jyotiraditya.dmt.ui.theme.DMTTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DMTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val state by playerViewModel.state.collectAsState()
                    var showSplash by remember { mutableStateOf(true) }

                    LaunchedEffect(Unit) {
                        playerViewModel.effects.collect { effect ->
                            when (effect) {
                                is PlayerEffect.OpenEqualizer -> openEqualizer(
                                    effect.audioSessionId,
                                )
                            }
                        }
                    }

                    Box {
                        DmtScreen(
                            state = state,
                            dispatch = playerViewModel::onIntent,
                        )
                        AnimatedVisibility(
                            visible = showSplash,
                            exit = fadeOut(tween(450)),
                        ) {
                            SplashOverlay { showSplash = false }
                        }
                    }
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
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            playerViewModel.onIntent(DmtAction.NoEqualizer)
        }
    }
}
