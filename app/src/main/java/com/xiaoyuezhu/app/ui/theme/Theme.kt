package com.xiaoyuezhu.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue600,
    secondary = Emerald500,
    onSecondary = Color.White,
    secondaryContainer = Emerald100,
    onSecondaryContainer = Emerald500,
    tertiary = Purple600,
    tertiaryContainer = Purple100,
    error = Red500,
    errorContainer = Red50,
    background = BackgroundGray,
    onBackground = Gray800,
    surface = Color.White,
    onSurface = Gray800,
    surfaceVariant = BackgroundGray,
    onSurfaceVariant = Gray500,
    outline = Gray200,
    outlineVariant = Gray100,
)

@Composable
fun XiaoyuezhuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = XiaoyuezhuTypography,
        content = content
    )
}
