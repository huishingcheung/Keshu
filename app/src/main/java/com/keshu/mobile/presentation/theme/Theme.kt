package com.keshu.mobile.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF155EEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7EEFF),
    onPrimaryContainer = Color(0xFF0B347A),
    secondary = Color(0xFF14805E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF4EC),
    onSecondaryContainer = Color(0xFF0A4D39),
    tertiary = Color(0xFFC7650A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8CF),
    onTertiaryContainer = Color(0xFF6F3500),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10213F),
    surfaceVariant = Color(0xFFF0F3F8),
    onSurfaceVariant = Color(0xFF5A667A),
    outline = Color(0xFFD7DEE9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF002D69),
    primaryContainer = Color(0xFF16498F),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFF70D5B0),
    onSecondary = Color(0xFF003829),
    secondaryContainer = Color(0xFF07543F),
    onSecondaryContainer = Color(0xFFB7F1D9),
    tertiary = Color(0xFFFFB873),
    onTertiary = Color(0xFF4B2700),
    tertiaryContainer = Color(0xFF703D08),
    onTertiaryContainer = Color(0xFFFFDCC0),
    background = Color(0xFF0D1420),
    surface = Color(0xFF121B29),
    onSurface = Color(0xFFE6ECF5),
    surfaceVariant = Color(0xFF1D2939),
    onSurfaceVariant = Color(0xFFB8C3D3),
    outline = Color(0xFF3C4A5E),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 42.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

@Composable
fun KeshuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
