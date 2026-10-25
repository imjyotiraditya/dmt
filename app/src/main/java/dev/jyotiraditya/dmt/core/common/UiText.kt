package dev.jyotiraditya.dmt.core.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** A string resource and its arguments, so state can carry text without a `Context`. */
data class UiText(@param:StringRes val id: Int, val args: List<Any>) {
    constructor(@StringRes id: Int, vararg args: Any) : this(id, args.toList())
}

@Composable
fun UiText.asString(): String {
    val resolved = args.map { if (it is UiText) it.asString() else it }
    return stringResource(id, *resolved.toTypedArray())
}
