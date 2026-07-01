package edu.jnu.smartedu.presentation.theme

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
    primary = Color(0xFF0057D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF1FF),
    onPrimaryContainer = Color(0xFF002C6D),
    secondary = Color(0xFF008A68),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F5EF),
    onSecondaryContainer = Color(0xFF003D2E),
    tertiary = Color(0xFFB25E00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFF0D9),
    onTertiaryContainer = Color(0xFF4A2700),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF1F4F8),
    onSurfaceVariant = Color(0xFF5F6B7A),
    outline = Color(0xFFD7DEE8),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB7FF),
    onPrimary = Color(0xFF062466),
    primaryContainer = Color(0xFF143B9C),
    onPrimaryContainer = Color(0xFFE7EDFF),
    secondary = Color(0xFF82DDB9),
    onSecondary = Color(0xFF003825),
    secondaryContainer = Color(0xFF0E543B),
    onSecondaryContainer = Color(0xFFD8F7E9),
    tertiary = Color(0xFFC9C1FF),
    onTertiary = Color(0xFF2B1C72),
    tertiaryContainer = Color(0xFF4737A8),
    onTertiaryContainer = Color(0xFFF0EDFF),
    background = Color(0xFF101318),
    surface = Color(0xFF191D24),
    onSurface = Color(0xFFE6E8EE),
    surfaceVariant = Color(0xFF252B35),
    onSurfaceVariant = Color(0xFFAEB7C5),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 27.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun JnuSmartEduTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        // 重建为克制的校园效率工具配色：浅灰底、白色内容面、蓝色只负责主操作。
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
