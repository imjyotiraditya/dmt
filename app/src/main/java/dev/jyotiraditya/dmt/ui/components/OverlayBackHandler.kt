package dev.jyotiraditya.dmt.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/** Handles back for something shown over the screens, such as an open sheet, before they do. */
@Composable
fun OverlayBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val dispatcher =
        checkNotNull(LocalNavigationEventDispatcherOwner.current) {
            "No NavigationEventDispatcher was provided"
        }
            .navigationEventDispatcher
    val currentOnBack by rememberUpdatedState(onBack)
    val handler = remember {
        object :
            NavigationEventHandler<NavigationEventInfo>(
                initialInfo = NavigationEventInfo.None,
                isBackEnabled = enabled,
            ) {
            override fun onBackCompleted() = currentOnBack()
        }
    }
    SideEffect { handler.isBackEnabled = enabled }
    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
        onDispose { handler.remove() }
    }
}
