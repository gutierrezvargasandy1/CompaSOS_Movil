package com.utng.compasos_movil.navigation

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utng.compasos_movil.AuthModule.AuthService
import com.utng.compasos_movil.AuthModule.AuthViewModel
import com.utng.compasos_movil.ProfileModule.PerfilMedicoRepository
import com.utng.compasos_movil.data.dao.PerfilMedicoDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.repository.UsuarioRepository
import com.utng.compasos_movil.data.wrapper.UsuarioConUbicacionesWrapper
import com.utng.compasos_movil.ui.screens.ContactosEmergenciaScreen
import com.utng.compasos_movil.ui.screens.DashboardScreen
import com.utng.compasos_movil.ui.screens.DispositivoConUsuario
import com.utng.compasos_movil.ui.screens.DispositivosScreen
import com.utng.compasos_movil.ui.screens.EditProfileScreen
import com.utng.compasos_movil.ui.screens.FamiliaScreen
import com.utng.compasos_movil.ui.screens.HistorialUbicacionesScreen
import com.utng.compasos_movil.ui.screens.LoginScreen
import com.utng.compasos_movil.ui.screens.NotificacionConAlerta
import com.utng.compasos_movil.ui.screens.NotificacionesScreen
import com.utng.compasos_movil.ui.screens.PerfilMedicoScreen
import com.utng.compasos_movil.ui.screens.ProfileScreen
import com.utng.compasos_movil.ui.screens.RegistroUsuarioScreen
import com.utng.compasos_movil.ui.screens.molals.MenuUsuario
import com.utng.compasos_movil.utils.SessionManager

// ============================================================
// FACTORY PARA CREAR AUTHVIEWMODEL CON DEPENDENCIAS
// ============================================================

/**
 * Factory para crear AuthViewModel con sus dependencias
 * Incluye AuthService y SessionManager
 */
class AuthViewModelFactory(
    private val authService: AuthService,
    private val sessionManager: SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {

    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authService, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ============================================================
// NAVEGACIÓN COMPLETA CON AUTHSERVICE, REPOSITORIOS Y SESSIONMANAGER
// ============================================================

/**
 * AppNavigation - Configuración completa de navegación
 *
 * Parámetros:
 *   usuarioDao: El DAO de Usuario para acceso a datos
 *   perfilMedicoDao: El DAO de PerfilMedico para acceso a datos médicos
 *   context: Context de la aplicación (para SessionManager)
 *
 * Ejemplo de uso en MainActivity:
 *
 * val db = Room.databaseBuilder(...).build()
 * setContent {
 *     AppNavigation(
 *         usuarioDao = db.usuarioDao(),
 *         perfilMedicoDao = db.perfilMedicoDao(),
 *         context = applicationContext
 *     )
 * }
 */
@Composable
fun AppNavigation(
    usuarioDao: UsuarioDao,
    perfilMedicoDao: PerfilMedicoDao,
    context: Context
) {
    val navController = rememberNavController()

    // ============================================================
    // CREAR INSTANCIAS DE REPOSITORIOS Y SERVICIOS
    // ============================================================

    // Crear repositorios
    val usuarioRepository = UsuarioRepository(usuarioDao)
    val perfilMedicoRepository = PerfilMedicoRepository(perfilMedicoDao)

    // Crear el servicio de autenticación
    val authService = AuthService(usuarioRepository)

    // Crear el gestor de sesión
    val sessionManager = SessionManager(context)

    // Crear el ViewModel de autenticación con factory
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authService, sessionManager)
    )

    // ============================================================
    // NAVHOST
    // ============================================================

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {

        // ============================================================
        // AUTENTICACIÓN
        // ============================================================

        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(Screen.Registro.route) {
            RegistroUsuarioScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // ============================================================
        // PERFIL MÉDICO (POST-REGISTRO)
        // ============================================================

        composable(Screen.PerfilMedico.route) {
            PerfilMedicoScreen(navController)
        }

        // ============================================================
        // APLICACIÓN PRINCIPAL
        // ============================================================

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                navController = navController,
                usuario = MenuUsuario(
                    nombreCompleto = sessionManager.obtenerUsuarioNombre() ?: "Usuario",
                    correo = sessionManager.obtenerUsuarioEmail() ?: "usuario@correo.com"
                ),
                onCerrarSesion = {
                    // Logout
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ============================================================
        // PERFIL - Con datos reales de BD
        // ============================================================

        composable(Screen.Perfil.route) {
            ProfileScreen(
                navController = navController,
                usuarioRepository = usuarioRepository,
                perfilMedicoRepository = perfilMedicoRepository,
                sessionManager = sessionManager
            )
        }

        // ============================================================
        // EDITAR PERFIL - Con datos reales de BD
        // ============================================================

        composable(Screen.EditarPerfil.route) {
            EditProfileScreen(
                navController = navController,
                usuarioRepository = usuarioRepository,
                perfilMedicoRepository = perfilMedicoRepository,
                sessionManager = sessionManager
            )
        }

        // ============================================================
        // CONTACTOS DE EMERGENCIA
        // ============================================================

        composable(Screen.ContactosEmergencia.route) {
            ContactosEmergenciaScreen(navController = navController)
        }

        // ============================================================
        // FAMILIA
        // ============================================================

        composable(Screen.Familia.route) {
            FamiliaScreen(navController = navController)
        }

        // ============================================================
        // DISPOSITIVOS
        // ============================================================

        composable(Screen.Dispositivos.route) {
            DispositivosScreen(
                navController = navController,
                dispositivos = listOf(
                    DispositivoConUsuario(
                        dispositivo = DispositivoEntity(
                            id = "1",
                            usuarioId = "u1",
                            tipo = "reloj",
                            modelo = "Watch Series 9",
                            fabricante = "Apple",
                            numeroSerie = null,
                            tokenFcm = null,
                            bateria = 78,
                            conectado = true,
                            fechaVinculacion = "12 jul 2026"
                        ),
                        usuario = UsuarioEntity(
                            id = "u1",
                            nombre = "Mamá",
                            apellidoPaterno = null,
                            apellidoMaterno = null,
                            correo = "mama@correo.com",
                            password = "",
                            telefono = null,
                            foto = null,
                            fechaNacimiento = null,
                            sexo = null,
                            activo = true,
                            fechaRegistro = "12 jul 2026"
                        )
                    ),
                    DispositivoConUsuario(
                        dispositivo = DispositivoEntity(
                            id = "2",
                            usuarioId = "u2",
                            tipo = "reloj",
                            modelo = "Galaxy Watch",
                            fabricante = "Samsung",
                            numeroSerie = null,
                            tokenFcm = null,
                            bateria = 15,
                            conectado = true,
                            fechaVinculacion = "03 ago 2026"
                        )
                    ),
                    DispositivoConUsuario(
                        dispositivo = DispositivoEntity(
                            id = "3",
                            usuarioId = "u3",
                            tipo = "telefono",
                            modelo = "iPhone 15",
                            fabricante = "Apple",
                            numeroSerie = null,
                            tokenFcm = null,
                            bateria = null,
                            conectado = false,
                            fechaVinculacion = "20 jun 2026"
                        )
                    )
                )
            )
        }

        // ============================================================
        // HISTORIAL DE UBICACIONES
        // ============================================================

        composable(route = Screen.HistorialUbicaciones.route) {
            val usuarioEstático = UsuarioEntity(
                id = "user_123",
                nombre = "Juan",
                apellidoPaterno = "Pérez",
                apellidoMaterno = "García",
                correo = "juan.perez@example.com",
                password = "hashedPassword",
                telefono = "+52 8123456789",
                foto = null,
                fechaNacimiento = "1995-05-15",
                sexo = "M",
                activo = true,
                fechaRegistro = "2024-01-15"
            )

            val ubicacionesEstáticas = listOf(
                HistorialUbicacionEntity(
                    id = "ubicacion_1",
                    usuarioId = "user_123",
                    latitud = 25.6866,
                    longitud = -100.3161,
                    fecha = "2024-01-20 08:30:00"
                ),
                HistorialUbicacionEntity(
                    id = "ubicacion_2",
                    usuarioId = "user_123",
                    latitud = 25.6900,
                    longitud = -100.3100,
                    fecha = "2024-01-20 09:15:00"
                ),
                HistorialUbicacionEntity(
                    id = "ubicacion_3",
                    usuarioId = "user_123",
                    latitud = 25.6950,
                    longitud = -100.3050,
                    fecha = "2024-01-20 10:45:00"
                ),
                HistorialUbicacionEntity(
                    id = "ubicacion_4",
                    usuarioId = "user_123",
                    latitud = 25.7000,
                    longitud = -100.2900,
                    fecha = "2024-01-20 12:30:00"
                ),
                HistorialUbicacionEntity(
                    id = "ubicacion_5",
                    usuarioId = "user_123",
                    latitud = 25.6850,
                    longitud = -100.3200,
                    fecha = "2024-01-20 14:20:00"
                ),
            )

            val wrapper = UsuarioConUbicacionesWrapper(
                usuario = usuarioEstático,
                historialUbicaciones = ubicacionesEstáticas
            )

            HistorialUbicacionesScreen(usuarioConUbicaciones = wrapper)
        }

        // ============================================================
        // NOTIFICACIONES
        // ============================================================

        composable(Screen.Notificaciones.route) {
            NotificacionesScreen(
                navController = navController,
                notificaciones = listOf(
                    NotificacionConAlerta(
                        notificacion = NotificacionEntity(
                            id = "1",
                            alertaId = "a1",
                            destinatario = "Mamá",
                            tipo = "SMS",
                            estado = "enviada",
                            fecha = "09 ago 2026, 10:14"
                        ),
                        alerta = AlertaEntity(
                            id = "a1",
                            usuarioId = "u1",
                            dispositivoId = null,
                            tipoAlerta = "Botón de pánico",
                            descripcion = "Alerta activada manualmente",
                            estado = "activa",
                            fecha = "09 ago 2026, 10:14"
                        )
                    ),
                    NotificacionConAlerta(
                        notificacion = NotificacionEntity(
                            id = "2",
                            alertaId = "a1",
                            destinatario = "Papá",
                            tipo = "SMS",
                            estado = "pendiente",
                            fecha = "09 ago 2026, 10:14"
                        )
                    ),
                    NotificacionConAlerta(
                        notificacion = NotificacionEntity(
                            id = "3",
                            alertaId = "a2",
                            destinatario = "Hermano",
                            tipo = "Llamada",
                            estado = "fallida",
                            fecha = "08 ago 2026, 22:03"
                        )
                    )
                )
            )
        }

        // ============================================================
        // CONFIGURACIÓN (Placeholder)
        // ============================================================

        composable(Screen.Configuracion.route) {
            PantallaPlaceholder("Configuración")
        }
    }
}

/**
 * Pantalla placeholder para rutas en construcción
 */
@Composable
private fun PantallaPlaceholder(titulo: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("$titulo — pantalla en construcción")
    }
}

