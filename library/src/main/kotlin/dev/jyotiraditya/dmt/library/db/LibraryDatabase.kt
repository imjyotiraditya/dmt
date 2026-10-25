package dev.jyotiraditya.dmt.library.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** The library as of the last scan, kept across launches. */
@Database(entities = [TrackEntity::class], version = 2, exportSchema = false)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun tracks(): TrackDao
}
