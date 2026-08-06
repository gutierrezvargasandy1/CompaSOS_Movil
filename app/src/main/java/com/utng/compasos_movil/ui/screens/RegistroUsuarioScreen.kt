package com.utng.compasos_movil.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.utng.compasos_movil.navigation.Screen
import com.utng.compasos_movil.ui.theme.CompaSOSButtonShapeRadius
import com.utng.compasos_movil.ui.theme.CompaSOSColors
import com.utng.compasos_movil.ui.theme.CompaSOSFieldShapeRadius
import com.utng.compasos_movil.ui.theme.compaSOSTextFieldColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Estado del formulario, alineado 1 a 1 con la tabla `usuarios`.
 * `foto` se guarda como TEXT (por ejemplo, la URL/URI resultante de subir la imagen),
 * aquí solo se maneja la URI local seleccionada mientras no se sube.
 */
data class RegistroUsuarioState(
    val nombre: String = "",
    val apellidoPaterno: String = "",
    val apellidoMaterno: String = "",
    val correo: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val telefono: String = "",
    val fotoUri: String? = null,
    val fechaNacimiento: String = "", // formato yyyy-MM-dd, listo para DATE
    val sexo: String = ""
)

private val opcionesSexo = listOf("Masculino", "Femenino", "Otro", "Prefiero no decirlo")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroUsuarioScreen(
    navController: NavController,
    onRegistrar: (RegistroUsuarioState) -> Unit = {},
    onSeleccionarFoto: () -> Unit = {}
) {
    var state by remember { mutableStateOf(RegistroUsuarioState()) }
    var mostrarPassword by remember { mutableStateOf(false) }
    var mostrarConfirmarPassword by remember { mutableStateOf(false) }
    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarErrorPasswords by remember { mutableStateOf(false) }

    val puedeRegistrar = state.nombre.isNotBlank() &&
            state.correo.isNotBlank() &&
            state.password.isNotBlank() &&
            state.password == state.confirmarPassword

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        state = state.copy(fechaNacimiento = sdf.format(Date(millis)))
                    }
                    mostrarDatePicker = false
                }) { Text("Aceptar", color = CompaSOSColors.AccentBlue) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar", color = CompaSOSColors.TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
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
            LogoCompaSOS()

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Asistencia cuando más lo necesitas",
                color = CompaSOSColors.TextSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(24.dp))
            FotoPerfilPicker(fotoUri = state.fotoUri, onClick = onSeleccionarFoto)

            Spacer(Modifier.height(24.dp))

            CompaSOSTextField(
                value = state.nombre,
                onValueChange = { state = state.copy(nombre = it) },
                placeholder = "Nombre",
                icon = Icons.Filled.Person
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.apellidoPaterno,
                onValueChange = { state = state.copy(apellidoPaterno = it) },
                placeholder = "Apellido paterno",
                icon = Icons.Filled.Person
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.apellidoMaterno,
                onValueChange = { state = state.copy(apellidoMaterno = it) },
                placeholder = "Apellido materno",
                icon = Icons.Filled.Person
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.correo,
                onValueChange = { state = state.copy(correo = it) },
                placeholder = "Email",
                icon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.telefono,
                onValueChange = { state = state.copy(telefono = it) },
                placeholder = "Teléfono",
                icon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.fechaNacimiento,
                onValueChange = {},
                placeholder = "Fecha de nacimiento",
                icon = Icons.Filled.CalendarMonth,
                readOnly = true,
                onClick = { mostrarDatePicker = true }
            )
            Spacer(Modifier.height(14.dp))

            SexoDropdown(
                sexoSeleccionado = state.sexo,
                onSexoSeleccionado = { state = state.copy(sexo = it) }
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.password,
                onValueChange = {
                    state = state.copy(password = it)
                    mostrarErrorPasswords = false
                },
                placeholder = "Contraseña",
                icon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = mostrarPassword,
                onTogglePasswordVisibility = { mostrarPassword = !mostrarPassword }
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = state.confirmarPassword,
                onValueChange = {
                    state = state.copy(confirmarPassword = it)
                    mostrarErrorPasswords = false
                },
                placeholder = "Confirmar contraseña",
                icon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = mostrarConfirmarPassword,
                onTogglePasswordVisibility = { mostrarConfirmarPassword = !mostrarConfirmarPassword }
            )

            if (mostrarErrorPasswords) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Las contraseñas no coinciden",
                    color = Color(0xFFEF5350),
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (state.password != state.confirmarPassword) {
                        mostrarErrorPasswords = true
                    } else {
                        onRegistrar(state)
                        navController.navigate(Screen.PerfilMedico.route)
                    }
                },
                enabled = puedeRegistrar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompaSOSColors.AccentBlue,
                    disabledContainerColor = CompaSOSColors.AccentBlue.copy(alpha = 0.4f)
                )
            ) {
                Text("REGISTRARSE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Ya tienes cuenta? ", color = CompaSOSColors.TextSecondary, fontSize = 13.sp)
                Text(
                    "Inicia sesión",
                    color = CompaSOSColors.AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = {
                        navController.popBackStack()
                    })
                )
            }
        }
    }
}

@Composable
private fun LogoCompaSOS() {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "Compa",
            color = CompaSOSColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "SOS",
            color = CompaSOSColors.AccentBlue,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun FotoPerfilPicker(fotoUri: String?, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(CompaSOSColors.FieldBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Foto de perfil",
                tint = CompaSOSColors.AccentBlue,
                modifier = Modifier.size(48.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(CompaSOSColors.AccentBlue, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Agregar foto",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexoDropdown(sexoSeleccionado: String, onSexoSeleccionado: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        OutlinedTextField(
            value = sexoSeleccionado,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text("Sexo", color = CompaSOSColors.TextSecondary) },
            leadingIcon = {
                Icon(Icons.Filled.Wc, contentDescription = null, tint = CompaSOSColors.IconTint)
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            colors = compaSOSTextFieldColors(),
            shape = RoundedCornerShape(CompaSOSFieldShapeRadius),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opcionesSexo.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSexoSeleccionado(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

/**
 * Campo de texto reutilizable con el look del mockup: bordes redondeados,
 * ícono a la izquierda y, si es password, ícono de mostrar/ocultar a la derecha.
 * Si se pasa onClick (usado para el selector de fecha), el campo es de solo lectura y clickeable.
 */
@Composable
private fun CompaSOSTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisibility: (() -> Unit)? = null,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = CompaSOSColors.TextSecondary) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = CompaSOSColors.IconTint)
        },
        trailingIcon = {
            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = "Mostrar/ocultar contraseña",
                        tint = CompaSOSColors.IconTint
                    )
                }
            }
        },
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        readOnly = readOnly,
        colors = compaSOSTextFieldColors(),
        shape = RoundedCornerShape(CompaSOSFieldShapeRadius),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}