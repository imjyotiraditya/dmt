package dev.jyotiraditya.dmt.data

data class DmtStats(
    val totalMs: Long = 0L,
    val counts: Map<Long, Int> = emptyMap(),
)

fun String.toCounts(): Map<Long, Int> = split(';')
    .mapNotNull { entry ->
        val parts = entry.split(':')
        val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
        val count = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
        id to count
    }
    .toMap()

fun Map<Long, Int>.encodeCounts(): String =
    entries.joinToString(";") { "${it.key}:${it.value}" }
