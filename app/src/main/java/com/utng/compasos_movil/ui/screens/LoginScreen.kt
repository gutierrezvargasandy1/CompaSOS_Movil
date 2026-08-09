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
import androidx.navigation.NavController
import com.utng.compasos_movil.navigation.AppNavigation
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
 * onIniciarSesion ahora devuelve un resultado (String?) para poder mostrar el
 * banner de error correspondiente:
 *   - null           -> login exitoso
 *   - "mensaje..."    -> login fallido, se muestra ese mensaje en el toast de error
 *
 * Ejemplo real con tu lógica de autenticación:
 *
 * onIniciarSesion = { email, password ->
 *     if (autenticar(email, password)) null else "Correo o contraseña incorrectos"
 * }
 */
@Composable
fun LoginScreen(
    navController: NavController,
    onIniciarSesion: (String, String) -> String? = { _, _ -> null }
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mostrarPassword by remember { mutableStateOf(false) }

    val aviso = rememberCompaSOSAlertState()

    val puedeIniciarSesion = email.isNotBlank() && password.isNotBlank()

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
                keyboardType = KeyboardType.Email
            )
            Spacer(Modifier.height(14.dp))

            CompaSOSTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                icon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = mostrarPassword,
                onTogglePasswordVisibility = { mostrarPassword = !mostrarPassword }
            )

            Spacer(Modifier.height(16.dp))

            // Banner fijo: solo aparece si faltan datos por llenar.
            CompaSOSAlertBanner(
                mensaje = "Completa tu correo y contraseña para continuar",
                tipo = CompaSOSAlertType.Advertencia,
                visible = !puedeIniciarSesion && (email.isNotEmpty() || password.isNotEmpty())
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val error = onIniciarSesion(email, password)
                    if (error != null) {
                        aviso.mostrar(error, CompaSOSAlertType.Error)
                    } else {
                        aviso.mostrar("Inicio de sesión exitoso", CompaSOSAlertType.Exito)
                        navController.navigate(Screen.Dashboard.route)
                    }
                },
                enabled = puedeIniciarSesion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(CompaSOSButtonShapeRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CompaSOSColors.AccentBlue,
                    disabledContainerColor = CompaSOSColors.AccentBlue.copy(alpha = 0.4f)
                )
            ) {
                Text("INICIAR SESIÓN", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta? ", color = CompaSOSColors.TextSecondary, fontSize = 13.sp)
                Text(
                    "Regístrate",
                    color = CompaSOSColors.AccentBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = {
                        navController.navigate(Screen.Registro.route)
                    })
                )
            }
        }

        // Toast flotante: feedback del intento de inicio de sesión (éxito o error).
        CompaSOSAlertToast(
            mensaje = aviso.mensaje,
            tipo = aviso.tipo,
            visible = aviso.visible,
            onFinalizar = aviso::ocultar,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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

/**
 * Mismo componente de campo de texto usado en RegistroUsuarioScreen.
 * Si ya tienes CompaSOSTextField definido en un archivo compartido (por ejemplo
 * un archivo de componentes comunes), elimina esta copia de aquí y de
 * RegistroUsuarioScreen.kt para evitar duplicados, y solo importa la versión compartida.
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