package dev.jyotiraditya.dmt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dev.jyotiraditya.dmt.core.model.SourceMode
import dev.jyotiraditya.dmt.core.source.InstallId
import dev.jyotiraditya.dmt.core.source.MusicSource
import dev.jyotiraditya.dmt.core.source.SourceKey
import dev.jyotiraditya.dmt.data.repository.ArtworkRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.LocalMusicSource
import dev.jyotiraditya.dmt.data.repository.StoredInstallId
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds abstract fun artworkRepository(impl: ArtworkRepositoryImpl): ArtworkRepository

    @Binds
    @IntoMap
    @SourceKey(SourceMode.LOCAL)
    abstract fun localSource(source: LocalMusicSource): MusicSource

    @Binds abstract fun installId(stored: StoredInstallId): InstallId
}
