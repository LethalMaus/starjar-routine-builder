package dev.jamescullimore.starjar.routinebuilder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryYellow,
    onPrimary = Color.Black,
    primaryContainer = PrimaryYellow.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryYellow,
    background = AppBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = TextSecondaryDark,
    secondary = StarGold,
    onSecondary = Color.Black,
    error = Color(0xFFCF6679),
    onError = Color.Black,
    outline = Color(0xFF3E3E3E)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryYellow,
    onPrimary = Color.Black,
    primaryContainer = PrimaryYellow.copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF854D0E),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    secondary = StarGold,
    onSecondary = Color.Black,
    error = Color(0xFFB00020),
    onError = Color.White,
    outline = Color(0xFFE5E4DE)
)

@Composable
fun RoutineBuilderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = appTypography(),
        content = content
    )
}
