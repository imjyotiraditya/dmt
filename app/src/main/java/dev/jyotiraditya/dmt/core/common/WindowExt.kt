package dev.jyotiraditya.dmt.core.common

import android.content.res.Configuration
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import androidx.window.core.layout.WindowSizeClass

@Composable
fun windowDpSize(): DpSize =
    with(LocalDensity.current) {
        LocalWindowInfo.current.containerSize.toSize().toDpSize()
    }

@Composable
fun fitScaleFor(designHeightDp: Float, minScale: Float): Float =
    (windowDpSize().height.value / designHeightDp).coerceIn(minScale, 1f)

@Composable
fun isLandscapeWindow(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

@Composable
fun isCompactWindow(): Boolean {
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val shortWindow =
        !sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
    val narrowWindow =
        !sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
    return shortWindow && narrowWindow
}
