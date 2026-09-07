package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ElegantDarkColorScheme = darkColorScheme(
    primary = OceanCyan,
    onPrimary = Color(0xFF0A0F14),
    primaryContainer = Color(0xFF0891B2),
    onPrimaryContainer = Color(0xFFECFEFF),
    secondary = SeafoamGreen,
    onSecondary = Color(0xFF0A0F14),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = WarningAmber,
    onTertiary = Color(0xFF0A0F14),
    background = ElegantDarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = ElegantDarkCard,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = ElegantDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = ElegantDarkBorder,
    outlineVariant = Color(0xFF1E293B),
    error = DangerRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF10B981),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = WarningOrange,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = DangerRed,
    onError = Color.White
)

val SunContrastColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF08A),
    onPrimaryContainer = Color.Black,
    secondary = Color(0xFF047857),
    onSecondary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color(0xFFF8FAFC),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to Elegant Dark mode for maritime cockpit and sleek night aesthetics
    highContrastSunMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastSunMode -> SunContrastColorScheme
        darkTheme -> ElegantDarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme && !highContrastSunMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme && !highContrastSunMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
