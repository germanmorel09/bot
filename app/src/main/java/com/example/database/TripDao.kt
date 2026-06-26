package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.TripRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trip_records ORDER BY timestamp DESC")
    fun getAllTrips(): Flow<List<TripRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: TripRecord)

    @Query("DELETE FROM trip_records")
    suspend fun clearAllTrips()

    @Query("SELECT COUNT(*) FROM trip_records")
    suspend fun getCount(): Int
}
