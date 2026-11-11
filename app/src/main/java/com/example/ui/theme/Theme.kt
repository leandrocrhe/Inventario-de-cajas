package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Stitch Design System: Dark Color Scheme (AMOLED Pure Black & Tactile Amber)
private val DarkColorScheme =
    darkColorScheme(
        primary = Amber80,
        onPrimary = DarkRoast,
        primaryContainer = AmberContainerDark,
        onPrimaryContainer = AmberContainerText,
        secondary = Color(0xFFDDC2A1),
        onSecondary = Color(0xFF3D2E17),
        secondaryContainer = Color(0xFF38240D),
        onSecondaryContainer = Color(0xFFFFB77D),
        tertiary = Color(0xFFFFB4A8),
        onTertiary = Color(0xFF561E18),
        tertiaryContainer = Color(0xFF73342B),
        onTertiaryContainer = Color(0xFFFFDAD4),
        background = AmoledBlack,
        onBackground = WarmSandWhite,
        surface = SurfaceCard,
        onSurface = WarmSandWhite,
        surfaceVariant = CharcoalWood,
        onSurfaceVariant = MutedSandGray,
        outline = BorderDark,
        outlineVariant = BorderSubtle,
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6)
    )

// Stitch Design System: Warm Light Color Scheme (Porcelain & Kraft Utility)
private val LightColorScheme =
    lightColorScheme(
        primary = Amber40,
        onPrimary = Color.White,
        primaryContainer = AmberLightTint,
        onPrimaryContainer = DarkAmberDeep,
        secondary = AmberGrey40,
        onSecondary = Color.White,
        secondaryContainer = SoftSandTint,
        onSecondaryContainer = Color(0xFF261904),
        tertiary = Terracotta40,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFDAD4),
        onTertiaryContainer = Color(0xFF3B0904),
        background = WarmLightBackground,
        onBackground = DeepEspresso,
        surface = WarmLightSurface,
        onSurface = DeepEspresso,
        surfaceVariant = WarmLightSurfaceVariant,
        onSurfaceVariant = WarmSlateBrown,
        outline = Color(0xFF867467),
        outlineVariant = Color(0xFFD9C2B3),
        error = Color(0xFFBA1A1A),
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002)
    )

enum class ThemeMode(val title: String) {
    SYSTEM("Sistema"),
    DARK("Oscuro"),
    LIGHT("Claro")
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    },
    dynamicColor: Boolean = false, // Use our handcrafted Stitch Warm / AMOLED theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
