package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.model.ThemeMode

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private fun parseHexColor(hex: String, fallback: Color): Color {
    return runCatching {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(colorInt or 0xFF000000L)
        } else if (cleaned.length == 8) {
            Color(colorInt)
        } else {
            fallback
        }
    }.getOrDefault(fallback)
}

private val BaseDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = LsDarkBackground,
    onBackground = LsDarkOnBackground,
    surface = LsDarkSurface,
    onSurface = LsDarkOnSurface,
    surfaceVariant = LsDarkSurfaceVariant,
    onSurfaceVariant = LsDarkOnSurfaceVariant,
    surfaceContainer = LsDarkSurfaceContainer,
    surfaceContainerHigh = LsDarkSurfaceContainerHigh,
    outline = LsDarkOutline,
    outlineVariant = LsDarkOutlineVariant,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

private val BaseLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = LsLightBackground,
    onBackground = LsLightOnBackground,
    surface = LsLightSurface,
    onSurface = LsLightOnSurface,
    surfaceVariant = LsLightSurfaceVariant,
    onSurfaceVariant = LsLightOnSurfaceVariant,
    surfaceContainer = LsLightSurfaceContainer,
    surfaceContainerHigh = LsLightSurfaceContainerHigh,
    outline = LsLightOutline,
    outlineVariant = LsLightOutlineVariant,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight
)

@Composable
fun LsNotesTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColorHex: String? = null,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val baseScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BaseDarkColorScheme
        else -> BaseLightColorScheme
    }

    val colorScheme = if (!accentColorHex.isNullOrEmpty()) {
        val customAccent = parseHexColor(accentColorHex, baseScheme.primary)
        baseScheme.copy(
            primary = customAccent,
            secondary = customAccent
        )
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window
            if (window != null) {
                WindowCompat.setDecorFitsSystemWindows(window, false)
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


