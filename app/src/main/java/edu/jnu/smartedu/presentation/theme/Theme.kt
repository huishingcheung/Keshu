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
    primary = Color(0xFF3F5FCE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE4FF),
    onPrimaryContainer = Color(0xFF10245E),
    secondary = Color(0xFF087B69),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBDF2E3),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = Color(0xFF7A57A5),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0DBFF),
    onTertiaryContainer = Color(0xFF33204D),
    errorContainer = Color(0xFFFFDAD6),
    background = Color(0xFFF7F8FF),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF191B23),
    surfaceVariant = Color(0xFFE8EAF4),
    onSurfaceVariant = Color(0xFF5B6070),
    outline = Color(0xFFC5C7D3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7C7FF),
    onPrimary = Color(0xFF152B73),
    primaryContainer = Color(0xFF2C438C),
    onPrimaryContainer = Color(0xFFDDE4FF),
    secondary = Color(0xFF72DEC2),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF005143),
    onSecondaryContainer = Color(0xFFBDF2E3),
    tertiary = Color(0xFFE0B6FF),
    onTertiary = Color(0xFF472465),
    tertiaryContainer = Color(0xFF603B7D),
    onTertiaryContainer = Color(0xFFF0DBFF),
    background = Color(0xFF0B0E14),
    surface = Color(0xFF121721),
    onSurface = Color(0xFFE5E7F0),
    surfaceVariant = Color(0xFF202633),
    onSurfaceVariant = Color(0xFFC2C6D4),
    outline = Color(0xFF454B59),
)

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
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
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
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
