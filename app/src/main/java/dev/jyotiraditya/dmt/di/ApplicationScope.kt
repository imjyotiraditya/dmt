package dev.jyotiraditya.dmt.di

import javax.inject.Qualifier

/** The coroutine scope that lives as long as the app. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
