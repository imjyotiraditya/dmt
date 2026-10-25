package dev.jyotiraditya.dmt.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The size of the window, not the device, so split screen and foldables are handled. */
data class WindowShape(val width: Dp, val height: Dp) {
    /** Room for two panes side by side: a tablet, an unfolded phone, or a phone on its side. */
    val wide: Boolean
        get() = width >= 600.dp || (width > height && height < 480.dp)

    /** Too short for a cover above the controls. */
    val short: Boolean
        get() = height < 480.dp

    /** Wide enough for a navigation rail instead of a top bar. */
    val rail: Boolean
        get() = width >= 840.dp || (width > height && width >= 600.dp)
}

/** The shape of the window this composition is shown in. */
@Composable
@ReadOnlyComposable
fun windowShape(): WindowShape {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { WindowShape(size.width.toDp(), size.height.toDp()) }
}
