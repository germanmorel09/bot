package com.example.service

import com.example.model.TripRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

object DriverServiceBus {
    // Connection status of the Accessibility Service
    val isAccessibilityConnected = MutableStateFlow(false)

    // Running state of the assistant (turned ON/OFF by the user in the main screen)
    val isAssistantActive = MutableStateFlow(false)

    // Overlay visibility state
    val isOverlayActive = MutableStateFlow(false)

    // Current status message to display (e.g. "Buscando viajes...", "Esperando...")
    val statusMessage = MutableStateFlow("Buscando viajes...")

    // Details of the last parsed trip
    val lastTrip = MutableStateFlow<TripRecord?>(null)

    // Shared Flow to dispatch reactive side-effects like play sound or vibrate
    private val _events = MutableSharedFlow<ServiceEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    fun emitEvent(event: ServiceEvent) {
        _events.tryEmit(event)
    }
}

sealed interface ServiceEvent {
    data class TripProcessed(val trip: TripRecord) : ServiceEvent
    object PlaySound : ServiceEvent
    object TriggerVibrate : ServiceEvent
}
