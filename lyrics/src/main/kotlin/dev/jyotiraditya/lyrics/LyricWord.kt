package dev.jyotiraditya.lyrics

/**
 * Timing for one word (or syllable, for CJK) inside the text it belongs to.
 *
 * @property start inclusive character offset into the owning text.
 * @property end exclusive character offset into the owning text.
 * @property background true for backing vocals or adlibs: LRC `[bg: ...]`
 *   lines, or TTML spans with `ttm:role="x-bg"`.
 */
data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val start: Int,
    val end: Int,
    val background: Boolean,
)
