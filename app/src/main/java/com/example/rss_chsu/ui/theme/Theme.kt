package com.example.rss_chsu.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightBackground = Color(0xFFF7F7F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFF0F0EE)
val LightCardBorder = Color(0xFFE6E6E2)

val DarkPrimary = Color(0xFF1A1A1C)
val TextPrimary = Color(0xFF111113)
val TextSecondary = Color(0xFF76767A)
val ChipUnselected = Color(0xFFEBEBE8)
val BadgeContainer = Color(0xFFEEEEEC)

private val LightColorScheme = lightColorScheme(
    primary = DarkPrimary,
    onPrimary = Color.White,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = TextSecondary,
    outline = LightCardBorder,
    outlineVariant = ChipUnselected
)

private val DarkColorScheme = lightColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF383838),
    outlineVariant = Color(0xFF2C2C2C)
)

@Composable
fun RSS_chsuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
