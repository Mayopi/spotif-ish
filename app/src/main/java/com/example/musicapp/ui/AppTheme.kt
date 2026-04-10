package com.example.musicapp.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpotifyGreen = Color(0xFF1DB954)
val SpotifyCard = Color(0xFF212121)
val SpotifyBackground = Color(0xFF121212)
val SpotifyMuted = Color(0xFF535353)
val SpotifyTextMuted = Color(0xFFB3B3B3)
val SpotifyWhite = Color(0xFFF5F5F5)

private val SpotifishColors: ColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBackground,
    secondary = SpotifyCard,
    onSecondary = SpotifyWhite,
    tertiary = SpotifyMuted,
    background = SpotifyBackground,
    onBackground = SpotifyWhite,
    surface = SpotifyBackground,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyCard,
    onSurfaceVariant = SpotifyTextMuted,
    outline = SpotifyMuted,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SpotifishColors,
        content = content,
    )
}
