package dev.jyotiraditya.dmt.core.source

private const val FNV_OFFSET = -0x340d631b7bdddcdbL
private const val FNV_PRIME = 0x100000001b3L

/** A stable 64-bit FNV-1a hash of this id, used as a track id. */
fun String.toTrackId(): Long = encodeToByteArray().fold(FNV_OFFSET) { hash, byte ->
    (hash xor (byte.toLong() and 0xff)) * FNV_PRIME
}
