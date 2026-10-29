package dev.jyotiraditya.dmt.ui.motion

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.ui.LocalNavAnimatedContentScope

val EaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

val PressSpring = spring<Float>(dampingRatio = 0.6f, stiffness = 700f)

val SurfaceSpring = spring<Float>(dampingRatio = 0.9f, stiffness = Spring.StiffnessMediumLow)

/** Material's standard motion without the bounce. */
val SettledMotion: MotionScheme =
    object : MotionScheme by MotionScheme.standard() {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(stiffness = 700f)
    }

const val PAGE_MS = 320

const val FADE_MS = 200

private const val PAGE_SHIFT_DIVISOR = 12

/** The shared element scope the screens are shown in, or null outside it. */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

private val BoundsTransform = BoundsTransform { _, _ -> tween(PAGE_MS, easing = EaseOut) }

/**
 * Slides in a screen opened on top of another when [forward], or the one under it when going back,
 * fading as it shifts.
 */
fun pageTransition(forward: Boolean): ContentTransform {
    val direction = if (forward) 1 else -1
    val shift = tween<IntOffset>(PAGE_MS, easing = EaseOut)
    val enter =
        fadeIn(tween(PAGE_MS - FADE_MS / 2, delayMillis = FADE_MS / 2)) +
            slideInHorizontally(shift) { direction * it / PAGE_SHIFT_DIVISOR }
    val exit =
        fadeOut(tween(FADE_MS / 2)) +
            slideOutHorizontally(shift) { -direction * it / PAGE_SHIFT_DIVISOR }
    return enter togetherWith exit
}

/** Moves this element to the one with the same [key] on the screen it is replaced by. */
@Composable
fun Modifier.sharedElement(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val visibility = LocalNavAnimatedContentScope.current
    return with(shared) {
        this@sharedElement.sharedElement(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibility,
            boundsTransform = BoundsTransform,
        )
    }
}
