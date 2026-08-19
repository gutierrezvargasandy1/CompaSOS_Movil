package com.utng.compasos_movil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
 * pantalla principal de inicio de sesión integrada con [AuthViewModel].
 * gestiona la autenticación de usuarios, validación de campos, estados de carga y despliegue de mensajes de error o éxito.
 *
 * @param navController controlador para gestionar la navegación entre pantallas.
 * @param authViewModel viewModel encargado de la lógica de negocio de autenticación.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }

    val aviso = rememberCompaSOSAlertState()
    val authState by authViewModel.authState.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    val puedeIniciarSesion = email.isNotBlank() && password.isNotBlank()
    val estaCargando = authState is AuthState.Loading

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CompaSOSColors.Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoCompaSOS()

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Asistencia cuando más lo necesitas",
                color = CompaSOSColors.TextSecondary,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(32.dp))

            CompaSOSTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                icon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                enabled = !estaCargando
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                icon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = mostrarPassword,
                onTogglePasswordVisibility = { mostrarPassword = !mostrarPassword },
                enabled = !estaCargando
            )

            Spacer(Modifier.height(16.dp))

            // Banner: advertencia si faltan campos
            CompaSOSAlertBanner(
                mensaje = "Completa tu correo y contraseña para continuar",
                tipo = CompaSOSAlertType.Advertencia,
                visible = !puedeIniciarSesion && (email.isNotEmpty() || password.isNotEmpty())
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    // Limpiar errores previos
                    authViewModel.clearError()
                    // Intentar login
                    authViewModel.login(email, password)
                },
                enabled = puedeIniciarSesion && !estaCargando,
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
                    Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta? ", color = CompaSOSColors.TextSecondary, fontSize = 13.sp)
                Text(
                    "Regístrate",
                    color = CompaSOSColors.AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(
                        enabled = !estaCargando,
                        onClick = {
                            navController.navigate(Screen.Registro.route)
                        }
                    )
                )
            }
        }

        // Toast flotante: feedback del intento de login
        CompaSOSAlertToast(
            mensaje = aviso.mensaje,
            tipo = aviso.tipo,
            visible = aviso.visible,
            onFinalizar = aviso::ocultar,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    // ============================================================
    // MANEJO DE ESTADOS DEL LOGIN
    // ============================================================

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                aviso.mostrar("✓ Inicio de sesión exitoso", CompaSOSAlertType.Exito)
                // Navegar al dashboard después de un breve delay
                kotlinx.coroutines.delay(1000)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
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
 * componente composable privado que muestra el logotipo estilizado de compasos.
 */
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

/**
 * campo de texto reutilizable estructurado para formularios de inicio de sesión y registro.
 *
 * @param value valor de texto actual dentro del campo.
 * @param onValueChange callback invocado al modificarse el valor del texto.
 * @param placeholder texto descriptivo de sugerencia cuando el campo está vacío.
 * @param icon icono vectorial representativo mostrado al inicio del campo.
 * @param keyboardType tipo de teclado virtual a solicitar al sistema.
 * @param isPassword bandera que determina si el campo debe ocultar los caracteres ingresados.
 * @param passwordVisible estado que controla si la contraseña se muestra en texto plano.
 * @param onTogglePasswordVisibility callback opcional ejecutado al presionar el icono para alternar visibilidad.
 * @param readOnly define si el contenido es únicamente de lectura.
 * @param enabled indica si el campo permite interacción por parte del usuario.
 * @param onClick callback opcional ejecutado al hacer clic sobre el campo.
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