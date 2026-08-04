package dev.jyotiraditya.lyrics.ttml

/*
 * The words a document uses to say what a span or a singer is, which every part of reading
 * one has to agree on.
 */

/** The `ttm:role` of a span sung behind the line rather than as part of it. */
internal const val ROLE_BACKGROUND = "x-bg"

/** The `ttm:role` of a span holding the line in another language. */
internal const val ROLE_TRANSLATION = "x-translation"

/** The `ttm:role` of a span holding the line written in another script. */
internal const val ROLE_ROMANIZATION = "x-roman"

/** The `ttm:agent` type of a singer that stands for many. */
internal const val AGENT_TYPE_GROUP = "group"
