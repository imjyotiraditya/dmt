package dev.jyotiraditya.dmt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jyotiraditya.dmt.data.repository.JellyfinMediaRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.LyricsRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.MediaRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.PreferencesRepositoryImpl
import dev.jyotiraditya.dmt.data.repository.TrackMediaRepositoryImpl
import dev.jyotiraditya.dmt.domain.repository.LyricsRepository
import dev.jyotiraditya.dmt.domain.repository.MediaRepository
import dev.jyotiraditya.dmt.domain.repository.SettingsRepository
import dev.jyotiraditya.dmt.domain.repository.StatsRepository
import dev.jyotiraditya.dmt.domain.repository.TrackMediaRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Local
    @Binds
    abstract fun mediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @JellyfinSource
    @Binds
    abstract fun jellyfinMediaRepository(impl: JellyfinMediaRepositoryImpl): MediaRepository

    @Binds
    abstract fun lyricsRepository(impl: LyricsRepositoryImpl): LyricsRepository

    @Binds
    abstract fun settingsRepository(impl: PreferencesRepositoryImpl): SettingsRepository

    @Binds
    abstract fun statsRepository(impl: PreferencesRepositoryImpl): StatsRepository

    @Binds
    abstract fun trackMediaRepository(impl: TrackMediaRepositoryImpl): TrackMediaRepository
}
