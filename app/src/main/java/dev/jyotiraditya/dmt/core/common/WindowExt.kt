package dev.jyotiraditya.dmt.core.common

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

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
    val size = windowDpSize()
    return size.height < 480.dp && size.width < 840.dp
}
