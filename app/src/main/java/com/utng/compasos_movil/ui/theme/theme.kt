package com.utng.compasos_movil.ui.theme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de color de Material3 construido a partir de la paleta CompaSOSColors
 * (definida en CompaSOSTheme.kt) para que los componentes de Material3 como
 * Button, OutlinedTextField, etc. usen estos tonos de forma consistente.
 */
private val CompaSOSDarkColorScheme = darkColorScheme(
    primary = CompaSOSColors.AccentBlue,
    secondary = CompaSOSColors.AccentBlueDark,
    background = CompaSOSColors.Background,
    surface = CompaSOSColors.FieldBackground,
    onPrimary = CompaSOSColors.TextPrimary,
    onSecondary = CompaSOSColors.TextPrimary,
    onBackground = CompaSOSColors.TextPrimary,
    onSurface = CompaSOSColors.TextPrimary,
)

/**
 * Esta es la función que MainActivity.kt está importando y usando en setContent { }.
 * Envuelve toda la app en MaterialTheme con la paleta oscura de CompaSOS.
 */
@Composable
fun CompaSOS_MovilTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CompaSOSDarkColorScheme,
        content = content
    )
}