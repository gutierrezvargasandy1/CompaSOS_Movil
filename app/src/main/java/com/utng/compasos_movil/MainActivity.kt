package com.utng.compasos_movil

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utng.compasos_movil.config.AlertaMqttService
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.navigation.AppNavigation
import com.utng.compasos_movil.ui.theme.CompaSOS_MovilTheme
import com.utng.compasos_movil.utils.SessionManager

class MainActivity : ComponentActivity() {

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
        // Una sola llamada con el userId de la sesión activa (puede ser null
        // si el usuario aún no ha iniciado sesión — el servicio lo manejará).
        val userId = SessionManager(applicationContext).obtenerUsuarioId()
        AlertaMqttService.iniciar(applicationContext, userId)

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
}