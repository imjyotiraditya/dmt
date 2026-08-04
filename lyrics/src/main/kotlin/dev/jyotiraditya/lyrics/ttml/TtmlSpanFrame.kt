package dev.jyotiraditya.lyrics.ttml

/** A span the reader is inside of, held while the spans nested in it are read. */
internal class SpanFrame(
    val beginMs: Long,
    val endMs: Long,
    val textStart: Int,
    val background: Boolean,
) {
    var hadChild = false
}
