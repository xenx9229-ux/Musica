package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalGlassThemeConfig = staticCompositionLocalOf { GlassThemeConfig() }
val LocalAlbumAccentColor = staticCompositionLocalOf { IosCyan }

@Composable
fun MusicaTheme(
    config: GlassThemeConfig = GlassThemeConfig(),
    albumAccent: Color = IosCyan,
    content: @Composable () -> Unit
) {
    val activeAccent = if (config.adaptiveAlbumColor) albumAccent else parseColorHex(config.accentHex)
    val isDark = config.glassMode == GlassMode.MIDNIGHT_NEON

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = activeAccent,
            onPrimary = Color(0xFF04101C),
            secondary = IosViolet,
            onSecondary = Color.White,
            tertiary = IosRose,
            background = MidnightBackgroundTop,
            onBackground = MidnightOnSurface,
            surface = MidnightSurface,
            onSurface = MidnightOnSurface,
            surfaceVariant = Color(0xFF1B233D),
            onSurfaceVariant = MidnightSecondaryText,
            outline = Color.White.copy(alpha = 0.22f)
        )
    } else {
        lightColorScheme(
            primary = activeAccent,
            onPrimary = Color.White,
            secondary = IosCobalt,
            onSecondary = Color.White,
            tertiary = IosRose,
            background = FrostBackgroundTop,
            onBackground = FrostOnSurface,
            surface = FrostSurface,
            onSurface = FrostOnSurface,
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = FrostSecondaryText,
            outline = Color.White.copy(alpha = 0.72f)
        )
    }

    val radius = config.cornerSoftnessDp.coerceIn(14f, 34f)
    val shapes = Shapes(
        small = RoundedCornerShape((radius * 0.55f).dp),
        medium = RoundedCornerShape((radius * 0.8f).dp),
        large = RoundedCornerShape(radius.dp),
        extraLarge = RoundedCornerShape((radius + 8f).dp)
    )

    CompositionLocalProvider(
        LocalGlassThemeConfig provides config,
        LocalAlbumAccentColor provides activeAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MusicaTypography,
            shapes = shapes,
            content = content
        )
    }
}
