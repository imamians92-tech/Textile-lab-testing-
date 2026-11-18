package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = NavyPrimary,
    primaryContainer = NavyContainer,
    onPrimaryContainer = Color(0xFFD6E6FF),
    secondary = SteelBlue,
    onSecondary = PureWhite,
    secondaryContainer = DarkElevatedSurface,
    onSecondaryContainer = SilverText,
    tertiary = LabEmerald,
    onTertiary = PureWhite,
    background = DeepNavyBg,
    onBackground = SilverText,
    surface = DarkCardSurface,
    onSurface = SilverText,
    surfaceVariant = DarkElevatedSurface,
    onSurfaceVariant = MutedSlateText,
    outline = BorderGreyDark,
    outlineVariant = Color(0xFF1E324D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = LabCrimsonDarkBg,
    onErrorContainer = Color(0xFFFECACA)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFDCE7F7),
    onPrimaryContainer = NavyPrimary,
    secondary = SkyAccent,
    onSecondary = PureWhite,
    secondaryContainer = SoftGreySurface,
    onSecondaryContainer = NavyContainer,
    tertiary = LabEmerald,
    onTertiary = PureWhite,
    background = CoolMist,
    onBackground = CharcoalText,
    surface = PureWhite,
    onSurface = CharcoalText,
    surfaceVariant = SoftGreySurface,
    onSurfaceVariant = SlateText,
    outline = BorderGreyLight,
    outlineVariant = Color(0xFFE2E8F0),
    error = LabCrimson,
    onError = PureWhite,
    errorContainer = LabCrimsonBg,
    onErrorContainer = Color(0xFF7F1D1D)
)

val LabShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun TextileLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LabShapes,
        content = content
    )
}
