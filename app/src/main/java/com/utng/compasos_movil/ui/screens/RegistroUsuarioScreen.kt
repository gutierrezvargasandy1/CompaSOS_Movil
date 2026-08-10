package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.utng.compasos_movil.AuthModule.AuthState
import com.utng.compasos_movil.AuthModule.AuthViewModel
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
 * RegistroUsuarioScreen integrado con AuthViewModel
 * Maneja registro, validaciones, estados de carga y errores
 */
@Composable
fun RegistroUsuarioScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var nombre by remember { mutableStateOf("") }
    var apellidoPaterno by remember { mutableStateOf("") }
    var apellidoMaterno by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }
    var mostrarPasswordConfirm by remember { mutableStateOf(false) }

    val aviso = rememberCompaSOSAlertState()
    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    // Validaciones
    val camposObligatoriosLlenos = nombre.isNotBlank() &&
            apellidoPaterno.isNotBlank() &&
            correo.isNotBlank() &&
            password.isNotBlank() &&
            passwordConfirm.isNotBlank()

    val passwordsCoinciden = password == passwordConfirm
    val passwordValido = password.length >= 6
    val estaCargando = authState is AuthState.Loading
    val puedeRegistrar = camposObligatoriosLlenos && passwordsCoinciden && passwordValido && !estaCargando

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
            }

            item {
                Text(
                    text = "CompaSOS",
                    color = CompaSOSColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Crea tu cuenta",
                    color = CompaSOSColors.TextSecondary,
                    fontSize = 14.sp
                )
            }

            // ============================================================
            // FORMULARIO DE REGISTRO
            // ============================================================

            item {
                CompaSOSTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Nombre",
                    icon = Icons.Filled.Person,
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = apellidoPaterno,
                    onValueChange = { apellidoPaterno = it },
                    placeholder = "Apellido Paterno",
                    icon = Icons.Filled.Person,
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = apellidoMaterno,
                    onValueChange = { apellidoMaterno = it },
                    placeholder = "Apellido Materno (opcional)",
                    icon = Icons.Filled.Person,
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = "Correo Electrónico",
                    icon = Icons.Filled.Email,
                    keyboardType = KeyboardType.Email,
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    placeholder = "Teléfono (opcional)",
                    icon = Icons.Filled.Phone,
                    keyboardType = KeyboardType.Phone,
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Contraseña (mínimo 6 caracteres)",
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = mostrarPassword,
                    onTogglePasswordVisibility = { mostrarPassword = !mostrarPassword },
                    enabled = !estaCargando
                )
            }

            item {
                CompaSOSTextField(
                    value = passwordConfirm,
                    onValueChange = { passwordConfirm = it },
                    placeholder = "Confirmar Contraseña",
                    icon = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = mostrarPasswordConfirm,
                    onTogglePasswordVisibility = { mostrarPasswordConfirm = !mostrarPasswordConfirm },
                    enabled = !estaCargando
                )
            }

            // ============================================================
            // VALIDACIONES Y MENSAJES
            // ============================================================

            item {
                if (!passwordValido && password.isNotEmpty()) {
                    CompaSOSAlertBanner(
                        mensaje = "La contraseña debe tener mínimo 6 caracteres",
                        tipo = CompaSOSAlertType.Advertencia,
                        visible = true
                    )
                } else if (!passwordsCoinciden && passwordConfirm.isNotEmpty()) {
                    CompaSOSAlertBanner(
                        mensaje = "Las contraseñas no coinciden",
                        tipo = CompaSOSAlertType.Advertencia,
                        visible = true
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        authViewModel.clearError()
                        authViewModel.registrar(
                            nombre = nombre,
                            apellidoPaterno = apellidoPaterno,
                            apellidoMaterno = apellidoMaterno.takeIf { it.isNotEmpty() },
                            correo = correo,
                            password = password,
                            telefono = telefono.takeIf { it.isNotEmpty() }
                        )
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
                    if (estaCargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = CompaSOSColors.TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("CREAR CUENTA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("¿Ya tienes cuenta? ", color = CompaSOSColors.TextSecondary, fontSize = 13.sp)
                    Text(
                        "Inicia sesión",
                        color = CompaSOSColors.AccentBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(
                            enabled = !estaCargando,
                            onClick = {
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Registro.route) { inclusive = true }
                                }
                            }
                        )
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }

        // Toast flotante: feedback del registro
        CompaSOSAlertToast(
            mensaje = aviso.mensaje,
            tipo = aviso.tipo,
            visible = aviso.visible,
            onFinalizar = aviso::ocultar,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // ============================================================
    // MANEJO DE ESTADOS DEL REGISTRO
    // ============================================================

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                aviso.mostrar("✓ Registro exitoso. Inicia sesión.", CompaSOSAlertType.Exito)
                // Navegar al login después de un breve delay
                kotlinx.coroutines.delay(1500)
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Registro.route) { inclusive = true }
                }
            }

            is AuthState.Error -> {
                val mensaje = (authState as AuthState.Error).mensaje
                aviso.mostrar("✗ $mensaje", CompaSOSAlertType.Error)
            }

            is AuthState.Loading -> {
                // No hacer nada, el botón ya muestra el spinner
            }

            else -> {}
        }
    }
}

/**
 * Campo de texto reutilizable
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
    enabled: Boolean = true,
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
        enabled = enabled,
        colors = compaSOSTextFieldColors(),
        shape = RoundedCornerShape(CompaSOSFieldShapeRadius),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    )
}