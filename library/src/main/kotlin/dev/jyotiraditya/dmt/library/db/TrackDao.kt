package dev.jyotiraditya.dmt.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface TrackDao {

    @Query("SELECT * FROM tracks")
    suspend fun all(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE path IN (:paths)")
    suspend fun delete(paths: List<String>)

    /** Writes [changed] rows and removes the rows at [removed] paths in one transaction. */
    @Transaction
    suspend fun apply(changed: List<TrackEntity>, removed: List<String>) {
        // SQLite limits bound parameters, so large removals go in batches.
        removed.chunked(MAX_BOUND_PARAMETERS).forEach { delete(it) }
        if (changed.isNotEmpty()) insert(changed)
    }

    private companion object {
        const val MAX_BOUND_PARAMETERS = 900
    }
}
