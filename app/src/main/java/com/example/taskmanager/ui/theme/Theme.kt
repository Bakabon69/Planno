package com.example.taskmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.taskmanager.settings.model.AccentColor
import com.example.taskmanager.settings.model.ThemeMode

@Composable
fun TaskManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.INDIGO,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.HIGH_CONTRAST -> true
    }

    val primaryColor = runCatching { Color(android.graphics.Color.parseColor(accentColor.hex)) }.getOrDefault(Color(0xFF4D7FFF))

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = primaryColor.copy(alpha = 0.8f),
            tertiary = Color(0xFF00BFA6),
            background = Color(0xFF0D1117),
            surface = Color(0xFF161B22),
            surfaceVariant = Color(0xFF21262D),
            onBackground = Color(0xFFF0F6FC),
            onSurface = Color(0xFFF0F6FC),
            onSurfaceVariant = Color(0xFF8B949E),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF334155)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            secondary = primaryColor.copy(alpha = 0.85f),
            tertiary = Color(0xFF0D9488),
            background = Color(0xFFF1F5F9), // Soft eye-friendly slate background (no harsh glare)
            surface = Color(0xFFFFFFFF),    // Clean white cards for distinct contrast
            surfaceVariant = Color(0xFFE2E8F0), // Subtle separator / tag / unselected chip tint
            onBackground = Color(0xFF1E293B), // Deep Slate Navy (gentle high-contrast readable text)
            onSurface = Color(0xFF1E293B),    // Deep Slate Navy
            onSurfaceVariant = Color(0xFF64748B), // Clear secondary slate text (never washed out)
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}