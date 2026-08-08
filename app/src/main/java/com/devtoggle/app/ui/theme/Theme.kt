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
    primary = androidx.compose.ui.graphics.Color(0xFFB5C6FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002B73),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF113F98),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFDCE5FF),
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF394457),
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE2E8F5),
)

@Composable
fun DevToggleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}