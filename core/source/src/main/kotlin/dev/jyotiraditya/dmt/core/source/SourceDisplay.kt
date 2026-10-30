package dev.jyotiraditya.dmt.core.source

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/** How a kind of source is named and shown, and what it says when something goes wrong. */
data class SourceDisplay(
    @param:StringRes val title: Int,
    /** A one-line description. */
    @param:StringRes val line: Int,
    /** The description shown when picking a source during setup. */
    @param:StringRes val setupDetail: Int,
    @param:DrawableRes val icon: Int,
    /** The message when the source holds no tracks. */
    @param:StringRes val noTracks: Int,
    /** The message when the source could not be read. */
    @param:StringRes val scanFailed: Int,
    /** An example address for the sign-in form, or null for a source without one. */
    @param:StringRes val addressHint: Int? = null,
    /** The message when an address answers, but not as this kind of source. */
    @param:StringRes val wrongKind: Int? = null,
)
