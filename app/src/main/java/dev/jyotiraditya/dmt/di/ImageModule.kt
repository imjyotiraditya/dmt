package dev.jyotiraditya.dmt.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val MEMORY_CACHE_SHARE_OF_APP = 0.25
private const val DISK_CACHE_DIR = "cover_art"
private const val DISK_CACHE_MAX_BYTES = 50L * 1024 * 1024

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun imageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .memoryCache { memoryCache(context) }
            .diskCache { diskCache(context) }
            .build()

    private fun memoryCache(context: Context): MemoryCache =
        MemoryCache.Builder()
            .maxSizePercent(context, MEMORY_CACHE_SHARE_OF_APP)
            .build()

    private fun diskCache(context: Context): DiskCache =
        DiskCache.Builder()
            .directory(context.cacheDir.resolve(DISK_CACHE_DIR))
            .maxSizeBytes(DISK_CACHE_MAX_BYTES)
            .build()
}
