package com.cohors.app.ui.theme

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

// Cohors color palette — football pitch green inspired
private val PitchGreen = Color(0xFF1B6E3A)
private val PitchGreenDark = Color(0xFF0D4D24)
private val ChalkWhite = Color(0xFFF5F5F5)
private val WhistleYellow = Color(0xFFFFD600)
private val CardRed = Color(0xFFD32F2F)

private val DarkColorScheme = darkColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    primaryContainer = PitchGreenDark,
    onPrimaryContainer = Color.White,
    secondary = WhistleYellow,
    onSecondary = Color.Black,
    background = Color(0xFF121212),
    onBackground = ChalkWhite,
    surface = Color(0xFF1E1E1E),
    onSurface = ChalkWhite,
    error = CardRed
)

private val LightColorScheme = lightColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = PitchGreenDark,
    secondary = WhistleYellow,
    onSecondary = Color.Black,
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    error = CardRed
)

@Composable
fun CohorsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
