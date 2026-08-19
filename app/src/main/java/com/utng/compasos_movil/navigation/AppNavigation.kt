package com.utng.compasos_movil.navigation

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.utng.compasos_movil.AuthModule.AuthService
import com.utng.compasos_movil.AuthModule.AuthViewModel
import com.utng.compasos_movil.LocalizacionModule.LocationViewModel
import com.utng.compasos_movil.ProfileModule.PerfilMedicoRepository
import com.utng.compasos_movil.data.dao.*
import com.utng.compasos_movil.data.repository.UsuarioRepository
import com.utng.compasos_movil.ui.screens.*
import com.utng.compasos_movil.ui.screens.molals.MenuUsuario
import com.utng.compasos_movil.utils.SessionManager

// ── Factory AuthViewModel ─────────────────────────────────────────────────────

/**
 * fábrica de proveedores para instanciar [AuthViewModel] inyectando el servicio de autenticación y gestor de sesión.
 *
 * @property application contexto global de la aplicación android.
 * @property authService servicio encargada del flujo de autenticación de usuarios.
 * @property sessionManager administrador del estado de la sesión activa del usuario.
 */
class AuthViewModelFactory(
    private val application:    Application,
    private val authService:    AuthService,
    private val sessionManager: SessionManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    /**
     * crea una nueva instancia de la clase viewmodel requerida si corresponde a [AuthViewModel].
     *
     * @param modelClass la clase del viewmodel a instanciar.
     * @return una nueva instancia de [AuthViewModel].
     * @throws IllegalArgumentException si la clase especificada no es reconocible por esta fábrica.
     */
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(application, authService, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ── AppNavigation ─────────────────────────────────────────────────────────────

/**
 * componente principal de navegación composable que configura el grafo de destinos ([NavHost]),
 * gestiona permisos de ubicación en el dashboard y conecta pantallas con sus respectivas dependencias y daos.
 *
 * @param usuarioDao acceso a la entidad de usuarios.
 * @param perfilMedicoDao acceso a la entidad de perfiles médicos.
 * @param familiaDao acceso a la entidad de familias.
 * @param familiaUsuarioDao acceso a las relaciones usuario-familia.
 * @param contactoEmergenciaDao acceso a la entidad de contactos de emergencia.
 * @param notificacionDao acceso a la entidad de notificaciones.
 * @param dispositivoDao acceso a la entidad de dispositivos vinculados.
 * @param alertaDao acceso a la entidad de alertas.
 * @param historialUbicacionDao acceso al historial de ubicaciones geográficas.
 * @param ubicacionDao acceso a las ubicaciones registradas en alertas.
 * @param context contexto de la aplicación para inicializar servicios e inyecciones.
 * @param initialRoute ruta opcional para dirigir la navegación al iniciar (e.g. desde notificaciones push).
 */
@Composable
fun AppNavigation(
    usuarioDao:            UsuarioDao,
    perfilMedicoDao:       PerfilMedicoDao,
    familiaDao:            FamiliaDao,
    familiaUsuarioDao:     FamiliaUsuarioDao,
    contactoEmergenciaDao: ContactoEmergenciaDao,
    notificacionDao:       NotificacionDao,
    dispositivoDao:        DispositivoDao,
    alertaDao:             AlertaDao,
    historialUbicacionDao: HistorialUbicacionDao,
    ubicacionDao:          UbicacionDao,
    context:               Context,

    initialRoute:          String? = null
) {
    val navController = rememberNavController()

    // ── Repositorios y servicios ──────────────────────────────────────────────
    val usuarioRepository      = UsuarioRepository(usuarioDao)
    val perfilMedicoRepository = PerfilMedicoRepository(perfilMedicoDao)
    val authService            = AuthService(usuarioRepository)
    val sessionManager         = SessionManager(context)

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(
            application    = context.applicationContext as Application,
            authService    = authService,
            sessionManager = sessionManager
        )
    )

    // ── Deep link desde notificación push ────────────────────────────────────
    LaunchedEffect(initialRoute) {
        if (!initialRoute.isNullOrBlank()) navController.navigate(initialRoute)
    }

    // ── NavHost ───────────────────────────────────────────────────────────────
    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route
    ) {

        composable(Screen.Login.route) {
            LoginScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(Screen.Registro.route) {
            RegistroUsuarioScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(Screen.PerfilMedico.route) {
            PerfilMedicoScreen(navController)
        }

        composable(Screen.Dashboard.route) {
            val locationViewModel: LocationViewModel = viewModel()
            val ubicacion by locationViewModel.ubicacion.collectAsState()
            val localContext = LocalContext.current

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { perms ->
                val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                if (ok) locationViewModel.iniciarSeguimiento()
            }

            LaunchedEffect(Unit) {
                val tienePerm = ContextCompat.checkSelfPermission(
                    localContext, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (tienePerm) locationViewModel.iniciarSeguimiento()
                else permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }

            DashboardScreen(
                navController  = navController,
                usuario        = MenuUsuario(
                    nombreCompleto = sessionManager.obtenerUsuarioNombre() ?: "Usuario",
                    correo         = sessionManager.obtenerUsuarioEmail()  ?: "usuario@correo.com"
                ),
                latitud        = ubicacion.latitud,
                longitud       = ubicacion.longitud,
                sessionManager = sessionManager,   // ← añadido
                onCerrarSesion = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.VincularTv.route) {
            VincularTvScreen(
                navController = navController,
                usuarioDao    = usuarioDao,
                dispositivoDao = dispositivoDao,
                familiaUsuarioDao = familiaUsuarioDao,
                historialUbicacionDao = historialUbicacionDao   // ← agregar

            )
        }

        composable(Screen.Perfil.route) {
            ProfileScreen(
                navController          = navController,
                usuarioRepository      = usuarioRepository,
                perfilMedicoRepository = perfilMedicoRepository,
                sessionManager         = sessionManager
            )
        }

        composable(Screen.EditarPerfil.route) {
            EditProfileScreen(
                navController          = navController,
                usuarioRepository      = usuarioRepository,
                perfilMedicoRepository = perfilMedicoRepository,
                sessionManager         = sessionManager
            )
        }

        composable(Screen.ContactosEmergencia.route) {
            ContactosEmergenciaScreen(
                navController         = navController,
                usuarioDao            = usuarioDao,
                contactoEmergenciaDao = contactoEmergenciaDao
            )
        }

        composable(Screen.Familia.route) {
            FamiliaScreen(
                navController     = navController,
                usuarioDao        = usuarioDao,
                familiaDao        = familiaDao,
                familiaUsuarioDao = familiaUsuarioDao
            )
        }

        composable(Screen.Dispositivos.route) {
            DispositivosScreen(
                navController  = navController,
                dispositivoDao = dispositivoDao,
                usuarioDao     = usuarioDao
            )
        }

        composable(Screen.HistorialUbicaciones.route) {
            HistorialUbicacionesScreen(
                historialDao   = historialUbicacionDao,
                usuarioDao     = usuarioDao,
                sessionManager = sessionManager
            )
        }

        composable(Screen.Notificaciones.route) {
            NotificacionesScreen(
                navController   = navController,
                notificacionDao = notificacionDao,
                alertaDao       = alertaDao,
                usuarioDao      = usuarioDao
            )
        }

        // ── Alerta Detalle ────────────────────────────────────────────────────
        composable(
            route     = Screen.AlertaDetalle.route,
            arguments = listOf(navArgument("alertaId") { type = NavType.StringType })
        ) { backStackEntry ->
            val alertaId = backStackEntry.arguments?.getString("alertaId")
                ?: return@composable
            AlertaDetalleScreen(
                alertaId      = alertaId,
                navController = navController,
                alertaDao     = alertaDao,
                ubicacionDao  = ubicacionDao,
                usuarioDao    = usuarioDao    // ← añadido
            )
        }

        // ── Alertas recibidas ─────────────────────────────────────────────────
        composable(Screen.AlertasRecibidas.route) {
            AlertasRecibidasScreen(
                navController   = navController,
                notificacionDao = notificacionDao,
                alertaDao       = alertaDao,
                usuarioDao      = usuarioDao  // ← añadido
            )
        }

        composable(Screen.Configuracion.route) {
            PantallaPlaceholder("Configuración")
        }
    }
}

/**
 * componente composable auxiliar para renderizar pantallas temporales en desarrollo.
 *
 * @param titulo título que identifica la sección que se encuentra en construcción.
 */
@Composable
private fun PantallaPlaceholder(titulo: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$titulo — pantalla en construcción")
    }
}