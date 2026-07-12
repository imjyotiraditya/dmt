package dev.jyotiraditya.metadata

import java.io.File
import java.io.RandomAccessFile

internal fun ByteArray.startsWith(prefix: String): Boolean =
    prefix.withIndex().all { (index, char) -> this[index] == char.code.toByte() }

internal fun syncsafe(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

internal fun beInt(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)

internal fun leInt(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

internal fun syncsafeBytes(value: Int): ByteArray =
    byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

internal fun beIntBytes(value: Int): ByteArray =
    byteArrayOf(
        ((value ushr 24) and 0xFF).toByte(),
        ((value ushr 16) and 0xFF).toByte(),
        ((value ushr 8) and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )

internal fun leIntBytes(value: Int): ByteArray =
    byteArrayOf(
        value.toByte(),
        (value ushr 8).toByte(),
        (value ushr 16).toByte(),
        (value ushr 24).toByte(),
    )

internal fun RandomAccessFile.rewriteWithTail(
    metaEnd: Long,
    newHead: ByteArray,
    oldAudioStart: Long,
    tempDir: File,
) {
    val tail = File.createTempFile("dmt-audio", ".tmp", tempDir)
    try {
        seek(oldAudioStart)
        tail.outputStream().use { out ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
            }
        }

        seek(metaEnd - newHead.size)
        write(newHead)

        tail.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                write(buffer, 0, n)
            }
        }
        setLength(metaEnd + tail.length())
    } finally {
        tail.delete()
    }
}
