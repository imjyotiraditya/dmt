package dev.jyotiraditya.dmt.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.domain.model.Cover
import dev.jyotiraditya.dmt.domain.model.Track
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.ui.theme.Surface2
import dev.jyotiraditya.dmt.util.fieldColor

/** The covers, provided at the root of the UI. */
val LocalArtwork = staticCompositionLocalOf<ArtworkRepository?> { null }

private const val ART_FADE_MS = 240
private const val FIELD_FADE_MS = 500

/** The size asked for when the space a cover fills is not bounded. */
private const val UNBOUNDED_ART_PX = 512

/** The cover of [track] for a space [sizePx] wide, at once when it is in memory. */
@Composable
fun rememberCover(track: Track?, sizePx: Int): Cover {
    val store = LocalArtwork.current
    val cached = track?.let { store?.cached(it, sizePx) }
    val cover by
        produceState(
            initialValue = cached?.let(Cover::Ready) ?: Cover.Loading,
            track?.id,
            sizePx,
            store,
        ) {
            if (track == null || store == null) {
                value = Cover.Missing
                return@produceState
            }
            value = store.load(track, sizePx)?.let(Cover::Ready) ?: Cover.Missing
        }
    return cover
}

/** The page colour for [track] from its cover, at once when cached. */
@Composable
fun rememberFieldColor(track: Track?): Color {
    val store = LocalArtwork.current
    val seed = track?.id ?: 0L
    val field by
        produceState(
            initialValue =
                track?.let { store?.cachedColor(it) }?.let(::Color) ?: fallbackField(seed),
            track?.id,
            store,
        ) {
            val color = track?.let { store?.color(it) }
            value = color?.let(::Color) ?: fallbackField(seed)
        }
    return field
}

/** The page colour for a decoded [bitmap], or a fallback picked by [seed]. */
@Composable
fun rememberFieldColor(bitmap: Bitmap?, seed: Long): Color {
    val field by
        produceState(fallbackField(seed), bitmap, seed) {
            value = bitmap?.fieldColor() ?: fallbackField(seed)
        }
    return field
}

/** [field] easing into each new colour, to be read while drawing. */
@Composable
fun animateFieldAsState(field: Color): State<Color> =
    animateColorAsState(field, tween(FIELD_FADE_MS), label = "field")

/** The cover of [track], decoded at its drawn size, on its colour until it loads. */
@Composable
fun Artwork(track: Track?, modifier: Modifier = Modifier, shape: Shape = RectangleShape) {
    val store = LocalArtwork.current
    BoxWithConstraints(modifier = modifier) {
        val sizePx =
            maxOf(constraints.maxWidth, constraints.maxHeight).takeIf {
                it != Constraints.Infinity && it > 0
            } ?: UNBOUNDED_ART_PX
        Artwork(
            cover = rememberCover(track, sizePx),
            modifier = Modifier.matchParentSize(),
            shape = shape,
            placeholder = track?.let { store?.cachedColor(it) }?.let(::Color) ?: Surface2,
        )
    }
}

@Composable
fun Artwork(
    cover: Cover,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    placeholder: Color = Surface2,
) {
    Box(modifier = modifier.clip(shape).background(placeholder)) {
        Crossfade(targetState = cover, animationSpec = tween(ART_FADE_MS), label = "artwork") {
            when (it) {
                is Cover.Ready -> {
                    val image = remember(it.bitmap) { it.bitmap.asImageBitmap() }
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Medium,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Cover.Missing ->
                    Image(
                        painter = painterResource(R.drawable.cover_missing),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )

                Cover.Loading -> Unit
            }
        }
    }
}

/** A page colour picked by [seed] for a track without a cover colour. */
fun fallbackField(seed: Long): Color =
    FallbackFields[seed.mod(FallbackFields.size.toLong()).toInt()]

private val FallbackFields =
    listOf(Color(0xFF2E4A3A), Color(0xFF3A3350), Color(0xFF4A3330), Color(0xFF2E3C4A))
