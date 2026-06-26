package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.AppConfig
import com.example.model.TripRecord
import com.example.repository.TripRepository
import com.example.service.DriverAccessibilityService
import com.example.service.DriverOverlayService
import com.example.service.DriverServiceBus
import com.example.service.ServiceEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TripRepository(application)

    // Observable settings state
    private val _configState = MutableStateFlow(AppConfig())
    val configState: StateFlow<AppConfig> = _configState.asStateFlow()

    // Service Status states from the Shared Bus
    val isAssistantActive = DriverServiceBus.isAssistantActive
    val isOverlayActive = DriverServiceBus.isOverlayActive
    val isAccessibilityConnected = DriverServiceBus.isAccessibilityConnected

    // List of trip records from Room
    val allTrips: StateFlow<List<TripRecord>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load initial user preferences
        _configState.value = repository.loadConfig()
    }

    // --- Configuration actions ---
    fun updateConfig(newConfig: AppConfig) {
        _configState.value = newConfig
        repository.saveConfig(newConfig)

        // If overlay is disabled in config, ensure we stop the service
        if (!newConfig.showOverlay && DriverServiceBus.isOverlayActive.value) {
            stopOverlayService()
        } else if (newConfig.showOverlay && !DriverServiceBus.isOverlayActive.value && isAssistantActive.value) {
            startOverlayService()
        }
    }

    // --- History actions ---
    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            DriverServiceBus.lastTrip.value = null
            DriverServiceBus.statusMessage.value = "Buscando viajes..."
        }
    }

    // --- Assistant state controllers ---
    fun toggleAssistant(context: Context) {
        val currentActive = isAssistantActive.value
        if (!currentActive) {
            // Activating Assistant:
            // First verify Accessibility Service is turned on in System Settings
            if (!isAccessibilityServiceEnabled(context)) {
                // Return, the UI will capture this or we trigger setting intent
                return
            }

            // Verify and request Draw Overlays if turned on in config
            if (configState.value.showOverlay && !Settings.canDrawOverlays(context)) {
                return
            }

            isAssistantActive.value = true
            DriverServiceBus.statusMessage.value = "Buscando viajes..."

            if (configState.value.showOverlay) {
                startOverlayService()
            }
        } else {
            // Deactivating Assistant:
            isAssistantActive.value = false
            stopOverlayService()
        }
    }

    fun startOverlayService() {
        val context = getApplication<Application>()
        if (Settings.canDrawOverlays(context)) {
            val intent = Intent(context, DriverOverlayService::class.classjava() ?: DriverOverlayService::class.java)
            context.startService(intent)
        }
    }

    fun stopOverlayService() {
        val context = getApplication<Application>()
        val intent = Intent(context, DriverOverlayService::class.classjava() ?: DriverOverlayService::class.java)
        context.stopService(intent)
    }

    // Checking if our custom Accessibility Service is active in system settings
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = android.content.ComponentName(context, DriverAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = android.content.ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    // Helper extension for class.java safety in Kotlin DSL
    private fun <T : Any> kotlin.reflect.KClass<T>.classjava(): Class<T>? {
        return java
    }

    // --- Simulation Mechanism ---
    // Simulates receiving a ride-hailing request directly from code for safe execution testing.
    fun simulateRide(
        price: Double,
        pickupDist: Double,
        tripDist: Double,
        rating: Double,
        trips: Int,
        desc: String?
    ) {
        val config = configState.value
        val totalDistance = pickupDist + tripDist
        val pricePerKm = if (totalDistance > 0) price / totalDistance else 0.0

        var isValid = true
        var rejectionReason: String? = null

        when {
            pickupDist > config.maxPickupDistance -> {
                isValid = false
                rejectionReason = "Distancia de recogida excesiva (${String.format("%.1f", pickupDist)} km)"
            }
            pricePerKm < config.minPricePerKm -> {
                isValid = false
                rejectionReason = "Precio/km muy bajo (${String.format("%.1f", pricePerKm)} RD\$/km)"
            }
            tripDist > config.maxTripDistance -> {
                isValid = false
                rejectionReason = "Viaje demasiado largo (${String.format("%.1f", tripDist)} km)"
            }
            config.rejectWithDescription && !desc.isNullOrBlank() -> {
                isValid = false
                rejectionReason = "Tiene descripción del viaje"
            }
            trips < config.minPassengerTrips -> {
                isValid = false
                rejectionReason = "Pasajero con muy pocos viajes ($trips viajes)"
            }
            rating < config.minPassengerRating -> {
                isValid = false
                rejectionReason = "Baja calificación del pasajero (★ $rating)"
            }
        }

        val record = TripRecord(
            price = price,
            pickupDistance = pickupDist,
            tripDistance = tripDist,
            pricePerKm = pricePerKm,
            passengerTrips = trips,
            passengerRating = rating,
            description = desc,
            isValid = isValid,
            rejectionReason = rejectionReason
        )

        viewModelScope.launch {
            repository.insertTrip(record)
            DriverServiceBus.lastTrip.value = record
            if (isValid) {
                DriverServiceBus.statusMessage.value = "¡Viaje VÁLIDO detectado (Simulado)!"
            } else {
                DriverServiceBus.statusMessage.value = "Viaje rechazado (Simulado): $rejectionReason"
            }

            // Dispatch reactive events for sound/vibrate
            if (isValid) {
                if (config.playSound) {
                    DriverServiceBus.emitEvent(ServiceEvent.PlaySound)
                }
                if (config.enableVibration) {
                    DriverServiceBus.emitEvent(ServiceEvent.TriggerVibrate)
                }
            }
            DriverServiceBus.emitEvent(ServiceEvent.TripProcessed(record))
        }
    }
}
