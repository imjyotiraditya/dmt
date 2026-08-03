package dev.jyotiraditya.dmt.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.jyotiraditya.dmt.data.repository.PreferencesRepository
import dev.jyotiraditya.dmt.library.MusicLibrary
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun musicLibrary(
        @ApplicationContext context: Context,
        settingsRepository: PreferencesRepository,
    ): MusicLibrary = MusicLibrary(
        context = context,
        storedGeneration = object : MusicLibrary.GenerationStore {
            override suspend fun get(): Long = settingsRepository.libraryGeneration()

            override suspend fun set(generation: Long) =
                settingsRepository.setLibraryGeneration(generation)
        },
    )
}
