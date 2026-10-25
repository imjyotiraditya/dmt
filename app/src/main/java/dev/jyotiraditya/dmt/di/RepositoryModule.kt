package dev.jyotiraditya.dmt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jyotiraditya.dmt.data.repository.ArtworkRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.JellyfinMediaRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.MediaRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.SubsonicMediaRepositoryImpl
import dev.jyotiraditya.dmt.domain.repository.ArtworkRepository
import dev.jyotiraditya.dmt.domain.repository.MediaRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Local @Binds
    abstract fun mediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds abstract fun artworkRepository(impl: ArtworkRepositoryImpl): ArtworkRepository

    @JellyfinSource
    @Binds
    abstract fun jellyfinMediaRepository(impl: JellyfinMediaRepositoryImpl): MediaRepository

    @SubsonicSource
    @Binds
    abstract fun subsonicMediaRepository(impl: SubsonicMediaRepositoryImpl): MediaRepository
}
