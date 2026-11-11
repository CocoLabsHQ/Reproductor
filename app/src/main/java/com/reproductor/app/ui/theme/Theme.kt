package com.reproductor.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta extraída del logotipo
val Teal = Color(0xFF13C4B8)
val Cyan = Color(0xFF1E9BD7)
val Rooster = Color(0xFFE53935)
val Orange = Color(0xFFE8651A)
val Beak = Color(0xFFF5B82E)
val Mint = Color(0xFFA8F0D0)
val Navy = Color(0xFF0F2A44)

internal val LightColors = lightColorScheme(
    primary = Rooster, onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6), onPrimaryContainer = Color(0xFF410002),
    secondary = Cyan, onSecondary = Color.White,
    secondaryContainer = Color(0xFFCDEFFC), onSecondaryContainer = Navy,
    tertiary = Orange, onTertiary = Color.White,
    background = Color(0xFFE9FBF8), onBackground = Navy,
    surface = Color(0xFFF6FFFD), onSurface = Navy,
    surfaceVariant = Color(0xFFCFF3EE), onSurfaceVariant = Color(0xFF2B5560),
    outline = Color(0xFF5B8C94),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5A52), onPrimary = Color.White,
    primaryContainer = Color(0xFF7A1411), onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFF4CC3F0), onSecondary = Navy,
    secondaryContainer = Color(0xFF14506C), onSecondaryContainer = Color(0xFFCDEFFC),
    tertiary = Beak, onTertiary = Navy,
    background = Color(0xFF0B1F33), onBackground = Color(0xFFE6F7F5),
    surface = Color(0xFF0F2A44), onSurface = Color(0xFFE6F7F5),
    surfaceVariant = Color(0xFF173A5A), onSurfaceVariant = Mint,
    outline = Color(0xFF6FA8A6),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Degradado cian → turquesa como el fondo del logotipo (más oscuro en modo oscuro). */
fun brandBrush(dark: Boolean): Brush = if (dark) {
    Brush.verticalGradient(listOf(Color(0xFF0F2A44), Color(0xFF0E5A73), Color(0xFF0A4A52)))
} else {
    Brush.verticalGradient(listOf(Color(0xFF1E9BD7), Color(0xFF13C4B8), Color(0xFF0DB39F)))
}

@Composable
fun ReproductorTheme(theme: ResolvedTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalResolvedTheme provides theme) {
        MaterialTheme(
            colorScheme = theme.colorScheme(),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
