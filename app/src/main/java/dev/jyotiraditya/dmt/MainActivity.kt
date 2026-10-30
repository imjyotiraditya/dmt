package dev.jyotiraditya.dmt

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.jyotiraditya.dmt.domain.model.bitmap
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.presentation.main.AppViewModel
import dev.jyotiraditya.dmt.presentation.main.DmtScreen
import dev.jyotiraditya.dmt.presentation.main.SetupScreen
import dev.jyotiraditya.dmt.presentation.nowplaying.NowPlayingViewModel
import dev.jyotiraditya.dmt.presentation.nowplaying.seed
import dev.jyotiraditya.dmt.presentation.settings.LocalSourceDisplays
import dev.jyotiraditya.dmt.ui.components.rememberFieldColor
import dev.jyotiraditya.dmt.ui.theme.DMTTheme
import dev.jyotiraditya.dmt.ui.theme.accentFor
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var artwork: ArtworkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val app: AppViewModel = hiltViewModel()
            val nowPlaying: NowPlayingViewModel = hiltViewModel()
            val state by app.state.collectAsStateWithLifecycle()
            val playing by nowPlaying.state.collectAsStateWithLifecycle()
            val cover = rememberFieldColor(playing.cover.bitmap, playing.seed)
            DMTTheme(accent = accentFor(state.accent, cover)) {
                CompositionLocalProvider(LocalSourceDisplays provides app.displays) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        when {
                            !state.loaded -> Unit
                            !state.setupDone -> SetupScreen()
                            else -> DmtScreen(field = cover, artwork = artwork)
                        }
                    }
                }
            }
        }
    }
}
