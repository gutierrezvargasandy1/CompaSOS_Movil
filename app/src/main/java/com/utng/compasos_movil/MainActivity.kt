package com.utng.compasos_movil

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.utng.compasos_movil.config.AlertaMqttService
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.navigation.AppNavigation
import com.utng.compasos_movil.ui.theme.CompaSOS_MovilTheme
import com.utng.compasos_movil.utils.SessionManager

/**
 * actividad principal de la aplicación compasos.
 * administra la inicialización de la base de datos local room, la gestión de permisos en tiempo de ejecución,
 * el arranque de los servicios mqtt en segundo plano y la renderización de la navegación ui.
 */
class MainActivity : ComponentActivity() {

    /**
     * lanzador para la solicitud múltiple de permisos en tiempo de ejecución.
     * evalúa el resultado y procede a iniciar los servicios de la aplicación si se otorgan los permisos.
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val fgsLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.FOREGROUND_SERVICE_LOCATION] == true
        } else true

        if (locationGranted && fgsLocationGranted) {
            iniciarServicios()
        } else {
            Log.e("MainActivity", "Permisos necesarios no otorgados")
        }
    }

    /**
     * inicializa la actividad, la base de datos, verifica permisos e inicia el contenedor composable principal.
     *
     * @param savedInstanceState estado guardado de la instancia de la actividad si existe.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)

        val usuarioDao            = db.usuarioDao()
        val perfilMedicoDao       = db.perfilMedicoDao()
        val familiaDao            = db.familiaDao()
        val familiaUsuarioDao     = db.familiaUsuarioDao()
        val contactoEmergenciaDao = db.contactoEmergenciaDao()
        val notificacionDao       = db.notificacionDao()
        val alertaDao             = db.alertaDao()
        val dispositivoDao        = db.dispositivoDao()
        val historialUbicacionDao = db.historialUbicacionDao()
        val ubicacionDao          = db.ubicacionDao()

        if (hasRequiredPermissions()) iniciarServicios() else requestPermissions()

        setContent {
            CompaSOS_MovilTheme {
                AppNavigation(
                    usuarioDao            = usuarioDao,
                    perfilMedicoDao       = perfilMedicoDao,
                    familiaDao            = familiaDao,
                    familiaUsuarioDao     = familiaUsuarioDao,
                    contactoEmergenciaDao = contactoEmergenciaDao,
                    notificacionDao       = notificacionDao,
                    alertaDao             = alertaDao,
                    dispositivoDao        = dispositivoDao,
                    historialUbicacionDao = historialUbicacionDao,
                    ubicacionDao          = ubicacionDao,
                    context               = applicationContext,
                    initialRoute          = intent.getStringExtra("nav_route")
                )
            }
        }
    }

    /**
     * atiende las actualizaciones de intent cuando la actividad se relanza.
     *
     * @param intent nuevo intent asignado a la actividad.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    /**
     * inicia los servicios en segundo plano mqtt (alertaMqttService y tvSyncService)
     * utilizando el identificador del usuario en sesión activa.
     */
    private fun iniciarServicios() {
        val userId = SessionManager(applicationContext).obtenerUsuarioId()
        AlertaMqttService.iniciar(applicationContext, userId)
        TvSyncService.iniciar(applicationContext, userId)
    }

    /**
     * evalúa si los permisos requeridos de ubicación e inicio de servicio en primer plano están concedidos.
     *
     * @return true si todos los permisos indispensables están activos, false en caso contrario.
     */
    private fun hasRequiredPermissions(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fgsLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        return locationGranted && fgsLocationGranted
    }

    /**
     * solicita al usuario la concesión de permisos de ubicación, servicio en primer plano y notificaciones.
     */
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}