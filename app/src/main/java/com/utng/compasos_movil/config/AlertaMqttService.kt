package com.utng.compasos_movil.config

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.utng.compasos_movil.AlertaPhoneRepository.AlertaPhoneRepository
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class AlertaMqttService : Service() {

    private val scope          = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mqttClient:    MqttClient? = null
    private lateinit var repository:     AlertaPhoneRepository
    private lateinit var sessionManager: SessionManager

    // ← userId que queremos suscribir; puede llegar antes o después de conectar
    @Volatile private var familiarUserId: String? = null

    companion object {
        private const val CANAL_SVC    = "compasos_svc"
        private const val CANAL_SOS    = "compasos_sos"
        private const val NOTIF_SVC_ID = 9001
        private const val EXTRA_USER   = "usuario_id"

        /**
         * Llamar siempre con userId:
         *   - En MainActivity si el usuario ya tiene sesión activa.
         *   - En AuthViewModel justo después de un login exitoso.
         */
        fun iniciar(context: Context, userId: String? = null) {
            val intent = Intent(context, AlertaMqttService::class.java).apply {
                userId?.let { putExtra(EXTRA_USER, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(applicationContext)
        crearCanales()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_SVC_ID, notifServicio(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIF_SVC_ID, notifServicio())
        }

        inicializarRepo()
        conectarYSuscribir()
    }

    /**
     * Se llama cada vez que alguien invoca AlertaMqttService.iniciar().
     * Aprovechamos para recibir el userId y suscribir si el cliente ya conectó.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER)
            ?: sessionManager.obtenerUsuarioId()   // fallback: leer de la sesión actual

        if (userId != null && userId != familiarUserId) {
            familiarUserId = userId
            Log.d("AlertaMqttSvc", "onStartCommand → userId: $userId")
            // Si el cliente ya está conectado, suscribir ahora mismo
            if (mqttClient?.isConnected == true) {
                scope.launch { suscribirFamiliar(userId) }
            }
            // Si aún no conectó, conectarYSuscribir() usará familiarUserId al terminar
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        try { mqttClient?.disconnect() } catch (_: Exception) {}
        super.onDestroy()
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private fun inicializarRepo() {
        val db = AppDatabase.getInstance(applicationContext)
        repository = AlertaPhoneRepository(
            alertaDao         = db.alertaDao(),
            ubicacionDao      = db.ubicacionDao(),
            audioDao          = db.audioDao(),
            notificacionDao   = db.notificacionDao(),
            familiaUsuarioDao = db.familiaUsuarioDao(),
            sessionManager    = sessionManager,
            context           = applicationContext
        )
    }

    // ── MQTT ──────────────────────────────────────────────────────────────────

    private fun conectarYSuscribir() {
        scope.launch {
            try {
                val clientId = "compasos_phone_${System.currentTimeMillis()}"
                mqttClient = MqttClient(
                    MqttConfig.BROKER_URL, clientId, MemoryPersistence()
                ).apply {
                    connect(MqttConnectOptions().apply {
                        isCleanSession       = true
                        connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
                        keepAliveInterval    = MqttConfig.KEEP_ALIVE
                        isAutomaticReconnect = true
                    })
                }
                Log.d("AlertaMqttSvc", "Conectado al broker")

                // Topics del reloj
                mqttClient!!.subscribe("${MqttConfig.TOPIC_ALERTA}/+/sos", 1) { _, msg ->
                    scope.launch { manejarSOS(String(msg.payload, Charsets.UTF_8)) }
                }
                mqttClient!!.subscribe("${MqttConfig.TOPIC_ALERTA}/+/audio", 1) { _, msg ->
                    scope.launch {
                        repository.procesarAudio(String(msg.payload, Charsets.UTF_8))
                    }
                }

                // ── Topics familiares ─────────────────────────────────────────
                // Intentar con familiarUserId ya guardado, o leer de sesión ahora
                val userId = familiarUserId ?: sessionManager.obtenerUsuarioId()
                if (userId != null) {
                    familiarUserId = userId
                    suscribirFamiliar(userId)
                } else {
                    Log.w("AlertaMqttSvc",
                        "Sin sesión al conectar — esperando userId via onStartCommand")
                }

            } catch (e: Exception) {
                Log.e("AlertaMqttSvc", "Error MQTT: ${e.message}")
            }
        }
    }

    /** Suscribe los dos topics del familiar. Seguro llamarlo varias veces. */
    private suspend fun suscribirFamiliar(userId: String) {
        try {
            val topicAlerta    = "${MqttConfig.TOPIC_FAMILIA}/$userId/alerta"
            val topicUbicacion = "${MqttConfig.TOPIC_FAMILIA}/$userId/ubicacion"

            mqttClient!!.subscribe(topicAlerta, 1) { _, msg ->
                scope.launch { manejarAlertaFamiliar(String(msg.payload, Charsets.UTF_8)) }
            }
            mqttClient!!.subscribe(topicUbicacion, 1) { _, msg ->
                scope.launch {
                    repository.procesarUbicacionFamiliar(String(msg.payload, Charsets.UTF_8))
                }
            }
            Log.d("AlertaMqttSvc", "✅ Suscrito a topics de familiar: $userId")
        } catch (e: Exception) {
            Log.e("AlertaMqttSvc", "Error suscribiendo familiar: ${e.message}")
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private suspend fun manejarSOS(payloadJson: String) {
        Log.d("AlertaMqttSvc", "SOS recibido: $payloadJson")
        val alerta = repository.procesarSOS(payloadJson) ?: return
        repository.iniciarRastreoEnVivo(alerta.id, scope)
        mostrarNotifSOS(
            alertaId    = alerta.id,
            dispositivo = alerta.dispositivoId ?: "Reloj",
            titulo      = "Alerta ${alerta.tipoAlerta} recibida"
        )
    }

    private suspend fun manejarAlertaFamiliar(payloadJson: String) {
        Log.d("AlertaMqttSvc", "Alerta familiar recibida: $payloadJson")
        val alerta = repository.procesarAlertaFamiliar(payloadJson) ?: run {
            Log.w("AlertaMqttSvc", "procesarAlertaFamiliar devolvió null")
            return
        }
        Log.d("AlertaMqttSvc", "Alerta familiar guardada: ${alerta.id}")
        mostrarNotifSOS(
            alertaId    = alerta.id,
            dispositivo = alerta.dispositivoId ?: "Familiar",
            titulo      = "Un familiar necesita ayuda"
        )
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    private fun mostrarNotifSOS(alertaId: String, dispositivo: String, titulo: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)!!.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_route", "alertaDetalle/$alertaId")
        }
        val pending = PendingIntent.getActivity(
            this, alertaId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CANAL_SOS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ $titulo")
            .setContentText("Dispositivo: $dispositivo · Toca para ver la ubicación")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notif)
    }

    private fun notifServicio() = NotificationCompat.Builder(this, CANAL_SVC)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS activo")
        .setContentText("Escuchando alertas del reloj vinculado")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun crearCanales() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CANAL_SVC, "Servicio CompaSOS",
                    NotificationManager.IMPORTANCE_LOW)
            )
            nm.createNotificationChannel(
                NotificationChannel(CANAL_SOS, "Alertas de emergencia",
                    NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    enableLights(true)
                }
            )
        }
    }
}