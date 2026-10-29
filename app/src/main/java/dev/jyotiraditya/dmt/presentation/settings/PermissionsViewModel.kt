package dev.jyotiraditya.dmt.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jyotiraditya.dmt.data.repository.LibraryRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/** Keeps the library in step with what the user allows it to read. */
@HiltViewModel
class PermissionsViewModel @Inject constructor(private val libraryRepository: LibraryRepository) :
    ViewModel(),
    PermissionListener {

    val hasPermission: StateFlow<Boolean> = libraryRepository.hasPermission

    override fun onMusicAccess(granted: Boolean) = libraryRepository.onPermission(granted)

    override fun onFilesAccess() = libraryRepository.rescan()
}

/** What a screen that grants permissions reports back. */
interface PermissionListener {
    /** The music permission was granted or taken away. */
    fun onMusicAccess(granted: Boolean)

    /** Access to all files was granted, so files the media store skips can be read. */
    fun onFilesAccess()
}
