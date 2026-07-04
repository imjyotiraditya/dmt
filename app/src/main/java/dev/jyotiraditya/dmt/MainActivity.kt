package dev.jyotiraditya.dmt

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.jyotiraditya.dmt.ui.DmtAction
import dev.jyotiraditya.dmt.ui.DmtScreen
import dev.jyotiraditya.dmt.ui.PlayerViewModel
import dev.jyotiraditya.dmt.ui.SplashOverlay
import dev.jyotiraditya.dmt.ui.theme.AccentPalette
import dev.jyotiraditya.dmt.ui.theme.DMTTheme
import dev.jyotiraditya.dmt.ui.theme.LocalAccent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DMTTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: PlayerViewModel = viewModel()
                    val state by viewModel.state.collectAsState()
                    var showSplash by remember { mutableStateOf(true) }

                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { grants ->
                        viewModel.dispatch(
                            DmtAction.Permission(
                                grants[Manifest.permission.READ_MEDIA_AUDIO] == true
                            )
                        )
                    }
                    val requestPermissions = {
                        launcher.launch(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_AUDIO,
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        )
                    }
                    CompositionLocalProvider(
                        LocalAccent provides
                            AccentPalette[state.settings.accent % AccentPalette.size].second
                    ) {
                        Box {
                            DmtScreen(
                                state = state,
                                dispatch = viewModel::dispatch,
                                onRequestPermission = requestPermissions
                            )
                            AnimatedVisibility(
                                visible = showSplash,
                                exit = fadeOut(tween(450))
                            ) {
                                SplashOverlay { showSplash = false }
                            }
                        }
                    }
                }
            }
        }
    }
}
