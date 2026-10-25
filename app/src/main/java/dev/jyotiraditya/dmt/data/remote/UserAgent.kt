package dev.jyotiraditya.dmt.data.remote

import dev.jyotiraditya.dmt.BuildConfig

private const val PROJECT_URL = "https://github.com/imjyotiraditya/dmt"

/** How the app names itself to public lyrics and artwork services. */
const val DMT_USER_AGENT = "dmt v${BuildConfig.VERSION_NAME} ($PROJECT_URL)"
