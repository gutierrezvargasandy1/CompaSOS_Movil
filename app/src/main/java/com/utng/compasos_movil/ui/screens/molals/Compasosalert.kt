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
 * enumeración que define los tipos de aviso o alerta soportados en la interfaz.
 */
enum class CompaSOSAlertType {
    /**
     * aviso de éxito para operaciones completadas correctamente.
     */
    Exito,

    /**
     * aviso de error para indicar fallas o validaciones no superadas.
     */
    Error,

    /**
     * aviso de advertencia para alertar sobre situaciones precautorias.
     */
    Advertencia,

    /**
     * aviso informativo para entregar detalles o estados generales.
     */
    Info
}

/**
 * clase de datos privada que encapsula la configuración visual de estilo para un aviso.
 *
 * @property color color distintivo aplicado al fondo e ícono del aviso.
 * @property icon vector de imagen representado en el aviso.
 */
private data class AlertStyle(
    val color: Color,
    val icon: ImageVector
)

/**
 * determina el estilo visual (color e ícono) correspondiente según el tipo de alerta provisto.
 *
 * @param tipo tipo de alerta [CompaSOSAlertType] a evaluar.
 * @return estructura [AlertStyle] con el color e ícono configurados.
 */
private fun estiloPara(tipo: CompaSOSAlertType): AlertStyle = when (tipo) {
    CompaSOSAlertType.Exito -> AlertStyle(Color(0xFF4CAF50), Icons.Filled.CheckCircle)
    CompaSOSAlertType.Error -> AlertStyle(Color(0xFFEF5350), Icons.Filled.Error)
    CompaSOSAlertType.Advertencia -> AlertStyle(Color(0xFFFFA726), Icons.Filled.Warning)
    CompaSOSAlertType.Info -> AlertStyle(CompaSOSColors.AccentBlue, Icons.Filled.Info)
}

/**
 * componente composable de aviso tipo banner para mostrar dentro de formularios o contenedores.
 *
 * @param mensaje texto explicativo que se despliega en el banner.
 * @param tipo categoría de la alerta que define el estilo e ícono.
 * @param visible determina si el banner debe mostrarse u ocultarse con animación.
 * @param onCerrar función opcional invocada al presionar el botón de cierre.
 * @param modifier modificador de composición para personalizar el diseño externo.
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
 * componente composable de aviso flotante tipo toast que se oculta automáticamente tras una duración.
 *
 * @param mensaje contenido textual del mensaje de alerta.
 * @param tipo tipo de alerta que define la identidad gráfica del mensaje.
 * @param visible indica si el mensaje flotante está visible en la interfaz.
 * @param onFinalizar callback ejecutado automáticamente al finalizar la duración del mensaje.
 * @param duracionMs tiempo de visibilidad en milisegundos antes de ocultar.
 * @param modifier modificador para ajustar la posición o margen del componente.
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
 * clase gestora de estado para simplificar el control de visualización de alertas toast.
 *
 * @property visible indica la visibilidad actual del aviso.
 * @property mensaje texto configurado actualmente en la alerta.
 * @property tipo categoría actual de la alerta.
 */
class CompaSOSAlertState {
    var visible by mutableStateOf(false)
        private set
    var mensaje by mutableStateOf("")
        private set
    var tipo by mutableStateOf(CompaSOSAlertType.Info)
        private set

    /**
     * activa y muestra la alerta con el mensaje y tipo especificados.
     *
     * @param mensaje contenido del aviso a presentar.
     * @param tipo categoría visual para el aviso.
     */
    fun mostrar(mensaje: String, tipo: CompaSOSAlertType = CompaSOSAlertType.Info) {
        this.mensaje = mensaje
        this.tipo = tipo
        this.visible = true
    }

    /**
     * oculta la alerta en pantalla.
     */
    fun ocultar() {
        visible = false
    }
}

/**
 * función composable helper para recordar y mantener una instancia de [CompaSOSAlertState].
 *
 * @return instancia persistente de [CompaSOSAlertState] durante las recomposiciones.
 */
@Composable
fun rememberCompaSOSAlertState(): CompaSOSAlertState = remember { CompaSOSAlertState() }