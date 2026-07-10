package com.formuloo.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = FormulooPrimary,
    onPrimary = FormulooOnPrimary,
    secondary = FormulooSecondary,
    onSecondary = FormulooOnSecondary,
    background = FormulooBackground,
    surface = FormulooSurface,
    error = FormulooError,
    outline = FormulooOutline,
    onSurfaceVariant = FormulooOnSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = FormulooPrimary,
    onPrimary = FormulooOnPrimary,
    secondary = FormulooSecondary,
    onSecondary = FormulooOnSecondary,
    error = FormulooError,
)

val FormulooShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

@Composable
fun FormulooTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FormulooTypography,
        shapes = FormulooShapes,
        content = content,
    )
}
