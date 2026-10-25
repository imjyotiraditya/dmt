package dev.jyotiraditya.dmt.presentation.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.jyotiraditya.dmt.R
import dev.jyotiraditya.dmt.data.remote.jellyfin.JellyfinError
import dev.jyotiraditya.dmt.data.remote.jellyfin.toJellyfinError
import dev.jyotiraditya.dmt.data.remote.subsonic.SubsonicError
import dev.jyotiraditya.dmt.data.remote.subsonic.toSubsonicError
import dev.jyotiraditya.dmt.domain.model.SourceMode
import dev.jyotiraditya.dmt.ui.icons.DmtIcons

@get:StringRes
internal val SourceMode.title: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> R.string.source_kind_local
            SourceMode.JELLYFIN -> R.string.source_kind_jellyfin
            SourceMode.SUBSONIC -> R.string.source_kind_subsonic
        }

@get:DrawableRes
internal val SourceMode.icon: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> DmtIcons.Device
            SourceMode.JELLYFIN -> DmtIcons.Jellyfin
            SourceMode.SUBSONIC -> DmtIcons.Navidrome
        }

@get:StringRes
internal val SourceMode.setupDetail: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> R.string.setup_source_local_detail
            SourceMode.JELLYFIN -> R.string.setup_source_jellyfin_detail
            SourceMode.SUBSONIC -> R.string.setup_source_subsonic_detail
        }

@get:StringRes
internal val SourceMode.noTracksMessage: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> R.string.library_empty_local
            SourceMode.JELLYFIN -> R.string.library_empty_jellyfin
            SourceMode.SUBSONIC -> R.string.library_empty_subsonic
        }

@get:StringRes
internal val SourceMode.scanFailedMessage: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> R.string.library_scan_failed_local
            SourceMode.JELLYFIN -> R.string.library_scan_failed_jellyfin
            SourceMode.SUBSONIC -> R.string.library_scan_failed_subsonic
        }

/** A one-line description of this kind of source. */
@get:StringRes
internal val SourceMode.line: Int
    get() =
        when (this) {
            SourceMode.LOCAL -> R.string.setup_source_local_detail
            SourceMode.JELLYFIN -> R.string.source_kind_jellyfin_summary
            SourceMode.SUBSONIC -> R.string.source_kind_subsonic_summary
        }

/** The message for a sign-in to this kind of server that failed with [failure]. */
@StringRes
internal fun SourceMode.loginMessage(failure: Throwable): Int = when (this) {
    SourceMode.JELLYFIN -> failure.toJellyfinError().loginMessage
    SourceMode.SUBSONIC -> failure.toSubsonicError().loginMessage
    SourceMode.LOCAL -> R.string.source_form_error_unreachable
}

@get:StringRes
private val JellyfinError.loginMessage: Int
    get() =
        when (this) {
            JellyfinError.UNREACHABLE -> R.string.source_form_error_unreachable
            JellyfinError.BAD_CREDENTIALS -> R.string.source_form_error_rejected
            JellyfinError.NOT_JELLYFIN -> R.string.source_form_error_not_jellyfin
        }

@get:StringRes
private val SubsonicError.loginMessage: Int
    get() =
        when (this) {
            SubsonicError.UNREACHABLE -> R.string.source_form_error_unreachable
            SubsonicError.BAD_CREDENTIALS -> R.string.source_form_error_rejected
            SubsonicError.TOKEN_UNSUPPORTED -> R.string.source_form_error_token_unsupported
            SubsonicError.NOT_SUBSONIC -> R.string.source_form_error_not_subsonic
        }

@get:StringRes
internal val SourceMode.urlHint: Int
    get() =
        when (this) {
            SourceMode.SUBSONIC -> R.string.source_form_address_hint_subsonic

            SourceMode.LOCAL,
            SourceMode.JELLYFIN,
            -> R.string.source_form_address_hint_jellyfin
        }
