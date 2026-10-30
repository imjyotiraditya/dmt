package dev.jyotiraditya.dmt.core.source

import dagger.MapKey
import dev.jyotiraditya.dmt.core.model.SourceMode

/** Binds a [MusicSource] into the map of sources under its [value]. */
@MapKey
@Retention(AnnotationRetention.BINARY)
annotation class SourceKey(val value: SourceMode)
