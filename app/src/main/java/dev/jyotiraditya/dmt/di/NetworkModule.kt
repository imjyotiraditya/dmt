package dev.jyotiraditya.dmt.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Ignores unknown fields, so new server fields do not break parsing. */
    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun httpClient(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
        }
    }
}
