package dev.jyotiraditya.dmt.presentation.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.core.common.rememberCursorAlpha
import dev.jyotiraditya.dmt.ui.theme.LocalAccent
import dev.jyotiraditya.dmt.ui.theme.TuiBg
import dev.jyotiraditya.dmt.ui.theme.TuiBright
import dev.jyotiraditya.dmt.ui.theme.TuiDim
import kotlin.math.roundToInt

private const val TYPE_MS = 700
private const val SPLASH_MS = 2000

@Composable
private fun rememberTypedCount(length: Int, onDone: () -> Unit): Int {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = SPLASH_MS
                0f at 0
                1f at TYPE_MS using LinearEasing
                1f at SPLASH_MS
            },
        )
        onDone()
    }
    return (length * progress.value).roundToInt()
}

@Composable
fun SplashOverlay(onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TuiBg),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(LocalAccent.current),
                )
                Text(
                    text = " " + stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = TuiBright,
                )
            }
            TypedTagline(
                onDone = onDone,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun TypedTagline(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val tagline = stringResource(R.string.tagline)
    val typed = rememberTypedCount(tagline.length, onDone)
    val cursorAlpha = rememberCursorAlpha()
    val accent = LocalAccent.current
    Box(modifier = modifier) {
        Text(
            text = "${tagline}_",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Transparent,
        )
        Text(
            text = buildAnnotatedString {
                append(tagline.take(typed))
                withStyle(SpanStyle(color = accent.copy(alpha = cursorAlpha))) {
                    append("_")
                }
            },
            style = MaterialTheme.typography.labelMedium,
            color = TuiDim,
        )
    }
}
