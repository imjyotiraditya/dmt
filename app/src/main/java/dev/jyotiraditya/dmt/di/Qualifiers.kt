package dev.jyotiraditya.dmt.di

import javax.inject.Qualifier

/** The coroutine scope that lives as long as the app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Local

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class JellyfinSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SubsonicSource
