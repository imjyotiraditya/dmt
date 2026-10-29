package dev.jyotiraditya.dmt.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dev.jyotiraditya.dmt.R

/** A back stack for each [Place], which place is shown, and the library shelf in view. */
@Stable
class Navigator
internal constructor(
    private val stacks: Map<Place, NavBackStack<NavKey>>,
    placeState: MutableState<Place>,
    shelfState: MutableState<Shelf>,
) {
    var place by placeState
        private set

    /** The library shelf shown under its tabs. */
    var shelf by shelfState
        private set

    /** The back stack of the place shown. */
    val backStack: NavBackStack<NavKey>
        get() = stack(place)

    /** Whether the screen shown sits on top of others in its place. */
    val nested: Boolean
        get() = backStack.size > 1

    fun stack(place: Place): NavBackStack<NavKey> = stacks.getValue(place)

    /** Opens [destination] on top of the screen shown. */
    fun open(destination: Destination) {
        backStack.add(destination)
    }

    /** Shows [place] as it was left, or takes it back to its first screen when already shown. */
    fun show(place: Place) {
        if (place == this.place) {
            backStack.retainAll { it == place.root }
        } else {
            this.place = place
        }
    }

    fun showShelf(shelf: Shelf) {
        this.shelf = shelf
    }

    /** Shows the library's first screen on [shelf], from wherever the user is. */
    fun openLibrary(shelf: Shelf) {
        this.shelf = shelf
        stack(Place.LIBRARY).retainAll { it == Place.LIBRARY.root }
        place = Place.LIBRARY
    }

    /** Shows [destination] in the library over [shelf], from wherever the user is. */
    fun openInLibrary(shelf: Shelf, destination: Destination) {
        this.shelf = shelf
        openIn(Place.LIBRARY, destination)
    }

    /** Shows [destination] over the first screen of [place], from wherever the user is. */
    fun openIn(place: Place, destination: Destination) {
        stack(place).apply {
            retainAll { it == place.root }
            add(destination)
        }
        this.place = place
    }

    /** Leaves every screen opened on top of [destination] in the place shown. */
    fun backTo(destination: Destination) {
        val index = backStack.lastIndexOf(destination)
        if (index >= 0) backStack.subList(index + 1, backStack.size).clear()
    }

    /** Leaves the screen shown, or returns to Listen from the first screen of another place. */
    fun back() {
        when {
            nested -> backStack.removeLastOrNull()
            place != Place.LISTEN -> place = Place.LISTEN
        }
    }

    /** Whether [back] has somewhere to go. */
    val canGoBack: Boolean
        get() = nested || place != Place.LISTEN
}

@Composable
fun rememberNavigator(): Navigator {
    val listen = rememberNavBackStack(Place.LISTEN.root)
    val library = rememberNavBackStack(Place.LIBRARY.root)
    val settings = rememberNavBackStack(Place.SETTINGS.root)
    val place = rememberSaveable { mutableStateOf(Place.LISTEN) }
    val shelf = rememberSaveable { mutableStateOf(Shelf.TRACKS) }
    return remember(listen, library, settings) {
        Navigator(
            stacks =
                mapOf(Place.LISTEN to listen, Place.LIBRARY to library, Place.SETTINGS to settings),
            placeState = place,
            shelfState = shelf,
        )
    }
}

/**
 * The name of the screen under the one shown, for its back link; null at a place's first screen.
 */
@Composable
fun Navigator.backLabel(): String? {
    if (!nested) return null
    return when (val under = backStack[backStack.lastIndex - 1]) {
        Destination.Library -> stringResource(shelf.label)
        is Destination.Artist -> under.name
        Destination.Sources -> stringResource(R.string.sources_title)
        Destination.SourceKinds -> stringResource(R.string.sources_add)
        else -> stringResource(place.label)
    }
}
