package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_records")
data class TripRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val price: Double,
    val pickupDistance: Double,
    val tripDistance: Double,
    val pricePerKm: Double,
    val passengerTrips: Int,
    val passengerRating: Double,
    val description: String?,
    val isValid: Boolean,
    val rejectionReason: String?
)
