package dev.jyotiraditya.dmt.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import dev.jyotiraditya.dmt.MainActivity
import dev.jyotiraditya.dmt.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "DMT_WIDGET_UPDATER"

// Stable request codes — must never collide across PendingIntents.
private const val REQ_PREV = 1001
private const val REQ_PLAY_PAUSE = 1002
private const val REQ_NEXT = 1003
private const val REQ_OPEN_APP = 1004

/**
 * Single authoritative source of widget [RemoteViews] construction and delivery.
 *
 * Implements high-fidelity pre-rendering of text onto Bitmaps using the app's
 * true JetBrains Mono font assets. This guarantees the typeface, weights, and
 * spacing look identical to the in-app player across all device launchers,
 * avoiding system font loading limitations.
 */
object WidgetUpdater {

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Build [RemoteViews] from current [WidgetPlaybackStateRepository] state,
     * load ASCII artwork asynchronously, and push to every active widget instance.
     * Must be called from a coroutine.
     */
    suspend fun updateAll(context: Context) {
        val appContext = context.applicationContext
        val state = WidgetPlaybackStateRepository.load(appContext)
        Log.d(TAG, "updateAll title='${state.title}' isPlaying=${state.isPlaying}")

        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, CompactWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return

        val views = buildViews(appContext, state)

        // Asynchronously load the ASCII artwork and set the bitmap
        if (state.hasTrack) {
            val artworkUri = state.artworkUriString?.let { Uri.parse(it) }
            val trackUri = state.trackUriString?.let { Uri.parse(it) }
            val bitmap = withContext(Dispatchers.IO) {
                val cached = WidgetArtworkCache.get(appContext, artworkUri, trackUri)
                if (cached != null) {
                    cached
                } else {
                    // Generate procedural ASCII placeholder using title + artist hash as seed
                    val seed = (state.title + state.artist).hashCode().toLong()
                    val placeholder = dev.jyotiraditya.dmt.core.common.generateAsciiPlaceholder(
                        appContext,
                        seed = seed,
                        cols = 32
                    )
                    // Scale to fit target dimensions cleanly
                    val density = appContext.resources.displayMetrics.density
                    val targetPx = (46f * density).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(placeholder, targetPx, targetPx, true)
                    if (scaled !== placeholder) placeholder.recycle()
                    scaled
                }
            }
            if (bitmap != null) {
                views.setImageViewBitmap(R.id.widget_artwork, bitmap)
            }
        }

        manager.updateAppWidget(ids, views)
        Log.d(TAG, "updateAll: pushed to ${ids.size} instance(s)")
    }

    /**
     * Synchronous variant — safe to call from [CompactWidgetProvider.onUpdate]
     * without starting a coroutine. Artwork is shown as placeholder initially.
     */
    fun updateAllSync(context: Context) {
        val appContext = context.applicationContext
        val state = WidgetPlaybackStateRepository.load(appContext)

        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(
            ComponentName(appContext, CompactWidgetProvider::class.java),
        )
        if (ids.isEmpty()) return

        manager.updateAppWidget(ids, buildViews(appContext, state))
    }

    // -----------------------------------------------------------------------
    // View construction
    // -----------------------------------------------------------------------

    /** Build RemoteViews synchronously — no coroutine, no artwork IO needed. */
    private fun buildViews(context: Context, state: WidgetPlaybackState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_compact)
        if (state.hasTrack) {
            applyTrackState(context, views, state)
        } else {
            applyIdleState(context, views)
        }
        attachPendingIntents(context, views)
        return views
    }

    // -----------------------------------------------------------------------
    // Track state
    // -----------------------------------------------------------------------

    private fun applyTrackState(context: Context, views: RemoteViews, state: WidgetPlaybackState) {
        // 1. Render Track Title — bodyMedium (13sp JetBrains Mono, Normal weight, with accent cursor)
        val titleText = (state.title.ifBlank { "unknown track" }) + "_"
        val titleBmp = renderTextToBitmap(
            context = context,
            text = titleText,
            widthDp = 240f,
            heightDp = 20f,
            textSizeSp = 13f,
            isBold = false,
            textColor = 0xFFDFE4E5.toInt(),
            isCentered = false,
            hasAccentCursor = true
        )
        views.setImageViewBitmap(R.id.widget_title, titleBmp)

        // 2. Render Artist Name — labelSmall (10sp JetBrains Mono, Normal weight)
        val artistText = state.artist.ifBlank { "unknown artist" }.lowercase()
        val artistBmp = renderTextToBitmap(
            context = context,
            text = artistText,
            widthDp = 240f,
            heightDp = 16f,
            textSizeSp = 10f,
            isBold = false,
            textColor = 0xFF767D80.toInt(),
            isCentered = false,
            hasAccentCursor = false
        )
        views.setImageViewBitmap(R.id.widget_artist, artistBmp)

        // 3. Render Button glyphs in Solid White (drawing is tinted dynamically in XML)
        val prevBmp = renderTextToBitmap(context, "| <<", 52f, 36f, 13f, true, 0xFFFFFFFF.toInt())
        val playPauseBmp = renderTextToBitmap(
            context = context,
            text = if (state.isPlaying) "||" else "▷",
            widthDp = 44f,
            heightDp = 36f,
            textSizeSp = 13f,
            isBold = true,
            textColor = 0xFFFFFFFF.toInt()
        )
        val nextBmp = renderTextToBitmap(context, ">> |", 52f, 36f, 13f, true, 0xFFFFFFFF.toInt())

        views.setImageViewBitmap(R.id.widget_prev, prevBmp)
        views.setImageViewBitmap(R.id.widget_play_pause, playPauseBmp)
        views.setImageViewBitmap(R.id.widget_next, nextBmp)

        // Progress bar (0–100)
        val progress = if (state.durationMs > 0)
            (state.positionMs * 100 / state.durationMs).toInt().coerceIn(0, 100)
        else 0
        views.setProgressBar(R.id.widget_progress, 100, progress, false)

        // Display the artwork image view container and clear previous image
        views.setViewVisibility(R.id.widget_artwork, View.VISIBLE)
        views.setImageViewBitmap(R.id.widget_artwork, null)
    }

    // -----------------------------------------------------------------------
    // Idle state
    // -----------------------------------------------------------------------

    private fun applyIdleState(context: Context, views: RemoteViews) {
        // 1. Render Idle Title
        val idleBmp = renderTextToBitmap(
            context = context,
            text = "nothing playing_",
            widthDp = 240f,
            heightDp = 20f,
            textSizeSp = 13f,
            isBold = false,
            textColor = 0xFFDFE4E5.toInt(),
            isCentered = false,
            hasAccentCursor = true
        )
        views.setImageViewBitmap(R.id.widget_title, idleBmp)

        // 2. Clear Artist Name
        val emptyBmp = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        views.setImageViewBitmap(R.id.widget_artist, emptyBmp)

        // 3. Render Buttons
        val prevBmp = renderTextToBitmap(context, "| <<", 52f, 36f, 13f, true, 0xFFFFFFFF.toInt())
        val playBmp = renderTextToBitmap(context, "▷", 44f, 36f, 13f, true, 0xFFFFFFFF.toInt())
        val nextBmp = renderTextToBitmap(context, ">> |", 52f, 36f, 13f, true, 0xFFFFFFFF.toInt())

        views.setImageViewBitmap(R.id.widget_prev, prevBmp)
        views.setImageViewBitmap(R.id.widget_play_pause, playBmp)
        views.setImageViewBitmap(R.id.widget_next, nextBmp)

        views.setProgressBar(R.id.widget_progress, 100, 0, false)

        // Hide artwork container when idle
        views.setViewVisibility(R.id.widget_artwork, View.GONE)
    }

    // -----------------------------------------------------------------------
    // Pre-rendering Text-To-Bitmap Engine
    // -----------------------------------------------------------------------

    private fun renderTextToBitmap(
        context: Context,
        text: String,
        widthDp: Float,
        heightDp: Float,
        textSizeSp: Float,
        isBold: Boolean,
        textColor: Int,
        isCentered: Boolean = true,
        hasAccentCursor: Boolean = false
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt().coerceAtLeast(1)
        val heightPx = (heightDp * density).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            isAntiAlias = true
            color = textColor
            textSize = textSizeSp * context.resources.displayMetrics.scaledDensity
            // Force load the app's packaged JetBrains Mono resources
            val fontRes = if (isBold) R.font.jetbrains_mono_bold else R.font.jetbrains_mono
            typeface = ResourcesCompat.getFont(context, fontRes)
        }

        // Truncate text to fit container if drawing title/artist
        val textToDraw = if (!isCentered) {
            val maxTextWidth = widthPx - 4f
            truncateText(paint, text, maxTextWidth)
        } else {
            text
        }

        val fontMetrics = paint.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        val textOffset = textHeight / 2 - fontMetrics.descent
        val y = heightPx / 2f + textOffset

        if (isCentered) {
            paint.textAlign = Paint.Align.CENTER
            val x = widthPx / 2f
            canvas.drawText(textToDraw, x, y, paint)
        } else {
            paint.textAlign = Paint.Align.LEFT
            val x = 0f
            if (hasAccentCursor) {
                val hasUnderscore = textToDraw.endsWith("_")
                val baseText = if (hasUnderscore) textToDraw.substring(0, textToDraw.length - 1) else textToDraw
                
                canvas.drawText(baseText, x, y, paint)
                
                if (hasUnderscore) {
                    val baseWidth = paint.measureText(baseText)
                    paint.color = context.getColor(R.color.tui_accent)
                    canvas.drawText("_", x + baseWidth, y, paint)
                }
            } else {
                canvas.drawText(textToDraw, x, y, paint)
            }
        }

        return bitmap
    }

    private fun truncateText(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        
        val suffix = if (text.endsWith("_")) "_..." else "..."
        val hasCursor = text.endsWith("_")
        val cleanText = if (hasCursor) text.substring(0, text.length - 1) else text

        var truncated = cleanText
        while (truncated.isNotEmpty() && paint.measureText(truncated + suffix) > maxWidth) {
            truncated = truncated.substring(0, truncated.length - 1)
        }

        return if (truncated.isNotEmpty()) truncated + suffix else suffix
    }

    // -----------------------------------------------------------------------
    // PendingIntents — prev + play/pause + next + open-app
    // -----------------------------------------------------------------------

    private fun attachPendingIntents(context: Context, views: RemoteViews) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        val prevIntent = PendingIntent.getBroadcast(
            context, REQ_PREV,
            Intent(ACTION_WIDGET_PREV).apply {
                component = ComponentName(context, WidgetActionReceiver::class.java)
            },
            flags,
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            context, REQ_PLAY_PAUSE,
            Intent(ACTION_WIDGET_PLAY_PAUSE).apply {
                component = ComponentName(context, WidgetActionReceiver::class.java)
            },
            flags,
        )
        val nextIntent = PendingIntent.getBroadcast(
            context, REQ_NEXT,
            Intent(ACTION_WIDGET_NEXT).apply {
                component = ComponentName(context, WidgetActionReceiver::class.java)
            },
            flags,
        )
        val openAppIntent = PendingIntent.getActivity(
            context, REQ_OPEN_APP,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            flags,
        )

        views.setOnClickPendingIntent(R.id.widget_prev, prevIntent)
        views.setOnClickPendingIntent(R.id.widget_play_pause, playPauseIntent)
        views.setOnClickPendingIntent(R.id.widget_next, nextIntent)
        views.setOnClickPendingIntent(R.id.widget_text_container, openAppIntent)
        views.setOnClickPendingIntent(R.id.widget_artwork, openAppIntent)
    }
}
