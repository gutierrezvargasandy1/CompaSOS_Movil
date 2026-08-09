package com.utng.compasos_movil.ui.screens.molals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.ui.theme.CompaSOSFieldShapeRadius
import kotlinx.coroutines.delay

/**
 * Tipos de aviso soportados. Cada uno trae su propio color e ícono,
 * pero comparten la misma forma/tipografía que el resto de los componentes de CompaSOS.
 */
enum class CompaSOSAlertType {
    Exito,
    Error,
    Advertencia,
    Info
}

private data class AlertStyle(
    val color: Color,
    val icon: ImageVector
)

private fun estiloPara(tipo: CompaSOSAlertType): AlertStyle = when (tipo) {
    CompaSOSAlertType.Exito -> AlertStyle(Color(0xFF4CAF50), Icons.Filled.CheckCircle)
    CompaSOSAlertType.Error -> AlertStyle(Color(0xFFEF5350), Icons.Filled.Error)
    CompaSOSAlertType.Advertencia -> AlertStyle(Color(0xFFFFA726), Icons.Filled.Warning)
    CompaSOSAlertType.Info -> AlertStyle(CompaSOSColors.AccentBlue, Icons.Filled.Info)
}

/**
 * Aviso tipo "banner" para insertar dentro de un formulario o pantalla,
 * por ejemplo debajo de un campo de contraseña o arriba de un botón de enviar.
 *
 * Ejemplo (reemplaza el Text de error suelto en RegistroUsuarioScreen):
 *
 * CompaSOSAlertBanner(
 *     mensaje = "Las contraseñas no coinciden",
 *     tipo = CompaSOSAlertType.Error,
 *     visible = mostrarErrorPasswords
 * )
 */
@Composable
fun CompaSOSAlertBanner(
    mensaje: String,
    tipo: CompaSOSAlertType = CompaSOSAlertType.Error,
    visible: Boolean = true,
    onCerrar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val estilo = estiloPara(tipo)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = estilo.color.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(CompaSOSFieldShapeRadius)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = estilo.icon,
                contentDescription = null,
                tint = estilo.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = mensaje,
                color = CompaSOSColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            if (onCerrar != null) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cerrar aviso",
                        tint = estilo.color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Aviso tipo "toast" flotante, pensado para colocarse encima de todo el
 * contenido (por ejemplo dentro de un Box que envuelva la pantalla completa),
 * y que se oculta solo después de [duracionMs].
 *
 * Ejemplo, envolviendo la pantalla en un Box:
 *
 * Box(Modifier.fillMaxSize()) {
 *     // ...contenido de la pantalla...
 *
 *     CompaSOSAlertToast(
 *         mensaje = "Registro exitoso",
 *         tipo = CompaSOSAlertType.Exito,
 *         visible = mostrarToast,
 *         onFinalizar = { mostrarToast = false },
 *         modifier = Modifier.align(Alignment.TopCenter)
 *     )
 * }
 */
@Composable
fun CompaSOSAlertToast(
    mensaje: String,
    tipo: CompaSOSAlertType = CompaSOSAlertType.Exito,
    visible: Boolean,
    onFinalizar: () -> Unit = {},
    duracionMs: Long = 3000L,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(duracionMs)
            onFinalizar()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val estilo = estiloPara(tipo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = CompaSOSColors.FieldBackground,
                    shape = RoundedCornerShape(CompaSOSFieldShapeRadius)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = estilo.icon,
                contentDescription = null,
                tint = estilo.color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = mensaje,
                color = CompaSOSColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Recordatorio de estado simple para controlar un CompaSOSAlertToast desde
 * cualquier pantalla sin repetir boilerplate.
 *
 * Ejemplo de uso:
 *
 * val avisoState = rememberCompaSOSAlertState()
 * // al hacer login exitoso:
 * avisoState.mostrar("Bienvenido de nuevo", CompaSOSAlertType.Exito)
 * // en el Box raíz de la pantalla:
 * CompaSOSAlertToast(
 *     mensaje = avisoState.mensaje,
 *     tipo = avisoState.tipo,
 *     visible = avisoState.visible,
 *     onFinalizar = avisoState::ocultar,
 *     modifier = Modifier.align(Alignment.TopCenter)
 * )
 */
class CompaSOSAlertState {
    var visible by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf("")
        private set
    var tipo by mutableStateOf(CompaSOSAlertType.Info)
        private set

    fun mostrar(mensaje: String, tipo: CompaSOSAlertType = CompaSOSAlertType.Info) {
        this.mensaje = mensaje
        this.tipo = tipo
        this.visible = true
    }

    fun ocultar() {
        visible = false
    }
}

@Composable
fun rememberCompaSOSAlertState(): CompaSOSAlertState = remember { CompaSOSAlertState() }