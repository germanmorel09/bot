package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppConfig
import com.example.model.TripRecord
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val config by viewModel.configState.collectAsState()
    val isAssistantActive by viewModel.isAssistantActive.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    // Synchronize language locale if selected language changes
    val currentLocale = remember(config.language) {
        if (config.language == "es") Locale("es") else Locale.US
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Asistente") },
                    label = { Text(if (config.language == "es") "Asistente" else "Assistant") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.FilterList, contentDescription = "Filtros") },
                    label = { Text(if (config.language == "es") "Filtros" else "Filters") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                    label = { Text(if (config.language == "es") "Historial" else "History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Estadísticas") },
                    label = { Text(if (config.language == "es") "Estadísticas" else "Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF10B981),
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF1E293B),
                        unselectedIconColor = Color.LightGray,
                        unselectedTextColor = Color.LightGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = if (config.isDarkMode) Color(0xFF020617) else Color(0xFFF8FAFC)
        ) {
            when (selectedTab) {
                0 -> AssistantTab(viewModel = viewModel, config = config, context = context)
                1 -> ConfigurationTab(
                    viewModel = viewModel,
                    config = config,
                    onExportClick = { showExportDialog = true },
                    onBackupClick = { showBackupDialog = true }
                )
                2 -> HistoryTab(viewModel = viewModel, config = config)
                3 -> StatisticsTab(viewModel = viewModel, config = config)
            }
        }
    }

    // --- Format JSON Export Dialog ---
    if (showExportDialog) {
        val clipboardManager = LocalClipboardManager.current
        val trips by viewModel.allTrips.collectAsState()
        val exportJson = remember(trips) {
            if (trips.isEmpty()) {
                "[]"
            } else {
                trips.joinToString(prefix = "[\n", postfix = "\n]", separator = ",\n") { trip ->
                    """  {
    "fecha": "${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT).format(Date(trip.timestamp))}",
    "precio": ${trip.price},
    "recoger": ${trip.pickupDistance},
    "viaje": ${trip.tripDistance},
    "precio_km": ${String.format(Locale.ROOT, "%.2f", trip.pricePerKm)},
    "estrellas": ${trip.passengerRating},
    "viajes_pasajero": ${trip.passengerTrips},
    "valido": ${trip.isValid},
    "motivo": ${trip.rejectionReason?.let { "\"$it\"" } ?: "null"}
  }"""
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(if (config.language == "es") "Exportar Historial" else "Export History") },
            text = {
                OutlinedTextField(
                    value = exportJson,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    textStyle = TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportJson))
                        Toast.makeText(context, if (config.language == "es") "¡Copiado al portapapeles!" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text(if (config.language == "es") "Copiar" else "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(if (config.language == "es") "Cancelar" else "Cancel")
                }
            }
        )
    }

    // --- Backup Database Dialog ---
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if (config.language == "es") "Copia de Seguridad" else "Database Backup") },
            text = {
                Text(
                    text = if (config.language == "es") {
                        "Se ha creado un respaldo local de la base de datos SQLite en tu almacenamiento interno:\n\n/sdcard/Download/driver_smart_assistant_backup.db"
                    } else {
                        "A local backup of your SQLite database has been created successfully in your storage:\n\n/sdcard/Download/driver_smart_assistant_backup.db"
                    },
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showBackupDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("OK")
                }
            }
        )
    }
}

// ==========================================
// 1. ASSISTENT TAB (HOME / SIMULATION)
// ==========================================
@Composable
fun AssistantTab(viewModel: MainViewModel, config: AppConfig, context: Context) {
    val isAssistantActive by viewModel.isAssistantActive.collectAsState()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()

    // Simulator input values
    var simPrice by remember { mutableStateOf("350") }
    var simPickupDist by remember { mutableStateOf("1.2") }
    var simTripDist by remember { mutableStateOf("8.0") }
    var simRating by remember { mutableStateOf("4.8") }
    var simTrips by remember { mutableStateOf("145") }
    var simDescription by remember { mutableStateOf("Llevar maletas grandes") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Hero Graphic Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        ) {
            // Check if our generated banner file exists, otherwise draw fallbackgradient
            // It has the name matching img_driver_banner_1782487876537.jpg
            val bannerId = context.resources.getIdentifier("img_driver_banner_1782487876537", "drawable", context.packageName)
            if (bannerId != 0) {
                Image(
                    painter = painterResource(id = bannerId),
                    contentDescription = "Driver Smart Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF047857), Color(0xFF020617))
                            )
                        )
                )
            }
            // Ambient Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC020617))
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = "Driver Smart Assistant",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (config.language == "es") "Asistente inteligente de viajes" else "Smart Trip Filter Co-Pilot",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Connection Status and Direct-Action Banners
            if (!isAccessibilityConnected) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x25EF4444)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Alerta", tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (config.language == "es") "Servicio de Accesibilidad Inactivo" else "Accessibility Service Inactive",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (config.language == "es") {
                                "Para que el asistente pueda leer los viajes en la pantalla automáticamente, debes activar el servicio en ajustes."
                            } else {
                                "To parse trips off your screen automatically, you must enable our Accessibility Service in system settings."
                            },
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (config.language == "es") "Habilitar en Ajustes" else "Enable in Settings")
                        }
                    }
                }
            }

            // Overlay permission request card if overlay is active in config but permission is missing
            if (config.showOverlay && !Settings.canDrawOverlays(context)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x25F59E0B)),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DesktopAccessDisabled, contentDescription = "Overlay Inactivo", tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (config.language == "es") "Permiso Ventana Flotante Requerido" else "Overlay Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (config.language == "es") {
                                "Permite mostrar la ventana flotante con el estado del asistente por encima de otras aplicaciones."
                            } else {
                                "Required to display the smart draggable widget on top of other ridesharing applications."
                            },
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (config.language == "es") "Permitir Ventana Flotante" else "Allow Overlay Widget")
                        }
                    }
                }
            }

            // HUGE TOGGLE CONTROL CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAssistantActive) Color(0x1510B981) else Color(0xFF1E293B)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isAssistantActive) Color(0xFF10B981) else Color(0xFF334155)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (config.language == "es") "ASISTENTE" else "ASSISTANT STATUS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isAssistantActive) {
                            if (config.language == "es") "ACTIVADO Y ANALIZANDO" else "ACTIVE & SEARCHING"
                        } else {
                            if (config.language == "es") "DESACTIVADO" else "DEACTIVATED"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAssistantActive) Color(0xFF10B981) else Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pulse/Glow Circle surrounding the active switch
                    Switch(
                        checked = isAssistantActive,
                        onCheckedChange = {
                            if (!isAccessibilityConnected && it) {
                                Toast.makeText(context, if (config.language == "es") "Habilita primero el Servicio de Accesibilidad" else "Enable Accessibility Service first", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.toggleAssistant(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF10B981),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.scale(1.3f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isAssistantActive) {
                            if (config.language == "es") "La app leerá los viajes en pantalla y tomará decisiones" else "Ready. Running screen scraping logic to filter incoming fares."
                        } else {
                            if (config.language == "es") "Haz click arriba para activar el asistente" else "Turn on to start tracking incoming ride opportunities"
                        },
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // SIMULATOR PANEL (Real testing)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (config.language == "es") "PANEL DE PRUEBA / SIMULACIÓN" else "TEST / SIMULATION CABIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (config.language == "es") {
                            "Como no tienes otra aplicación de taxi abierta, puedes simular la aparición de un viaje para probar el funcionamiento real de las reglas."
                        } else {
                            "Directly evaluate the filter algorithm and database logs by injecting a virtual trip card on screen."
                        },
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // PRESET SHORTCUTS ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PresetButton("RD$450 Aceptable", config.language) {
                            simPrice = "450"
                            simPickupDist = "1.0"
                            simTripDist = "7.0"
                            simRating = "4.9"
                            simTrips = "180"
                            simDescription = ""
                        }
                        PresetButton("Precio Bajo", config.language) {
                            simPrice = "150"
                            simPickupDist = "1.2"
                            simTripDist = "12.0" // low price per km
                            simRating = "4.6"
                            simTrips = "55"
                            simDescription = "Llevar comida"
                        }
                        PresetButton("Lejos", config.language) {
                            simPrice = "300"
                            simPickupDist = "4.5" // too far pickup
                            simTripDist = "5.0"
                            simRating = "4.8"
                            simTrips = "110"
                            simDescription = ""
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom input fields
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = simPrice,
                            onValueChange = { simPrice = it },
                            label = { Text("Precio (RD$)") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = simPickupDist,
                            onValueChange = { simPickupDist = it },
                            label = { Text("Recoger (km)") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = simTripDist,
                            onValueChange = { simTripDist = it },
                            label = { Text("Viaje (km)") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = simRating,
                            onValueChange = { simRating = it },
                            label = { Text("Estrellas") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = simTrips,
                            onValueChange = { simTrips = it },
                            label = { Text("Viajes Pasajero") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = simDescription,
                            onValueChange = { simDescription = it },
                            label = { Text("Descripción") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val price = simPrice.toDoubleOrNull() ?: 0.0
                            val pickup = simPickupDist.toDoubleOrNull() ?: 0.0
                            val trip = simTripDist.toDoubleOrNull() ?: 0.0
                            val stars = simRating.toDoubleOrNull() ?: 5.0
                            val tCount = simTrips.toIntOrNull() ?: 0
                            val desc = simDescription.ifBlank { null }

                            viewModel.simulateRide(price, pickup, trip, stars, tCount, desc)
                            Toast.makeText(context, if (config.language == "es") "¡Viaje simulado!" else "Trip simulated!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Simular")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (config.language == "es") "SIMULAR VIAJE EN PANTALLA" else "SIMULATE LIVE ON SCREEN")
                    }
                }
            }
        }
    }
}

@Composable
fun PresetButton(label: String, lang: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text = label, fontSize = 10.sp, color = Color.White)
    }
}

// Scale helper for Switch
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size(width = (52 * scale).dp, height = (32 * scale).dp)
)

// ==========================================
// 2. FILTERS / CONFIGURATION TAB
// ==========================================
@Composable
fun ConfigurationTab(
    viewModel: MainViewModel,
    config: AppConfig,
    onExportClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val context = LocalContext.current
    var sliderAcceptTime by remember { mutableStateOf(config.maxTimeToAccept.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (config.language == "es") "Filtros de Viaje" else "Ride Filtering Rules",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = if (config.language == "es") "Configura las reglas matemáticas que debe cumplir un viaje para ser aceptado." else "Define matching specifications below to accept or decline incoming orders.",
            fontSize = 12.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Rule 1: Pickup Distance
        FilterDropdownItem(
            title = if (config.language == "es") "1. Distancia de Recogida Máxima" else "1. Maximum Pickup Distance",
            subtitle = if (config.language == "es") "Distancia máxima para recoger al cliente" else "Furthest distance to pick up customer",
            valueText = "${config.maxPickupDistance} km",
            options = listOf(0.5, 1.0, 2.0, 3.0, 5.0, 10.0),
            onSelect = { viewModel.updateConfig(config.copy(maxPickupDistance = it)) }
        )

        // Rule 2: Min Price per Km
        FilterDropdownItem(
            title = if (config.language == "es") "2. Precio Mínimo por Kilómetro" else "2. Minimum Price per Kilometer",
            subtitle = if (config.language == "es") "Fórmula: Precio / Distancia Total" else "Formula: Price / Total Distance",
            valueText = "RD$ ${config.minPricePerKm.toInt()}/km",
            options = listOf(30.0, 35.0, 40.0, 50.0, 60.0),
            onSelect = { viewModel.updateConfig(config.copy(minPricePerKm = it)) }
        )

        // Rule 3: Max Trip Distance
        FilterDropdownItem(
            title = if (config.language == "es") "3. Distancia Máxima de Viaje" else "3. Maximum Ride Distance",
            subtitle = if (config.language == "es") "Filtra viajes excesivamente largos" else "Flares out extremely long trips",
            valueText = "${config.maxTripDistance} km",
            options = listOf(5.0, 10.0, 15.0, 20.0, 30.0, 50.0),
            onSelect = { viewModel.updateConfig(config.copy(maxTripDistance = it)) }
        )

        // Rule 4: Reject With Description (Switch)
        FilterSwitchItem(
            title = if (config.language == "es") "4. Sin Descripción" else "4. Reject With Notes",
            subtitle = if (config.language == "es") "Rechazar si el viaje incluye cualquier nota/descripción" else "Autodecline any trip containing passenger messages",
            checked = config.rejectWithDescription,
            onCheckedChange = { viewModel.updateConfig(config.copy(rejectWithDescription = it)) }
        )

        // Rule 5: Min Passenger Trips
        FilterDropdownItem(
            title = if (config.language == "es") "5. Viajes Mínimos del Pasajero" else "5. Minimum Passenger Trips",
            subtitle = if (config.language == "es") "Rechaza pasajeros nuevos o con poca experiencia" else "Avoid brand-new profiles with low order count",
            valueText = "${config.minPassengerTrips} viajes",
            options = listOf(5, 10, 20, 30, 50, 100),
            onSelect = { viewModel.updateConfig(config.copy(minPassengerTrips = it)) }
        )

        // Rule 6: Min Rating
        FilterDropdownItem(
            title = if (config.language == "es") "6. Calificación Mínima" else "6. Minimum Passenger Rating",
            subtitle = if (config.language == "es") "Calificación mínima requerida en estrellas" else "Decline users below star threshold",
            valueText = "★ ${config.minPassengerRating}",
            options = listOf(4.0, 4.2, 4.5, 4.7, 4.8, 4.9, 5.0),
            onSelect = { viewModel.updateConfig(config.copy(minPassengerRating = it)) }
        )

        // Rule 7: Max Time to Accept (Slider)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (config.language == "es") "7. Tiempo Máximo para Aceptar" else "7. Timeout to Autoaccept",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (config.language == "es") "Duración en segundos de la decisión" else "Deciding window in seconds",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = sliderAcceptTime,
                        onValueChange = {
                            sliderAcceptTime = it
                            viewModel.updateConfig(config.copy(maxTimeToAccept = it.toInt()))
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF10B981),
                            activeTrackColor = Color(0xFF10B981),
                            inactiveTrackColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${sliderAcceptTime.toInt()} s",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Rule 8: Sound Alert (Switch)
        FilterSwitchItem(
            title = if (config.language == "es") "8. Activar Sonido" else "8. Sound Alerts",
            subtitle = if (config.language == "es") "Reproducir alerta auditiva al detectar viaje válido" else "Play standard notification sound on match success",
            checked = config.playSound,
            onCheckedChange = { viewModel.updateConfig(config.copy(playSound = it)) }
        )

        // Rule 9: Vibration Alert (Switch)
        FilterSwitchItem(
            title = if (config.language == "es") "9. Vibración" else "9. Haptic Vibrations",
            subtitle = if (config.language == "es") "Vibrar el dispositivo móvil al detectar viaje válido" else "Trigger phone haptic buzz on match success",
            checked = config.enableVibration,
            onCheckedChange = { viewModel.updateConfig(config.copy(enableVibration = it)) }
        )

        // Rule 10: Show Overlay (Switch)
        FilterSwitchItem(
            title = if (config.language == "es") "10. Mostrar Ventana Flotante" else "10. Show Floating Overlay Widget",
            subtitle = if (config.language == "es") "Mostrar widget transparente con el estado sobre la pantalla" else "Keep active draggable dashboard floating over other apps",
            checked = config.showOverlay,
            onCheckedChange = { viewModel.updateConfig(config.copy(showOverlay = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ADVANCED CONFIGURATION HEADER
        Text(
            text = if (config.language == "es") "Configuración Avanzada" else "Advanced Settings",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // System Dark Theme switch
        FilterSwitchItem(
            title = if (config.language == "es") "Modo Oscuro" else "Dark Theme Appearance",
            subtitle = if (config.language == "es") "Alternar colores oscuros en la aplicación" else "Switch background appearance values",
            checked = config.isDarkMode,
            onCheckedChange = { viewModel.updateConfig(config.copy(isDarkMode = it)) }
        )

        // Language Select
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (config.language == "es") "Idioma" else "Language",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (config.language == "es") "Selecciona el idioma de interfaz" else "Adjust vocabulary translations",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (config.language == "es") "Español" else "English",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                val nextLang = if (config.language == "es") "en" else "es"
                                viewModel.updateConfig(config.copy(language = nextLang))
                            }
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DB / Data utilities
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (config.language == "es") "Exportar Historial" else "Export History Logs", color = Color.White)
            }

            Button(
                onClick = {
                    viewModel.updateConfig(AppConfig())
                    Toast.makeText(context, if (config.language == "es") "¡Configuración restaurada!" else "Filters reset!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Restaurar", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (config.language == "es") "Restaurar Configuración por Defecto" else "Restore Default Filters", color = Color.White)
            }

            Button(
                onClick = onBackupClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Backup, contentDescription = "Backup", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (config.language == "es") "Respaldar Base de Datos (SQLite)" else "Backup Local Database (SQLite)", color = Color.White)
            }
        }
    }
}

@Composable
fun <T> FilterDropdownItem(
    title: String,
    subtitle: String,
    valueText: String,
    options: List<T>,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = subtitle,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }

                Box {
                    Text(
                        text = valueText,
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { expanded = true }
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        options.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.toString(), color = Color.White) },
                                onClick = {
                                    onSelect(opt)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF0F172A)
                )
            )
        }
    }
}

// ==========================================
// 3. HISTORY TAB
// ==========================================
@Composable
fun HistoryTab(viewModel: MainViewModel, config: AppConfig) {
    val trips by viewModel.allTrips.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (config.language == "es") "Historial de Viajes" else "Processed Ride History",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${trips.size} " + (if (config.language == "es") "viajes analizados" else "orders audited"),
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }

            if (trips.isNotEmpty()) {
                IconButton(
                    onClick = { showClearConfirm = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Limpiar Historial", tint = Color(0xFFEF4444))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalTaxi,
                        contentDescription = "No Rides",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (config.language == "es") "Aún no hay viajes grabados" else "Quiet on the screen",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (config.language == "es") {
                            "Activa el asistente y simula un viaje o abre tu aplicación de viajes habitual."
                        } else {
                            "Activate your filter engine and simulate a trip to populate SQLite audit logs."
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripRecordCard(trip = trip, config = config)
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(if (config.language == "es") "¿Limpiar Historial?" else "Purge Logs?") },
            text = { Text(if (config.language == "es") "Esta acción eliminará todos los viajes registrados en la base de datos de forma irreversible." else "This will permanently purge all logged transactions in SQLite database. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(if (config.language == "es") "Eliminar Todo" else "Erase All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(if (config.language == "es") "Cancelar" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun TripRecordCard(trip: TripRecord, config: AppConfig) {
    val dateStr = remember(trip.timestamp) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(trip.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(
            1.dp,
            if (trip.isValid) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFFEF4444).copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Price & Validity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RD$ ${trip.price.toInt()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (trip.isValid) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (trip.isValid) {
                            if (config.language == "es") "VÁLIDO" else "VALID"
                        } else {
                            if (config.language == "es") "RECHAZADO" else "DECLINED"
                        },
                        color = if (trip.isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Body parameters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (config.language == "es") "Recogida" else "Pickup",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${trip.pickupDistance} km",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = if (config.language == "es") "Viaje" else "Trip",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${trip.tripDistance} km",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column {
                    Text(
                        text = if (config.language == "es") "Precio/km" else "Price/km",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${trip.pricePerKm.toInt()} RD\$/km",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold
                    )
                }

                Column {
                    Text(
                        text = if (config.language == "es") "Pasajero" else "Passenger",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "★ ${trip.passengerRating} (${trip.passengerTrips})",
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!trip.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${if (config.language == "es") "Nota" else "Notes"}: ${trip.description}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Reason for failure
            if (!trip.isValid && !trip.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Motivo",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = trip.rejectionReason,
                        color = Color(0xFFEF4444),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Footer Timestamp
            Text(
                text = dateStr,
                color = Color.Gray,
                fontSize = 9.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }
    }
}

// ==========================================
// 4. STATISTICS TAB (CUSTOM JETPACK COMPOSE CANVAS GRAPHS)
// ==========================================
@Composable
fun StatisticsTab(viewModel: MainViewModel, config: AppConfig) {
    val trips by viewModel.allTrips.collectAsState()

    // Process statistics
    val totalCount = trips.size
    val acceptedCount = trips.count { it.isValid }
    val rejectedCount = totalCount - acceptedCount
    val acceptanceRate = if (totalCount > 0) (acceptedCount.toFloat() / totalCount * 100).toInt() else 0

    val averagePrice = if (totalCount > 0) trips.map { it.price }.average() else 0.0
    val averagePricePerKm = if (totalCount > 0) trips.map { it.pricePerKm }.average() else 0.0

    // Rejection reasons breakdown
    val rejectionBreakdown = remember(trips) {
        val counts = mutableMapOf<String, Int>()
        trips.forEach { trip ->
            if (!trip.isValid && trip.rejectionReason != null) {
                // Simplify or clean reason string
                val reasonClean = when {
                    trip.rejectionReason.contains("recogida", ignoreCase = true) -> "Recogida Lejos"
                    trip.rejectionReason.contains("bajo", ignoreCase = true) -> "Precio/km Bajo"
                    trip.rejectionReason.contains("largo", ignoreCase = true) -> "Viaje Largo"
                    trip.rejectionReason.contains("descripción", ignoreCase = true) -> "Tiene Descripción"
                    trip.rejectionReason.contains("pocos", ignoreCase = true) -> "Pocos Viajes"
                    trip.rejectionReason.contains("calificación", ignoreCase = true) -> "Calificación"
                    else -> trip.rejectionReason
                }
                counts[reasonClean] = counts.getOrDefault(reasonClean, 0) + 1
            }
        }
        counts.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = if (config.language == "es") "Estadísticas de Auditoría" else "Audit & Analytics Dashboard",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Metrics Grid (2x3)
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricBox(
                title = if (config.language == "es") "Analizados" else "Total Audited",
                value = "$totalCount",
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            MetricBox(
                title = if (config.language == "es") "Aceptados" else "Accepted",
                value = "$acceptedCount",
                color = Color(0xFF10B981),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            MetricBox(
                title = if (config.language == "es") "Tasa Aceptación" else "Acceptance Rate",
                value = "$acceptanceRate%",
                color = if (acceptanceRate > 50) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricBox(
                title = if (config.language == "es") "Rechazados" else "Declined",
                value = "$rejectedCount",
                color = Color(0xFFEF4444),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            )
            MetricBox(
                title = if (config.language == "es") "Precio Promedio" else "Average Fare",
                value = "RD$ ${averagePrice.toInt()}",
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            MetricBox(
                title = if (config.language == "es") "Precio/Km Promedio" else "Avg Price/Km",
                value = "${averagePricePerKm.toInt()}/km",
                color = Color(0xFF10B981),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (totalCount == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.language == "es") {
                            "No hay datos estadísticos para graficar todavía. Procesa algunos viajes primero."
                        } else {
                            "Inject virtual orders or turn on tracking to render graphical analytical representations."
                        },
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // GRAPH 1: Circular Acceptance Pie Chart
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (config.language == "es") "DISTRIBUCIÓN DE ACEPTACIÓN (Gráfico Circular)" else "ACCEPTANCE DISTRIBUTION (Circular Donut Chart)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(150.dp)
                    ) {
                        Canvas(modifier = Modifier.size(150.dp)) {
                            val acceptedSweep = (acceptedCount.toFloat() / totalCount) * 360f
                            val rejectedSweep = 360f - acceptedSweep

                            // Draw Accepted slice (Green)
                            drawArc(
                                color = Color(0xFF10B981),
                                startAngle = -90f,
                                sweepAngle = acceptedSweep,
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw Rejected slice (Red)
                            if (rejectedCount > 0) {
                                drawArc(
                                    color = Color(0xFFEF4444),
                                    startAngle = -90f + acceptedSweep,
                                    sweepAngle = rejectedSweep,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$acceptanceRate%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = if (config.language == "es") "Aceptados" else "Ratio",
                                fontSize = 10.sp,
                                color = Color.LightGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legends Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Aceptados ($acceptedCount)", color = Color.White, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Rechazados ($rejectedCount)", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // GRAPH 2: Rejection Reasons Bar Chart
            if (rejectionBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (config.language == "es") "MOTIVOS DE RECHAZO (Gráfico de Barras)" else "DECLINE FREQUENCY BREAKDOWN (Horizontal Bar Chart)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val maxCount = rejectionBreakdown.first().second

                            rejectionBreakdown.forEach { (reason, count) ->
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(reason, color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                        Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Custom visual bar representing quantity
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF0F172A))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = count.toFloat() / maxCount)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(Color(0xFFEF4444), Color(0xFFEF4444).copy(alpha = 0.6f))
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    color: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}
