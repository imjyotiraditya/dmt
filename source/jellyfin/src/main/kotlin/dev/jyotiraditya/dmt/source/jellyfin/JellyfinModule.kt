package dev.jyotiraditya.dmt.source.jellyfin

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceKey

@Module
@InstallIn(SingletonComponent::class)
abstract class JellyfinModule {

    @Binds
    @IntoMap
    @SourceKey(SourceMode.JELLYFIN)
    abstract fun jellyfin(source: JellyfinMusicSource): MusicSource
}
