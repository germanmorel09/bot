package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.model.AppConfig
import com.example.model.TripRecord
import com.example.repository.TripRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

class DriverAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var repository: TripRepository

    // Deduplication tracking: key is "price_pickup_trip", value is timestamp
    private val processedTrips = mutableMapOf<String, Long>()

    override fun onCreate() {
        super.onCreate()
        repository = TripRepository(this)
        Log.d("DriverAccessibility", "Accessibility Service onCreate")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        DriverServiceBus.isAccessibilityConnected.value = true
        Log.d("DriverAccessibility", "Accessibility Service Connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        DriverServiceBus.isAccessibilityConnected.value = false
        job.cancel()
        Log.d("DriverAccessibility", "Accessibility Service onDestroy")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Only run analysis if the user has explicitly toggled on the Assistant in the UI
        if (!DriverServiceBus.isAssistantActive.value) {
            return
        }

        val rootNode = rootInActiveWindow ?: return
        val texts = mutableListOf<String>()
        traverseNode(rootNode, texts)
        rootNode.recycle()

        if (texts.isNotEmpty()) {
            parseAndEvaluate(texts)
        }
    }

    override fun onInterrupt() {
        Log.d("DriverAccessibility", "Accessibility Service Interrupted")
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, texts: MutableList<String>) {
        if (node == null) return
        val text = node.text
        if (text != null && text.isNotBlank()) {
            texts.add(text.toString())
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, texts)
            child?.recycle()
        }
    }

    private fun parseAndEvaluate(texts: List<String>) {
        // Check if this screen contains relevant trip-related keywords before running complex regexes
        val joined = texts.joinToString(" ").lowercase(Locale.ROOT)
        val hasPrice = joined.contains("rd$") || joined.contains("$")
        val hasDistance = joined.contains("km")

        if (!hasPrice || !hasDistance) {
            return
        }

        // --- Parsing variables ---
        var price = 0.0
        var pickupDistance = -1.0
        var tripDistance = -1.0
        var passengerRating = 5.0
        var passengerTrips = 0
        var description: String? = null

        // 1. Parse Price: e.g. "RD$ 350", "RD$350", "$350"
        val priceRegex = """(?:RD)?\$\s*([\d,.]+)""".toRegex(RegexOption.IGNORE_CASE)
        for (text in texts) {
            val match = priceRegex.find(text)
            if (match != null) {
                val valueStr = match.groupValues[1].replace(",", "")
                price = valueStr.toDoubleOrNull() ?: 0.0
                break
            }
        }

        // 2. Parse Distances
        // In modern ride apps:
        // - "Recoger a: 1.2 km"
        // - "Distancia de viaje: 8.0 km"
        // Let's analyze line by line
        val pickupRegex = """(?:recoger|recogida|pickup|retirar|recoge)(?:\s+a)?:\s*([\d,.]+)\s*km""".toRegex(RegexOption.IGNORE_CASE)
        val tripRegex = """(?:viaje|destino|recorrido|distancia de viaje|viaje de):\s*([\d,.]+)\s*km""".toRegex(RegexOption.IGNORE_CASE)
        val generalKmRegex = """([\d,.]+)\s*km""".toRegex(RegexOption.IGNORE_CASE)

        for (text in texts) {
            pickupRegex.find(text)?.let {
                pickupDistance = it.groupValues[1].replace(",", ".").toDoubleOrNull() ?: -1.0
            }
            tripRegex.find(text)?.let {
                tripDistance = it.groupValues[1].replace(",", ".").toDoubleOrNull() ?: -1.0
            }
        }

        // Fallback: If named markers weren't found, find all numeric km patterns in order
        if (pickupDistance < 0 || tripDistance < 0) {
            val foundDistances = mutableListOf<Double>()
            for (text in texts) {
                // If it's a specific label, don't double parse
                if (pickupRegex.containsMatchIn(text) || tripRegex.containsMatchIn(text)) {
                    continue
                }
                generalKmRegex.findAll(text).forEach { match ->
                    match.groupValues[1].replace(",", ".").toDoubleOrNull()?.let {
                        foundDistances.add(it)
                    }
                }
            }
            if (foundDistances.isNotEmpty()) {
                if (pickupDistance < 0) pickupDistance = foundDistances[0]
                if (tripDistance < 0 && foundDistances.size > 1) {
                    tripDistance = foundDistances[1]
                } else if (tripDistance < 0) {
                    // If only one distance is found, let's treat it as the trip distance (safety fallback)
                    tripDistance = pickupDistance
                    pickupDistance = 1.0 // Assume a standard 1km pick up as default
                }
            }
        }

        // If distances remain unresolved, we can't evaluate
        if (price <= 0 || pickupDistance < 0 || tripDistance < 0) {
            return
        }

        // 3. Parse Rating (stars like 4.8 or ★ 4.8)
        val ratingRegex = """★\s*([\d.]+)|([\d.]+)\s*★|\b([345]\.\d)\b""".toRegex()
        for (text in texts) {
            val match = ratingRegex.find(text)
            if (match != null) {
                val ratingStr = match.groupValues[1].ifBlank { match.groupValues[2].ifBlank { match.groupValues[3] } }
                passengerRating = ratingStr.toDoubleOrNull() ?: 5.0
                break
            }
        }

        // 4. Parse Passenger Trips Count: "145 viajes" or "145 trips"
        val tripsRegex = """(\d+)\s*(?:viajes|viaje|trips|trip)""".toRegex(RegexOption.IGNORE_CASE)
        for (text in texts) {
            val match = tripsRegex.find(text)
            if (match != null) {
                passengerTrips = match.groupValues[1].toIntOrNull() ?: 0
                break
            }
        }

        // 5. Parse Description (Notes, tags, or extra details)
        val descRegex = """(?:descripción|descripcion|nota|detalles):\s*(.+)""".toRegex(RegexOption.IGNORE_CASE)
        for (text in texts) {
            val match = descRegex.find(text)
            if (match != null) {
                description = match.groupValues[1].trim()
                break
            }
        }
        // Fallback for simulation card descriptions
        if (description == null) {
            val descIndex = texts.indexOfFirst { it.lowercase().startsWith("descripción:") || it.lowercase().startsWith("descripcion:") || it.lowercase().startsWith("nota:") }
            if (descIndex != -1 && descIndex + 1 < texts.size) {
                description = texts[descIndex + 1].trim()
            }
        }

        // --- Deduplication Check ---
        val tripKey = "${price.toInt()}_${String.format("%.1f", pickupDistance)}_${String.format("%.1f", tripDistance)}"
        val now = System.currentTimeMillis()
        val lastProcessedTime = processedTrips[tripKey]
        if (lastProcessedTime != null && (now - lastProcessedTime) < 10000) {
            // Already processed this trip in the last 10 seconds. Skip.
            return
        }
        processedTrips[tripKey] = now

        // Clean up old entries from deduplication map (> 1 min)
        processedTrips.entries.removeIf { now - it.value > 60000 }

        // --- Run Rules Engine ---
        val config = repository.loadConfig()
        val totalDistance = pickupDistance + tripDistance
        val pricePerKm = if (totalDistance > 0) price / totalDistance else 0.0

        var isValid = true
        var rejectionReason: String? = null

        when {
            pickupDistance > config.maxPickupDistance -> {
                isValid = false
                rejectionReason = "Distancia de recogida excesiva (${String.format("%.1f", pickupDistance)} km)"
            }
            pricePerKm < config.minPricePerKm -> {
                isValid = false
                rejectionReason = "Precio/km muy bajo (${String.format("%.1f", pricePerKm)} RD\$/km)"
            }
            tripDistance > config.maxTripDistance -> {
                isValid = false
                rejectionReason = "Viaje demasiado largo (${String.format("%.1f", tripDistance)} km)"
            }
            config.rejectWithDescription && !description.isNullOrBlank() -> {
                isValid = false
                rejectionReason = "Tiene descripción del viaje"
            }
            passengerTrips < config.minPassengerTrips -> {
                isValid = false
                rejectionReason = "Pasajero con muy pocos viajes ($passengerTrips viajes)"
            }
            passengerRating < config.minPassengerRating -> {
                isValid = false
                rejectionReason = "Baja calificación del pasajero (★ $passengerRating)"
            }
        }

        val record = TripRecord(
            price = price,
            pickupDistance = pickupDistance,
            tripDistance = tripDistance,
            pricePerKm = pricePerKm,
            passengerTrips = passengerTrips,
            passengerRating = passengerRating,
            description = description,
            isValid = isValid,
            rejectionReason = rejectionReason
        )

        // --- Save and Notify ---
        serviceScope.launch {
            repository.insertTrip(record)

            // Update global service bus
            DriverServiceBus.lastTrip.value = record
            if (isValid) {
                DriverServiceBus.statusMessage.value = "¡Viaje VÁLIDO detectado!"
            } else {
                DriverServiceBus.statusMessage.value = "Viaje rechazado: $rejectionReason"
            }

            // Trigger alerts if trip is valid
            if (isValid) {
                if (config.playSound) {
                    DriverServiceBus.emitEvent(ServiceEvent.PlaySound)
                    playSoundAlert()
                }
                if (config.enableVibration) {
                    DriverServiceBus.emitEvent(ServiceEvent.TriggerVibrate)
                    triggerVibrationAlert()
                }
            }

            DriverServiceBus.emitEvent(ServiceEvent.TripProcessed(record))
        }
    }

    private fun playSoundAlert() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(applicationContext, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            Log.e("DriverAccessibility", "Error playing notification sound", e)
        }
    }

    private fun triggerVibrationAlert() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        } catch (e: Exception) {
            Log.e("DriverAccessibility", "Error triggering vibration", e)
        }
    }
}
