package dev.jyotiraditya.dmt.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

/** Reads and writes the stored library. */
@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks")
    suspend fun all(): List<TrackEntity>

    @Query("DELETE FROM tracks")
    suspend fun clear()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracks: List<TrackEntity>)

    /** Replaces the stored library, so that tracks whose files are gone do not linger. */
    @Transaction
    suspend fun replaceAll(tracks: List<TrackEntity>) {
        clear()
        insert(tracks)
    }
}
