package dev.jyotiraditya.dmt.library.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Holds the library between launches, so that a scan is only needed when something changed. */
@Database(entities = [TrackEntity::class], version = 1, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao
}
