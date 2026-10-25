package dev.jyotiraditya.dmt.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.core.common.UiText
import dev.jyotiraditya.dmt.core.common.asString
import dev.jyotiraditya.dmt.ui.theme.Danger
import dev.jyotiraditya.dmt.ui.theme.Ink
import dev.jyotiraditya.dmt.ui.theme.Margin
import dev.jyotiraditya.dmt.ui.theme.SecondaryAlpha
import dev.jyotiraditya.dmt.ui.theme.Surface2

/** A bar showing [error], or [notice] when there is no error. */
@Composable
fun Notice(error: UiText?, notice: UiText?, modifier: Modifier = Modifier) {
    val message = (error ?: notice)?.asString()?.let { it to (error != null) }
    // The outgoing message stays drawn while it slides away.
    AnimatedContent(
        targetState = message,
        transitionSpec = {
            (fadeIn() + slideInVertically { it }) togetherWith
                (fadeOut() + slideOutVertically { it })
        },
        label = "notice",
        modifier = modifier,
    ) { shown ->
        if (shown != null) {
            Text(
                text = shown.first,
                style = MaterialTheme.typography.bodyMedium,
                color = if (shown.second) Danger else Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier.fillMaxWidth()
                        .background(Surface2)
                        .padding(horizontal = Margin, vertical = 14.dp),
            )
        }
    }
}

/** An empty page's title and explanation, with an optional [action]. */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    ink: Color = Ink,
    action: (@Composable () -> Unit)? = null,
) {
    Column(modifier = modifier.padding(vertical = 32.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge, color = ink)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = ink.copy(alpha = SecondaryAlpha),
            modifier = Modifier.padding(top = 12.dp).widthIn(max = 460.dp),
        )
        if (action != null) {
            Spacer(modifier = Modifier.height(24.dp))
            action()
        }
    }
}

/** Keeps the scroll and other saved state of [content] under [key] while it is away. */
@Composable
fun ScrollMemory(key: Any, content: @Composable () -> Unit) {
    rememberSaveableStateHolder().SaveableStateProvider(key, content)
}
