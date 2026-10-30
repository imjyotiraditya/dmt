package dev.jyotiraditya.dmt.data.repository

import dev.jyotiraditya.dmt.core.source.InstallId
import javax.inject.Inject

/** This install's id, kept in the app's settings. */
class StoredInstallId @Inject constructor(private val preferences: PreferencesRepository) :
    InstallId {
    override suspend fun get(): String = preferences.installId()
}
