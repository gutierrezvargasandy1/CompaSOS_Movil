package com.utng.compasos_movil.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utng.compasos_movil.ui.screens.DashboardScreen
import com.utng.compasos_movil.ui.screens.LoginScreen
import com.utng.compasos_movil.ui.screens.PerfilMedicoScreen
import com.utng.compasos_movil.ui.screens.RegistroUsuarioScreen
import com.utng.compasos_movil.ui.screens.molals.MenuUsuario

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {

        composable(Screen.Login.route) {
            LoginScreen(navController)
        }

        composable(Screen.Registro.route) {
            RegistroUsuarioScreen(navController)
        }

        composable(Screen.PerfilMedico.route) {
            PerfilMedicoScreen(navController)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                // TODO: reemplazar por el usuario real (de tu ViewModel/sesión)
                usuario = MenuUsuario(
                    nombreCompleto = "Usuario",
                    correo = "usuario@correo.com"
                ),
                onCerrarSesion = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Pantallas del menú lateral: placeholders temporales ---
        // Reemplaza cada una por su pantalla real cuando esté lista, o el
        // NavController volverá a truenar si algún día borras una de estas
        // líneas mientras Screen.kt sigue apuntando a esa ruta.
        composable(Screen.ContactosEmergencia.route) {
            PantallaPlaceholder("Contactos de emergencia")
        }
        composable(Screen.Familia.route) {
            PantallaPlaceholder("Familia")
        }
        composable(Screen.Dispositivos.route) {
            PantallaPlaceholder("Dispositivos")
        }
        composable(Screen.HistorialUbicaciones.route) {
            PantallaPlaceholder("Historial de ubicaciones")
        }
        composable(Screen.Notificaciones.route) {
            PantallaPlaceholder("Notificaciones")
        }
        composable(Screen.Configuracion.route) {
            PantallaPlaceholder("Configuración")
        }
    }
}

@Composable
private fun PantallaPlaceholder(titulo: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("$titulo — pantalla en construcción")
    }
}