package com.devtoggle.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF2457D6),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFDCE5FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF00174D),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E8F7),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF131C2B),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF000000),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF222222),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF111111),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFCCCCCC),
    background = androidx.compose.ui.graphics.Color(0xFF000000),
    surface = androidx.compose.ui.graphics.Color(0xFF000000),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFAAAAAA),
    outlineVariant = androidx.compose.ui.graphics.Color(0xFF2A2A2A)
)

@Composable
fun DevToggleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}