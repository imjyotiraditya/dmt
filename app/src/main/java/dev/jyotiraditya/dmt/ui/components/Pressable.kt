package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.semantics.Role
import dev.jyotiraditya.dmt.ui.motion.PressSpring
import kotlinx.coroutines.launch

private const val PRESSED_SCALE = 0.97f

/** Click handling that shrinks the element to [sink] while pressed, in place of a ripple. */
fun Modifier.pressable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    sink: Float = PRESSED_SCALE,
    role: Role = Role.Button,
    label: String? = null,
): Modifier = combinedClickable(
    interactionSource = null,
    indication = ScaleIndication(sink),
    enabled = enabled,
    role = role,
    onClickLabel = label,
    onLongClick = onLongClick,
    onClick = onClick,
)

/** On and off handling for a row that holds a switch, shrinking it to [sink] while pressed. */
fun Modifier.pressableToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    sink: Float = PRESSED_SCALE,
    role: Role = Role.Switch,
): Modifier = toggleable(
    value = value,
    interactionSource = null,
    indication = ScaleIndication(sink),
    enabled = enabled,
    role = role,
    onValueChange = onValueChange,
)

/** Selection handling for one option of a group, shrinking it to [sink] while pressed. */
fun Modifier.pressableSelect(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    sink: Float = PRESSED_SCALE,
    role: Role = Role.RadioButton,
): Modifier = selectable(
    selected = selected,
    interactionSource = null,
    indication = ScaleIndication(sink),
    enabled = enabled,
    role = role,
    onClick = onClick,
)

private data class ScaleIndication(val pressedScale: Float) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        ScaleIndicationNode(interactionSource, pressedScale)
}

private class ScaleIndicationNode(
    private val interactionSource: InteractionSource,
    private val pressedScale: Float,
) : Modifier.Node(),
    DrawModifierNode {

    private val scale = Animatable(1f)

    override fun onAttach() {
        coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                val target =
                    when (interaction) {
                        is PressInteraction.Press -> pressedScale

                        is PressInteraction.Release,
                        is PressInteraction.Cancel,
                        -> 1f

                        else -> return@collect
                    }
                launch { scale.animateTo(target, PressSpring) }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        scale(scale.value) { this@draw.drawContent() }
    }
}
