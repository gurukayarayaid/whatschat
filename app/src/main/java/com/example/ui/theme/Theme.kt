package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val title: String) {
    LIGHT("Mode Terang"),
    DARK("Mode Gelap"),
    SYSTEM("Default Sistem")
}

data class ExtendedChatColors(
    val chatBackground: Color,
    val sentBubble: Color,
    val receivedBubble: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val headerBackground: Color,
    val lockNoticeBackground: Color
)

val LocalChatColors = staticCompositionLocalOf {
    ExtendedChatColors(
        chatBackground = WaLightChatBackground,
        sentBubble = WaLightSentBubble,
        receivedBubble = WaLightReceivedBubble,
        textPrimary = WaLightTextPrimary,
        textSecondary = WaLightTextSecondary,
        divider = WaLightDivider,
        headerBackground = WaGreenPrimary,
        lockNoticeBackground = Color(0xFFFFF7DB)
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = WaTealDark,
    onPrimary = Color(0xFF111B21),
    primaryContainer = WaDarkHeader,
    onPrimaryContainer = Color(0xFFE9EDEF),
    secondary = WaGreenAccent,
    onSecondary = Color.Black,
    background = WaDarkBackground,
    onBackground = WaDarkTextPrimary,
    surface = WaDarkSurface,
    onSurface = WaDarkTextPrimary,
    surfaceVariant = Color(0xFF202C33),
    onSurfaceVariant = WaDarkTextSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = WaGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = WaGreenDark,
    onPrimaryContainer = Color.White,
    secondary = WaGreenAccent,
    onSecondary = Color.White,
    background = WaLightBackground,
    onBackground = WaLightTextPrimary,
    surface = WaLightSurface,
    onSurface = WaLightTextPrimary,
    surfaceVariant = Color(0xFFE9EDEF),
    onSurfaceVariant = WaLightTextSecondary
)

@Composable
fun WhatsChatTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val extendedChatColors = if (isDark) {
        ExtendedChatColors(
            chatBackground = WaDarkChatBackground,
            sentBubble = WaDarkSentBubble,
            receivedBubble = WaDarkReceivedBubble,
            textPrimary = WaDarkTextPrimary,
            textSecondary = WaDarkTextSecondary,
            divider = WaDarkDivider,
            headerBackground = WaDarkHeader,
            lockNoticeBackground = Color(0xFF182229)
        )
    } else {
        ExtendedChatColors(
            chatBackground = WaLightChatBackground,
            sentBubble = WaLightSentBubble,
            receivedBubble = WaLightReceivedBubble,
            textPrimary = WaLightTextPrimary,
            textSecondary = WaLightTextSecondary,
            divider = WaLightDivider,
            headerBackground = WaBluePrimary,
            lockNoticeBackground = Color(0xFFE0F2FE)
        )
    }

    CompositionLocalProvider(LocalChatColors provides extendedChatColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward compatibility alias for tests
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    WhatsChatTheme(
        themeMode = if (darkTheme) AppThemeMode.DARK else AppThemeMode.LIGHT,
        content = content
    )
}
