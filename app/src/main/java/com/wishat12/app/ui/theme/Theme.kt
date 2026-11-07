package com.wishat12.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAA91FF),
    onPrimary = Color(0xFF201042),
    secondary = Color(0xFF72D6CA),
    background = Color(0xFF0D0D13),
    surface = Color(0xFF171720),
    surfaceVariant = Color(0xFF242432),
    onBackground = Color(0xFFF4F1FA),
    onSurface = Color(0xFFF4F1FA),
    outline = Color(0xFF454454)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6847D7),
    secondary = Color(0xFF087E75),
    background = Color(0xFFF7F6FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEAF5)
)

@Composable
fun WishAt12Theme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
