package dev.jyotiraditya.dmt.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jyotiraditya.dmt.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

private const val PROJECT_URL = "https://github.com/imjyotiraditya/dmt"

/** How the app names itself in every request. */
private const val USER_AGENT = "dmt v${BuildConfig.VERSION_NAME} ($PROJECT_URL)"

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

    /** A client that reads and writes JSON with [json] and throws on any failed status. */
    @Provides
    @Singleton
    fun httpClient(json: Json): HttpClient = HttpClient(CIO) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 15_000
        }
        install(UserAgent) { agent = USER_AGENT }
        install(ContentNegotiation) { json(json, contentType = ContentType.Any) }
    }
}
