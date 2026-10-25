package dev.jyotiraditya.dmt.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.ui.motion.PressSpring
import dev.jyotiraditya.dmt.ui.theme.Ground
import dev.jyotiraditya.dmt.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_REPEAT_MS = 200L

@Composable
fun DmtIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = Ink,
    size: Dp = 24.dp,
    description: String? = null,
) {
    Icon(
        painter = painterResource(icon),
        contentDescription = description,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/**
 * A tappable icon, filled while [active]; [onHold] repeats while held, in place of a long press.
 */
@Composable
fun IconTap(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ink: Color = Ink,
    field: Color = Ground,
    active: Boolean = false,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    onLongClick: (() -> Unit)? = null,
    onHold: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val fill by animateColorAsState(if (active) ink else Color.Transparent, label = "iconFill")
    val glyph by animateColorAsState(if (active) field else ink, label = "iconGlyph")
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, PressSpring, label = "iconPress")
    val hold by rememberUpdatedState(onHold)
    // Repeats the hold action until the press ends.
    val repeat: (() -> Unit)? = onHold?.let {
        {
            scope.launch {
                while (pressed) {
                    hold?.invoke()
                    delay(HOLD_REPEAT_MS)
                }
            }
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (enabled) 1f else 0.35f
                }
                .background { fill }
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClickLabel = description,
                    onLongClick = repeat ?: onLongClick,
                    onClick = onClick,
                ),
    ) {
        DmtIcon(icon = icon, tint = glyph, size = iconSize, description = description)
    }
}
