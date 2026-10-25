package dev.jyotiraditya.dmt.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

val PressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 700f)

val SurfaceSpring = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

const val PAGE_MS = 320

const val FADE_MS = 200

/** The shared element scope of the nearest [Depth], or null outside one. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The visibility scope of the [Depth] page being composed, or null outside one. */
val LocalPageVisibility = compositionLocalOf<AnimatedVisibilityScope?> { null }

private val BoundsTransform = BoundsTransform { _, _ -> tween(PAGE_MS, easing = EaseOut) }

/** Moves this element to the one with the same [key] on the other page of a [Depth] change. */
@Composable
fun Modifier.sharedElement(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalPageVisibility.current ?: return this
    return with(shared) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibility,
            boundsTransform = BoundsTransform,
        )
    }
}

/**
 * Moves between a list and what was opened from it, shifting by [depthOf] and keeping each page's
 * scroll under [keyOf] while it is away.
 */
@Composable
fun <T> Depth(
    target: T,
    keyOf: (T) -> Any,
    depthOf: (T) -> Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val saved = rememberSaveableStateHolder()
    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = target,
            contentKey = keyOf,
            transitionSpec = {
                val direction = if (depthOf(targetState) >= depthOf(initialState)) 1 else -1
                val shift = tween<IntOffset>(PAGE_MS, easing = EaseOut)
                val enter =
                    fadeIn(tween(PAGE_MS - FADE_MS / 2, delayMillis = FADE_MS / 2)) +
                        slideInHorizontally(shift) { direction * it / PAGE_SHIFT_DIVISOR }
                val exit =
                    fadeOut(tween(FADE_MS / 2)) +
                        slideOutHorizontally(shift) { -direction * it / PAGE_SHIFT_DIVISOR }
                enter togetherWith exit
            },
            label = "depth",
        ) { page ->
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                LocalPageVisibility provides this,
            ) {
                saved.SaveableStateProvider(keyOf(page)) { content(page) }
            }
        }
    }
}

private const val PAGE_SHIFT_DIVISOR = 12
