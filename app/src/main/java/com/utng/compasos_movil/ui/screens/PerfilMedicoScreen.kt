package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertBanner
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertToast
import com.utng.compasos_movil.ui.screens.molals.CompaSOSAlertType
import com.utng.compasos_movil.ui.screens.molals.rememberCompaSOSAlertState
import com.utng.compasos_movil.ui.theme.CompaSOSButtonShapeRadius
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.ui.theme.CompaSOSFieldShapeRadius
import com.utng.compasos_movil.ui.theme.compaSOSTextFieldColors

/**
 * estado del formulario, alineado con la tabla `perfil_medico`.
 * `usuario_id` no se captura aquí: se asigna al guardar, usando el id del
 * usuario recién creado en el paso anterior (RegistroUsuarioScreen).
 *
 * @property tipoSangre tipo de sangre seleccionado por el usuario.
 * @property alergias registro de alergias del usuario.
 * @property padecimientos enfermedades o padecimientos crónicos declarados.
 * @property medicamentos medicamentos de uso regular.
 * @property peso peso en kilogramos en formato numérico (cadena convertible a double).
 * @property altura altura en centímetros en formato numérico (cadena convertible a double).
 * @property observaciones notas u observaciones médicas adicionales.
 */
data class PerfilMedicoState(
    val tipoSangre: String = "",
    val alergias: String = "",
    val padecimientos: String = "",
    val medicamentos: String = "",
    val peso: String = "",   // NUMERIC(5,2) -> se valida/convierte antes de guardar
    val altura: String = "", // NUMERIC(5,2)
    val observaciones: String = ""
)

/** lista estática con las opciones de tipos de sangre disponibles en la aplicación. */
private val tiposSangre = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

/**
 * pantalla para el registro y gestión de la información del perfil médico del usuario.
 * permite ingresar datos clínicos, validando formatos numéricos y gestionando el guardado o la omisión.
 *
 * @param navController controlador para gestionar la navegación entre pantallas.
 * @param onGuardar función callback invocada al presionar guardar; retorna un mensaje de error si falla o null si es exitoso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilMedicoScreen(
    navController: NavController,
    onGuardar: (PerfilMedicoState) -> String? = { null }
) {
    var state by remember { mutableStateOf(PerfilMedicoState()) }
    var mostrarErrorNumeros by remember { mutableStateOf(false) }

    val aviso = rememberCompaSOSAlertState()

    // Todavía no existe una pantalla Home/Dashboard, así que por ahora regresamos
    // a Login limpiando el back stack. Cuando exista Home, cambia Screen.Login
    // por Screen.Home aquí.
    /**
     * navega hacia la pantalla inicial de inicio de sesión limpiando la pila de navegación previa.
     */
    fun irAHome() {
        navController.navigate(Screen.Login.route) {
            popUpTo(Screen.Login.route) { inclusive = true }
        }
    }

    /**
     * verifica que los valores ingresados en los campos de peso y altura correspondan a números válidos o estén vacíos.
     *
     * @return true si ambos campos contienen valores numéricos convertibles o están en blanco; false de lo contrario.
     */
    fun pesoAlturaValidos(): Boolean {
        val pesoOk = state.peso.isBlank() || state.peso.toDoubleOrNull() != null
        val alturaOk = state.altura.isBlank() || state.altura.toDoubleOrNull() != null
        return pesoOk && alturaOk
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Perfil médico",
                color = CompaSOSColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Esta información puede salvar tu vida en una emergencia",
                color = CompaSOSColors.TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            TipoSangreDropdown(
                tipoSeleccionado = state.tipoSangre,
                onTipoSeleccionado = { state = state.copy(tipoSangre = it) }
            )
            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                PerfilMedicoTextField(
                    value = state.peso,
                    onValueChange = {
                        state = state.copy(peso = it)
                        mostrarErrorNumeros = false
                    },
                    placeholder = "Peso (kg)",
                    icon = Icons.Filled.MonitorWeight,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                PerfilMedicoTextField(
                    value = state.altura,
                    onValueChange = {
                        state = state.copy(altura = it)
                        mostrarErrorNumeros = false
                    },
                    placeholder = "Altura (cm)",
                    icon = Icons.Filled.Height,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Banner fijo: peso/altura con formato inválido.
            CompaSOSAlertBanner(
                mensaje = "Ingresa peso y altura como números válidos (ej. 68.5)",
                tipo = CompaSOSAlertType.Advertencia,
                visible = mostrarErrorNumeros
            )

            Spacer(Modifier.height(4.dp))

            PerfilMedicoTextField(
                value = state.alergias,
                onValueChange = { state = state.copy(alergias = it) },
                placeholder = "Alergias",
                icon = Icons.Filled.WarningAmber,
                multiline = true
            )
            Spacer(Modifier.height(14.dp))

            PerfilMedicoTextField(
                value = state.padecimientos,
                onValueChange = { state = state.copy(padecimientos = it) },
                placeholder = "Padecimientos",
                icon = Icons.Filled.MedicalServices,
                multiline = true
            )
            Spacer(Modifier.height(14.dp))

            PerfilMedicoTextField(
                value = state.medicamentos,
                onValueChange = { state = state.copy(medicamentos = it) },
                placeholder = "Medicamentos",
                icon = Icons.Filled.Medication,
                multiline = true
            )
            Spacer(Modifier.height(14.dp))

            PerfilMedicoTextField(
                value = state.observaciones,
                onValueChange = { state = state.copy(observaciones = it) },
                placeholder = "Observaciones",
                icon = Icons.Filled.Notes,
                multiline = true
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (!pesoAlturaValidos()) {
                        mostrarErrorNumeros = true
                    } else {
                        val error = onGuardar(state)
                        if (error != null) {
                            aviso.mostrar(error, CompaSOSAlertType.Error)
                        } else {
                            aviso.mostrar("Perfil médico guardado", CompaSOSAlertType.Exito)
                            irAHome()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                colors = ButtonDefaults.buttonColors(containerColor = CompaSOSColors.AccentBlue)
            ) {
                Text("GUARDAR", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Omitir por ahora",
                color = CompaSOSColors.TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = { irAHome() })
            )
        }

        // Toast flotante: feedback al guardar (éxito o error del backend).
        CompaSOSAlertToast(
            mensaje = aviso.mensaje,
            tipo = aviso.tipo,
            visible = aviso.visible,
            onFinalizar = aviso::ocultar,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * componente desplegable privado para la selección del tipo de sangre.
 *
 * @param tipoSeleccionado valor del tipo de sangre seleccionado actualmente.
 * @param onTipoSeleccionado callback invocado al elegir una opción del menú.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TipoSangreDropdown(tipoSeleccionado: String, onTipoSeleccionado: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = tipoSeleccionado,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Tipo de sangre", color = CompaSOSColors.TextSecondary) },
            leadingIcon = {
                Icon(Icons.Filled.Bloodtype, contentDescription = null, tint = CompaSOSColors.IconTint)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = compaSOSTextFieldColors(),
            shape = RoundedCornerShape(CompaSOSFieldShapeRadius),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            tiposSangre.forEach { tipo ->
                DropdownMenuItem(
                    text = { Text(tipo) },
                    onClick = {
                        onTipoSeleccionado(tipo)
                        expandido = false
                    }
                )
            }
        }
    }
}

/**
 * componente de campo de texto personalizado y reutilizable para el formulario del perfil médico.
 *
 * @param value contenido textual actual del campo.
 * @param onValueChange callback ejecutado cuando el texto cambia.
 * @param placeholder texto explicativo o sugerencia cuando el campo está vacío.
 * @param icon icono descriptivo renderizado al inicio del campo.
 * @param keyboardType tipo de teclado virtual a mostrar al usuario.
 * @param multiline define si el campo permite múltiples líneas de entrada de texto.
 * @param modifier modificador para aplicar estilos o dimensiones adicionales al componente.
 */
@Composable
private fun PerfilMedicoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    multiline: Boolean = false,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = CompaSOSColors.TextSecondary) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = CompaSOSColors.IconTint)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = !multiline,
        minLines = if (multiline) 2 else 1,
        colors = compaSOSTextFieldColors(),
        shape = RoundedCornerShape(if (multiline) 18.dp else CompaSOSFieldShapeRadius),
        modifier = modifier.fillMaxWidth()
    )
}