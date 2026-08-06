package com.utng.compasos_movil.ui.theme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Paleta basada en el mockup: fondo azul-marino muy oscuro, tarjetas de campo
 * un poco más claras, y acento azul brillante para botones y textos destacados.
 * Ajusta estos valores si tienes una guía de estilo oficial del proyecto.
 */
object CompaSOSColors {
    val Background = Color(0xFF0B1229)
    val FieldBackground = Color(0xFF141B34)
    val FieldBorder = Color(0xFF223055)
    val AccentBlue = Color(0xFF1E88E5)
    val AccentBlueDark = Color(0xFF1565C0)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8A93B2)
    val IconTint = Color(0xFF6B7699)

    val LogoGradient: Brush
        get() = Brush.horizontalGradient(
            colors = listOf(AccentBlue, Color(0xFF42A5F5))
        )
}

val CompaSOSFieldShapeRadius = 28.dp
val CompaSOSButtonShapeRadius = 28.dp

/**
 * Colores reutilizables para OutlinedTextField, para no repetirlos en cada campo.
 */
@Composable
fun compaSOSTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CompaSOSColors.FieldBackground,
    unfocusedContainerColor = CompaSOSColors.FieldBackground,
    disabledContainerColor = CompaSOSColors.FieldBackground,
    focusedBorderColor = CompaSOSColors.AccentBlue,
    unfocusedBorderColor = CompaSOSColors.FieldBorder,
    focusedTextColor = CompaSOSColors.TextPrimary,
    unfocusedTextColor = CompaSOSColors.TextPrimary,
    focusedLeadingIconColor = CompaSOSColors.AccentBlue,
    unfocusedLeadingIconColor = CompaSOSColors.IconTint,
    focusedTrailingIconColor = CompaSOSColors.AccentBlue,
    unfocusedTrailingIconColor = CompaSOSColors.IconTint,
    unfocusedPlaceholderColor = CompaSOSColors.TextSecondary,
    focusedPlaceholderColor = CompaSOSColors.TextSecondary,
    cursorColor = CompaSOSColors.AccentBlue,
)