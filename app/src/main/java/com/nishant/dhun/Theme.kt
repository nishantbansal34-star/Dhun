package com.nishant.dhun

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFFFF7A45)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF1A0E08),
    secondary = Color(0xFFFFB38A),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF0E0E10),
    surfaceVariant = Color(0xFF1C1C20),
    surfaceContainer = Color(0xFF16161A),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2),
    onSurfaceVariant = Color(0xFFA8A8B0),
)

@Composable
fun DhunTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
