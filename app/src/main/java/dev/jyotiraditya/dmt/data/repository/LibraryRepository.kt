package dev.jyotiraditya.dmt.data.repository

import android.util.Log
import dev.jyotiraditya.dmt.core.common.PermissionChecker
import dev.jyotiraditya.dmt.di.ApplicationScope
import dev.jyotiraditya.dmt.domain.model.LibraryScan
import dev.jyotiraditya.dmt.domain.model.LibrarySnapshot
import dev.jyotiraditya.dmt.domain.usecase.ScanLibraryUseCase
import dev.jyotiraditya.dmt.util.audioPermission
import dev.jyotiraditya.dmt.util.suspendRunCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "LibraryRepository"

/** How long the device's files stay unchanged before they are scanned again. */
private val LIBRARY_SETTLE = 500.milliseconds

/** The library of the source in use, scanned again whenever it changes. */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Singleton
class LibraryRepository
@Inject
constructor(
    private val scanLibrary: ScanLibraryUseCase,
    private val preferences: PreferencesRepository,
    permissionChecker: PermissionChecker,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val library: StateFlow<LibrarySnapshot>
        field = MutableStateFlow(LibrarySnapshot())

    val scanning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /** The last scan that finished, or null before any. */
    val lastScan: StateFlow<LibraryScan?>
        field = MutableStateFlow<LibraryScan?>(null)

    /** Why the last scan failed, or null once one has finished. */
    val failure: StateFlow<Throwable?>
        field = MutableStateFlow<Throwable?>(null)

    /** Whether the app may read the music on this device. */
    val hasPermission: StateFlow<Boolean>
        field = MutableStateFlow(permissionChecker.isGranted(audioPermission))

    private var scanJob: Job? = null

    init {
        scope.launch {
            val stored = preferences.lastScan()
            lastScan.update { it ?: stored }
        }
        scope.launch {
            preferences.settings
                .map { it.server to it.hiddenFolders }
                .distinctUntilChanged()
                .drop(1)
                .collect { scan() }
        }
        scope.launch {
            preferences.settings
                .map { it.sourceMode }
                .distinctUntilChanged()
                .flatMapLatest { scanLibrary.changes() }
                .debounce(LIBRARY_SETTLE)
                .filter { hasPermission.value }
                .collect { scan() }
        }
        if (hasPermission.value) scan()
    }

    /** Records whether the music permission was granted, and scans once it is. */
    fun onPermission(granted: Boolean) {
        hasPermission.value = granted
        if (granted) scan()
    }

    /** Reads the whole library again, counting what was added and removed. */
    fun rescan() = scan(refresh = true)

    private fun scan(refresh: Boolean = false) {
        scanJob?.cancel()
        scanJob = scope.launch {
            scanning.value = true
            val before = library.value
            suspendRunCatching { scanLibrary(refresh).collect { library.value = it } }
                .onSuccess {
                    failure.value = null
                    val scan = LibraryScan.between(before.tracks, library.value.tracks, refresh)
                    lastScan.value = scan
                    preferences.saveLastScan(scan)
                }
                .onFailure {
                    Log.w(TAG, "Library scan failed", it)
                    // Empties the library only when this scan read none of it.
                    if (library.value === before) library.value = LibrarySnapshot()
                    failure.value = it
                }
            scanning.value = false
        }
    }
}
