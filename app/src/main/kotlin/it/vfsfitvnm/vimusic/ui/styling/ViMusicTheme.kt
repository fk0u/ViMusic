package it.vfsfitvnm.vimusic.ui.styling

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import it.vfsfitvnm.vimusic.ui.styling.SFProFontFamily

/**
 * ViMusic Material3 Theme
 * Provides an enhanced Material3 theme with dynamic color support
 * Modified by KOU
 */
@Composable
fun ViMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // Dynamic color is available on Android 12+
    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkViMusicColorScheme
        else -> lightViMusicColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ViMusicTypography,
        shapes = ViMusicShapes,
        content = content
    )
}

// Custom light color scheme
private val lightViMusicColorScheme = lightColorScheme(
    primary = Color(0xFF006687),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBDE9FF),
    onPrimaryContainer = Color(0xFF001F2A),
    secondary = Color(0xFF4D616C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD0E6F2),
    onSecondaryContainer = Color(0xFF081E27),
    tertiary = Color(0xFF5E5B7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3DFFF),
    onTertiaryContainer = Color(0xFF1A1836),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFCFF),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFBFCFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDCE4E9),
    onSurfaceVariant = Color(0xFF40484C),
    outline = Color(0xFF70787D),
    inverseOnSurface = Color(0xFFF0F1F3),
    inverseSurface = Color(0xFF2E3133),
    inversePrimary = Color(0xFF67D3FF)
)

// Custom dark color scheme
private val darkViMusicColorScheme = darkColorScheme(
    primary = Color(0xFF67D3FF),
    onPrimary = Color(0xFF003547),
    primaryContainer = Color(0xFF004D66),
    onPrimaryContainer = Color(0xFFBDE9FF),
    secondary = Color(0xFFB4CAD6),
    onSecondary = Color(0xFF1F333D),
    secondaryContainer = Color(0xFF354954),
    onSecondaryContainer = Color(0xFFD0E6F2),
    tertiary = Color(0xFFC6C3EA),
    onTertiary = Color(0xFF2F2D4D),
    tertiaryContainer = Color(0xFF464364),
    onTertiaryContainer = Color(0xFFE3DFFF),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1E),
    onBackground = Color(0xFFE1E2E5),
    surface = Color(0xFF191C1E),
    onSurface = Color(0xFFE1E2E5),
    surfaceVariant = Color(0xFF40484C),
    onSurfaceVariant = Color(0xFFC0C8CD),
    outline = Color(0xFF8A9297),
    inverseOnSurface = Color(0xFF191C1E),
    inverseSurface = Color(0xFFE1E2E5),
    inversePrimary = Color(0xFF006687)
)

// Custom typography
private val ViMusicTypography = androidx.compose.material3.Typography(
    displayLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displayMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    displaySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    headlineSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    titleSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    bodySmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    labelLarge = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelMedium = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    labelSmall = androidx.compose.ui.text.TextStyle(
        fontFamily = SFProFontFamily,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
    )
)

// Custom shapes
private val ViMusicShapes = androidx.compose.material3.Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(8),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12),
    large = androidx.compose.foundation.shape.RoundedCornerShape(16)
)