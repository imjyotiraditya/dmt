package dev.jyotiraditya.dmt

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.domain.model.bitmap
import dev.jyotiraditya.dmt.presentation.main.DmtScreen
import dev.jyotiraditya.dmt.presentation.main.SetupScreen
import dev.jyotiraditya.dmt.presentation.player.PlayerViewModel
import dev.jyotiraditya.dmt.presentation.player.nowPlayingSeed
import dev.jyotiraditya.dmt.ui.components.rememberFieldColor
import dev.jyotiraditya.dmt.ui.theme.DMTTheme
import dev.jyotiraditya.dmt.ui.theme.accentFor

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
            val state by playerViewModel.state.collectAsStateWithLifecycle()
            val player by playerViewModel.player.collectAsStateWithLifecycle()
            val cover = rememberFieldColor(state.cover.bitmap, state.nowPlayingSeed)
            DMTTheme(accent = accentFor(state.settings.accent, cover)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when {
                        !state.settingsLoaded -> Unit

                        !state.settings.setupDone ->
                            SetupScreen(
                                state = state,
                                dispatch = playerViewModel::onIntent,
                            )

                        else ->
                            DmtScreen(
                                state = state,
                                player = player,
                                field = cover,
                                dispatch = playerViewModel::onIntent,
                                artwork = playerViewModel.artwork,
                            )
                    }
                }
            }
        }
    }
}
