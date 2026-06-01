package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    secondary = IOSBlue,
    tertiary = ActiveGreen,
    background = PitchBlack,
    surface = DeepCarbon,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = NothingRed,
    secondary = IOSBlue,
    tertiary = ActiveGreen,
    background = Color(0xFFF4F4F6),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1C1F),
    onSurface = Color(0xFF1C1C1F)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force Dark by default for luxury Nothing OS feel
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
