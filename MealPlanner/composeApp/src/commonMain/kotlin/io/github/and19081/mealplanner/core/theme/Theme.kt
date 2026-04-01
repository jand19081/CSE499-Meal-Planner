package io.github.and19081.mealplanner.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.materialkolor.rememberDynamicColorScheme
import io.github.and19081.mealplanner.domain.repository.SettingsRepository
import kotlinx.serialization.Serializable

@Serializable
enum class AppTheme {
  LIGHT,
  DARK,
  SYSTEM,
}

enum class CornerStyle {
  ROUNDED,
  SQUARE,
}

enum class AccentColor(val color: Color, val label: String) {
  GREEN(Color(0xFF68A500), "Green"),
  BLUE(Color(0xFF2196F3), "Blue"),
  DARK_BLUE(Color(0xFF4C5CDC), "Dark Blue"),
  RED(Color(0xFFF44336), "Red"),
  PURPLE(Color(0xFF9C27B0), "Purple"),
  ORANGE(Color(0xFFFF9800), "Orange"),
}

data class AppSpacing(
    val small: Dp = 4.dp,
    val medium: Dp = 8.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
)

val LocalAppSpacing = staticCompositionLocalOf { AppSpacing() }

@Composable
fun MealPlannerTheme(settingsRepository: SettingsRepository, content: @Composable () -> Unit) {
  val theme by settingsRepository.theme.collectAsState()
  val accentColor by settingsRepository.accentColor.collectAsState()
  val cornerStyle by settingsRepository.cornerStyle.collectAsState()

  val adaptiveInfo = currentWindowAdaptiveInfo()
  val spacing =
      remember(adaptiveInfo.windowSizeClass.windowWidthSizeClass) {
        when (adaptiveInfo.windowSizeClass.windowWidthSizeClass) {
          WindowWidthSizeClass.COMPACT ->
              AppSpacing(small = 2.dp, medium = 4.dp, large = 12.dp, extraLarge = 16.dp)
          WindowWidthSizeClass.MEDIUM ->
              AppSpacing(small = 4.dp, medium = 8.dp, large = 16.dp, extraLarge = 24.dp)
          WindowWidthSizeClass.EXPANDED ->
              AppSpacing(small = 8.dp, medium = 12.dp, large = 24.dp, extraLarge = 32.dp)
          else -> AppSpacing()
        }
      }

  val useDarkTheme =
      when (theme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
      }

  val colorScheme = rememberDynamicColorScheme(seedColor = accentColor.color, isDark = useDarkTheme)

  val shapes =
      remember(cornerStyle) {
        when (cornerStyle) {
          CornerStyle.ROUNDED ->
              Shapes(
                  extraSmall = RoundedCornerShape(4.dp),
                  small = RoundedCornerShape(8.dp),
                  medium = RoundedCornerShape(12.dp),
                  large = RoundedCornerShape(16.dp),
                  extraLarge = RoundedCornerShape(28.dp),
              )
          CornerStyle.SQUARE ->
              Shapes(
                  extraSmall = RoundedCornerShape(0.dp),
                  small = RoundedCornerShape(0.dp),
                  medium = RoundedCornerShape(0.dp),
                  large = RoundedCornerShape(0.dp),
                  extraLarge = RoundedCornerShape(0.dp),
              )
        }
      }

  MaterialTheme(
      colorScheme = colorScheme,
      shapes = shapes,
      content = { CompositionLocalProvider(LocalAppSpacing provides spacing) { content() } },
  )
}
