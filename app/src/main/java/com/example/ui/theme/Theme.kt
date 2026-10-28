package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrowthPyramidsDarkScheme = darkColorScheme(
    primary = CyanPyramid,
    onPrimary = Color.White,
    secondary = EmeraldPyramid,
    onSecondary = Color.Black,
    tertiary = PurplePyramid,
    background = ObsidianDark,
    onBackground = Color.White,
    surface = SlateSurface,
    onSurface = Color.White,
    surfaceVariant = SlateCard,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrowthPyramidsDarkScheme,
        typography = Typography,
        content = content
    )
}
