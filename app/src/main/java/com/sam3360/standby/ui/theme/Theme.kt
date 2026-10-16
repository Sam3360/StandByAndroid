package com.sam3360.standby.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFC9B9FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFE5DFFF),
    background = androidx.compose.ui.graphics.Color(0xFF08090D),
    surface = androidx.compose.ui.graphics.Color(0xFF15161D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF3F1F7),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF16121F)
)

private val LightColorScheme = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF0077CC),
    secondary = androidx.compose.ui.graphics.Color(0xFFB9D7FF),
    background = androidx.compose.ui.graphics.Color(0xFFF4F7FB),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0D1320),
    onPrimary = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
)

@Composable
fun StandByTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
