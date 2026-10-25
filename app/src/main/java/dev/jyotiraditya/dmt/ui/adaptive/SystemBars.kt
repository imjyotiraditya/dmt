package dev.jyotiraditya.dmt.ui.adaptive

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.inkOn

/** Sets the system bar icons for content on [behind] while composed, restoring them after. */
@Composable
fun SystemBarsOver(behind: Color) {
    val view = LocalView.current
    val light = behind.inkOn() != Ink
    DisposableEffect(view, light) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = light
        controller?.isAppearanceLightNavigationBars = light
        onDispose {
            if (controller != null && previous != null) {
                controller.isAppearanceLightStatusBars = previous
                controller.isAppearanceLightNavigationBars = previous
            }
        }
    }
}
