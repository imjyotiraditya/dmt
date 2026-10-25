package dev.jyotiraditya.dmt.domain.model

data class DmtStats(val totalMs: Long = 0L, val counts: Map<Long, Int> = emptyMap())
