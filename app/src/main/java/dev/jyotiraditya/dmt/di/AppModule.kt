package dev.jyotiraditya.dmt.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.jyotiraditya.dmt.util.DefaultDispatcherProvider
import dev.jyotiraditya.dmt.util.DispatcherProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun dispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider
}
