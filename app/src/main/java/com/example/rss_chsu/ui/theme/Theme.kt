package com.example.rss_chsu.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Константы по умолчанию
private val BaseLightBackground = Color(0xFFF7F7F5)
private val BaseLightSurface = Color(0xFFFFFFFF)
private val BaseLightSurfaceContainer = Color(0xFFF0F0EE)
private val BaseLightCardBorder = Color(0xFFE6E6E2)

private val BaseDarkPrimary = Color(0xFF1A1A1C)
private val BaseTextPrimary = Color(0xFF111113)
private val BaseTextSecondary = Color(0xFF76767A)
private val BaseChipUnselected = Color(0xFFEBEBE8)
private val BaseBadgeContainer = Color(0xFFEEEEEC)

// Динамические провайдеры цветов
val LightBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val LightSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val LightSurfaceContainer: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val LightCardBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val DarkPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.primary

val OnDarkPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onPrimary

val TextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val ChipUnselected: Color
    @Composable get() = MaterialTheme.colorScheme.outlineVariant

val BadgeContainer: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

private val LightColorScheme = lightColorScheme(
    primary = BaseDarkPrimary,
    onPrimary = Color.White,
    background = BaseLightBackground,
    onBackground = BaseTextPrimary,
    surface = BaseLightSurface,
    onSurface = BaseTextPrimary,
    surfaceVariant = BaseLightSurfaceContainer,
    onSurfaceVariant = BaseTextSecondary,
    outline = BaseLightCardBorder,
    outlineVariant = BaseChipUnselected
)

private val DarkColorScheme = darkColorScheme(
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
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
