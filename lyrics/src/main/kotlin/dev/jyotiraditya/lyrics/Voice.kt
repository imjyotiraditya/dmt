package dev.jyotiraditya.lyrics

/**
 * Which side a line renders on in a duet layout. Not who's singing.
 *
 * This only ever flips between [PRIMARY] and [SECONDARY] whenever the singer
 * changes, no matter how many voices the source actually tags (`v1`, `v2`,
 * `v3`, and so on). If you need to know who's actually singing, that's
 * [LyricLine.singer].
 */
enum class Voice { PRIMARY, SECONDARY, GROUP }
