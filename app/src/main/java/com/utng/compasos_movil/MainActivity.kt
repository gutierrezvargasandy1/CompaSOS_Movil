package com.utng.compasos_movil

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.utng.compasos_movil.config.AlertaMqttService
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.navigation.AppNavigation
import com.utng.compasos_movil.ui.theme.CompaSOS_MovilTheme
import com.utng.compasos_movil.utils.SessionManager
import android.util.Log

class MainActivity : ComponentActivity() {

    // ── Launcher para solicitar permisos ─────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        val fgsLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions[Manifest.permission.FOREGROUND_SERVICE_LOCATION] == true
        } else {
            true // En versiones anteriores, no se necesita este permiso
        }

        if (locationGranted && fgsLocationGranted) {
            // Iniciar el servicio MQTT después de otorgar permisos
            val userId = SessionManager(applicationContext).obtenerUsuarioId()
            AlertaMqttService.iniciar(applicationContext, userId)
        } else {
            Log.e("MainActivity", "Permisos necesarios no otorgados")
            // Podrías mostrar un mensaje al usuario aquí
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ── Base de datos singleton ───────────────────────────────────────────
        val db = AppDatabase.getInstance(applicationContext)

        // ── DAOs ──────────────────────────────────────────────────────────────
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

        // ── Servicio MQTT ─────────────────────────────────────────────────────
        // Verificar permisos antes de iniciar el servicio
        if (hasRequiredPermissions()) {
            val userId = SessionManager(applicationContext).obtenerUsuarioId()
            AlertaMqttService.iniciar(applicationContext, userId)
        } else {
            // Solicitar permisos
            requestPermissions()
        }

        // ── Navegación ────────────────────────────────────────────────────────
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    // ── Métodos auxiliares ──────────────────────────────────────────────────

    private fun hasRequiredPermissions(): Boolean {
        val locationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val fgsLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return locationGranted && fgsLocationGranted
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        permissionLauncher.launch(permissions.toTypedArray())
    }
}