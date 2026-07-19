package dev.jyotiraditya.metadata

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels

internal fun ByteArray.startsWith(prefix: String): Boolean =
    prefix.withIndex().all { (index, char) -> this[index] == char.code.toByte() }

internal fun syncsafe(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)

internal fun beInt(bytes: ByteArray, offset: Int): Int =
    ByteBuffer.wrap(bytes, offset, 4).int

internal fun leInt(bytes: ByteArray, offset: Int): Int =
    ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

internal fun syncsafeBytes(value: Int): ByteArray =
    byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )

internal fun beIntBytes(value: Int): ByteArray =
    ByteBuffer.allocate(4).putInt(value).array()

internal fun leIntBytes(value: Int): ByteArray =
    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

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
            Channels.newInputStream(channel).copyTo(out)
        }

        seek(metaEnd - newHead.size)
        write(newHead)

        tail.inputStream().use { input ->
            input.copyTo(Channels.newOutputStream(channel))
        }
        setLength(metaEnd + tail.length())
    } finally {
        tail.delete()
    }
}
