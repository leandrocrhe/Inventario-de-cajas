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

private val DarkColorScheme =
  darkColorScheme(
    primary = Amber80,
    onPrimary = Color(0xFF4D2700),
    primaryContainer = Amber40,
    onPrimaryContainer = Color(0xFFFFDCC1),
    secondary = AmberGrey80,
    onSecondary = Color(0xFF3D2E17),
    secondaryContainer = AmberGrey40,
    onSecondaryContainer = Color(0xFFFBE0BD),
    tertiary = Terracotta80,
    background = WarmDarkBackground,
    surface = WarmDarkSurface,
    surfaceVariant = WarmDarkSurfaceVariant,
    onBackground = Color(0xFFECE0DA),
    onSurface = Color(0xFFECE0DA),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Amber40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC1),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = AmberGrey40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBE0BD),
    onSecondaryContainer = Color(0xFF261904),
    tertiary = Terracotta40,
    background = WarmLightBackground,
    surface = WarmLightSurface,
    surfaceVariant = WarmLightSurfaceVariant,
    onBackground = Color(0xFF211A16),
    onSurface = Color(0xFF211A16),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Use our handcrafted warm inventory theme by default
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
