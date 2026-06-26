package com.example.repository

import android.content.Context
import com.example.database.AppDatabase
import com.example.model.AppConfig
import com.example.model.TripRecord
import kotlinx.coroutines.flow.Flow

class TripRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val tripDao = database.tripDao()

    val allTrips: Flow<List<TripRecord>> = tripDao.getAllTrips()

    suspend fun insertTrip(trip: TripRecord) {
        tripDao.insertTrip(trip)
    }

    suspend fun clearHistory() {
        tripDao.clearAllTrips()
    }

    fun loadConfig(): AppConfig {
        return AppConfig.load(context)
    }

    fun saveConfig(config: AppConfig) {
        config.save(context)
    }
}
