package com.example.model

import android.content.Context
import android.content.SharedPreferences

data class AppConfig(
    val maxPickupDistance: Double = 2.0,      // in km
    val minPricePerKm: Double = 40.0,         // in RD$
    val maxTripDistance: Double = 15.0,       // in km
    val rejectWithDescription: Boolean = false,
    val minPassengerTrips: Int = 10,
    val minPassengerRating: Double = 4.5,
    val maxTimeToAccept: Int = 3,             // in seconds
    val playSound: Boolean = true,
    val enableVibration: Boolean = true,
    val showOverlay: Boolean = true,
    val isDarkMode: Boolean = true,
    val language: String = "es"               // "es" or "en"
) {
    companion object {
        private const val PREFS_NAME = "driver_assistant_prefs"

        fun load(context: Context): AppConfig {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return AppConfig(
                maxPickupDistance = prefs.getFloat("maxPickupDistance", 2.0f).toDouble(),
                minPricePerKm = prefs.getFloat("minPricePerKm", 40.0f).toDouble(),
                maxTripDistance = prefs.getFloat("maxTripDistance", 15.0f).toDouble(),
                rejectWithDescription = prefs.getBoolean("rejectWithDescription", false),
                minPassengerTrips = prefs.getInt("minPassengerTrips", 10),
                minPassengerRating = prefs.getFloat("minPassengerRating", 4.5f).toDouble(),
                maxTimeToAccept = prefs.getInt("maxTimeToAccept", 3),
                playSound = prefs.getBoolean("playSound", true),
                enableVibration = prefs.getBoolean("enableVibration", true),
                showOverlay = prefs.getBoolean("showOverlay", true),
                isDarkMode = prefs.getBoolean("isDarkMode", true),
                language = prefs.getString("language", "es") ?: "es"
            )
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("maxPickupDistance", maxPickupDistance.toFloat())
            putFloat("minPricePerKm", minPricePerKm.toFloat())
            putFloat("maxTripDistance", maxTripDistance.toFloat())
            putBoolean("rejectWithDescription", rejectWithDescription)
            putInt("minPassengerTrips", minPassengerTrips)
            putFloat("minPassengerRating", minPassengerRating.toFloat())
            putInt("maxTimeToAccept", maxTimeToAccept)
            putBoolean("playSound", playSound)
            putBoolean("enableVibration", enableVibration)
            putBoolean("showOverlay", showOverlay)
            putBoolean("isDarkMode", isDarkMode)
            putString("language", language)
            apply()
        }
    }
}
