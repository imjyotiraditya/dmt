package dev.jyotiraditya.dmt.presentation.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "DMT_WIDGET_PROVIDER"

/**
 * Lifecycle host for the DMT compact home-screen widget.
 *
 * This class is intentionally thin — it only handles Android widget lifecycle events.
 * All display logic lives in [WidgetUpdater].
 * All command routing lives in [WidgetActionReceiver].
 * All state persistence lives in [WidgetPlaybackStateRepository].
 *
 * Coroutine safety:
 *   Async work (artwork load) uses [goAsync] with a per-call [CoroutineScope].
 *   There is no long-lived provider-level scope.
 */
class CompactWidgetProvider : AppWidgetProvider() {

    /**
     * Called by Android when widgets need to be drawn:
     *   - Initial placement
     *   - After device reboot (launcher re-creates placed widgets)
     *   - After package update
     *
     * We immediately render the last-known cached state so the widget
     * is never an unexplained blank rectangle.
     */
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Log.d(TAG, "onUpdate: ${appWidgetIds.size} instance(s)")
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // Sync render from cache — no IO, no blank flash
        WidgetUpdater.updateAllSync(context)

        // Async re-render with artwork
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope.launch {
            try {
                WidgetUpdater.updateAll(context)
            } catch (e: Exception) {
                Log.e(TAG, "onUpdate async refresh failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Called when the first widget instance is placed.
     * Render cached state immediately.
     */
    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled")
        super.onEnabled(context)
        WidgetUpdater.updateAllSync(context)
    }

    /**
     * Called when specific widget instances are removed by the user.
     * No state cleanup needed — kept so future re-adds restore the last state.
     */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted: ${appWidgetIds.size} instance(s)")
        super.onDeleted(context, appWidgetIds)
    }

    /**
     * Called when the last widget instance is removed.
     * Clear artwork cache to free memory; state prefs are kept for re-adds.
     */
    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled")
        super.onDisabled(context)
        WidgetArtworkCache.clear()
        WidgetPlaybackStateRepository.invalidateCache()
    }
}
