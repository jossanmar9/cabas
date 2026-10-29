package org.cabas.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette taken from the logo: woven-basket amber, receipt green and coral.
private val Light = lightColorScheme(
    primary = Color(0xFF246A52), onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEBD3), onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF865219), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBE), onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFFA4402C), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDAD3), onTertiaryContainer = Color(0xFF3E0500),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBF4), onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFFBF4), onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFF0EBDD), onSurfaceVariant = Color(0xFF4D473C),
    surfaceTint = Color(0xFF246A52),
    outline = Color(0xFF7F7768), outlineVariant = Color(0xFFD0C6B5),
    inverseSurface = Color(0xFF34302A), inverseOnSurface = Color(0xFFF8EFE4), inversePrimary = Color(0xFF8ED6B4),
    surfaceBright = Color(0xFFFFFBF4), surfaceDim = Color(0xFFE1DACE),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFAF4EA),
    surfaceContainer = Color(0xFFF4EEE3), surfaceContainerHigh = Color(0xFFEFE8DD),
    surfaceContainerHighest = Color(0xFFE9E2D7),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8ED6B4), onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF005139), onPrimaryContainer = Color(0xFFAAF2D0),
    secondary = Color(0xFFFFB873), onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3C00), onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFFFB4A4), onTertiary = Color(0xFF631004),
    tertiaryContainer = Color(0xFF842817), onTertiaryContainer = Color(0xFFFFDAD3),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17130E), onBackground = Color(0xFFEBE1D6),
    surface = Color(0xFF17130E), onSurface = Color(0xFFEBE1D6),
    surfaceVariant = Color(0xFF4D473C), onSurfaceVariant = Color(0xFFD0C6B5),
    surfaceTint = Color(0xFF8ED6B4),
    outline = Color(0xFF999080), outlineVariant = Color(0xFF4D473C),
    inverseSurface = Color(0xFFEBE1D6), inverseOnSurface = Color(0xFF34302A), inversePrimary = Color(0xFF246A52),
    surfaceBright = Color(0xFF3E3832), surfaceDim = Color(0xFF17130E),
    surfaceContainerLowest = Color(0xFF110E0A), surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF231F1A), surfaceContainerHigh = Color(0xFF2E2924),
    surfaceContainerHighest = Color(0xFF39342E),
)

@Composable
fun CabasTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) Dark else Light, content = content)
}
