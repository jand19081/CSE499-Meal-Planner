package io.github.and19081.mealplanner.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppTheme { LIGHT, DARK, SYSTEM }
enum class CornerStyle { ROUNDED, SQUARE }
enum class AccentColor(val color: Color, val label: String) {
    GREEN(Color(0xFF4CAF50), "Green"),
    BLUE(Color(0xFF2196F3), "Blue"),
    RED(Color(0xFFF44336), "Red"),
    PURPLE(Color(0xFF9C27B0), "Purple"),
    ORANGE(Color(0xFFFF9800), "Orange")
}

@Composable
fun MealPlannerTheme(
    content: @Composable () -> Unit
) {
    val theme by SettingsRepository.theme.collectAsState()
    val accentColor by SettingsRepository.accentColor.collectAsState()
    val cornerStyle by SettingsRepository.cornerStyle.collectAsState()
    
    val useDarkTheme = when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }
    
    // Generate the color scheme based on the selected accent color
    val colorScheme = remember(accentColor, useDarkTheme) {
        val baseColor = accentColor.color
        
        // Helper to "flatten" a transparent color over a solid background
        fun Color.flatten(background: Color): Color {
            val a = this.alpha
            return Color(
                red = (this.red * a) + (background.red * (1f - a)),
                green = (this.green * a) + (background.green * (1f - a)),
                blue = (this.blue * a) + (background.blue * (1f - a)),
                alpha = 1f
            )
        }

        // Simple derivation for secondary and tertiary
        val secondary = Color(
            red = (baseColor.red * 0.8f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.8f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 1.2f).coerceIn(0f, 1f)
        )
        val tertiary = Color(
            red = (baseColor.red * 1.2f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.8f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 0.8f).coerceIn(0f, 1f)
        )

        if (useDarkTheme) {
            val bg = Color(0xFF1C1B1F) // Standard M3 dark background
            darkColorScheme(
                primary = baseColor,
                onPrimary = Color.Black,
                primaryContainer = baseColor.copy(alpha = 0.3f).flatten(bg),
                onPrimaryContainer = Color.White,
                secondary = secondary,
                onSecondary = Color.Black,
                secondaryContainer = secondary.copy(alpha = 0.3f).flatten(bg),
                onSecondaryContainer = Color.White,
                tertiary = tertiary,
                onTertiary = Color.Black,
                tertiaryContainer = tertiary.copy(alpha = 0.3f).flatten(bg),
                onTertiaryContainer = Color.White,
                surfaceVariant = baseColor.copy(alpha = 0.15f).flatten(bg),
                onSurfaceVariant = Color.LightGray
            )
        } else {
            val bg = Color.White
            lightColorScheme(
                primary = baseColor,
                onPrimary = Color.White,
                primaryContainer = baseColor.copy(alpha = 0.12f).flatten(bg),
                onPrimaryContainer = baseColor,
                secondary = secondary,
                onSecondary = Color.White,
                secondaryContainer = secondary.copy(alpha = 0.12f).flatten(bg),
                onSecondaryContainer = secondary,
                tertiary = tertiary,
                onTertiary = Color.White,
                tertiaryContainer = tertiary.copy(alpha = 0.12f).flatten(bg),
                onTertiaryContainer = tertiary,
                surfaceVariant = baseColor.copy(alpha = 0.08f).flatten(bg),
                onSurfaceVariant = baseColor.copy(alpha = 0.7f).flatten(bg)
            )
        }
    }
    
    val shapes = remember(cornerStyle) {
        when (cornerStyle) {
            CornerStyle.ROUNDED -> Shapes(
                extraSmall = RoundedCornerShape(4.dp),
                small = RoundedCornerShape(8.dp),
                medium = RoundedCornerShape(12.dp),
                large = RoundedCornerShape(16.dp),
                extraLarge = RoundedCornerShape(28.dp)
            )
            CornerStyle.SQUARE -> Shapes(
                extraSmall = RoundedCornerShape(0.dp),
                small = RoundedCornerShape(0.dp),
                medium = RoundedCornerShape(0.dp),
                large = RoundedCornerShape(0.dp),
                extraLarge = RoundedCornerShape(0.dp)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}