package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldLight,
    tertiary = EmeraldDark,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = PureWhite,
    onSecondary = SlateBackground,
    onTertiary = PureWhite,
    onBackground = GhostWhite,
    onSurface = GhostWhite
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    secondary = EmeraldPrimary,
    tertiary = EmeraldLight,
    background = Color(0xFFF8FAFC), // Very light soft blue-grey
    surface = Color(0xFFFFFFFF),
    onPrimary = PureWhite,
    onSecondary = PureWhite,
    onTertiary = SlateBackground,
    onBackground = SlateBackground,
    onSurface = SlateBackground
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to dark theme for high-end look
    dynamicColor: Boolean = false, // Set to false to enforce our custom branding
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
