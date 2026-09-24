package com.example.wallpaperapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB68B73),
    onPrimary = Color(0xFF1A1413),
    primaryContainer = Color(0xFF593E34),
    onPrimaryContainer = Color(0xFFF6E5DF),
    secondary = Color(0xFFC9B09D),
    onSecondary = Color(0xFF1D1716),
    secondaryContainer = Color(0xFF463A36),
    onSecondaryContainer = Color(0xFFF5E7DF),
    tertiary = Color(0xFF93B4C3),
    onTertiary = Color(0xFF091D25),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEAE4DD),
    surface = Color(0xFF1D1B1A),
    onSurface = Color(0xFFEAE4DD),
    surfaceVariant = Color(0xFF2A2625),
    onSurfaceVariant = Color(0xFFD7CFC9),
    outline = Color(0xFF5D534F),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFB56C5D),
    onPrimary = Color(0xFF1F1A18),
    primaryContainer = Color(0xFFFFE2D8),
    onPrimaryContainer = Color(0xFF3A291F),
    secondary = Color(0xFFD9B8A9),
    onSecondary = Color(0xFF1B1715),
    secondaryContainer = Color(0xFFF4E3D9),
    onSecondaryContainer = Color(0xFF332620),
    tertiary = Color(0xFF7AA7B6),
    onTertiary = Color(0xFF0D1D2A),
    background = wallpaperBackground,
    onBackground = wallpaperPrimary,
    surface = Color(0xFFFFFFFF),
    onSurface = wallpaperPrimary,
    surfaceVariant = Color(0xFFF3EFEA),
    onSurfaceVariant = Color(0xFF534D49),
    outline = Color(0xFFCBC0B7),
)

@Composable
fun WallpaperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
