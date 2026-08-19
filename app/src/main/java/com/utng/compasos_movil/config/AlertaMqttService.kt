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

/**
 * servicio en primer plano (foreground service) encargado de mantener la conexión constante vía mqtt.
 * escucha publicaciones del reloj inteligente y de los miembros de la familia, gestiona la recepción
 * de señales de emergencia o audio y dispara notificaciones del sistema para alertar al usuario.
 */
class AlertaMqttService : Service() {

    private val scope          = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var mqttClient:    MqttClient? = null
    private lateinit var repository:     AlertaPhoneRepository
    private lateinit var sessionManager: SessionManager

    @Volatile private var familiarUserId: String? = null

    companion object {
        private const val CANAL_SVC    = "compasos_svc"
        private const val CANAL_SOS    = "compasos_sos"
        private const val NOTIF_SVC_ID = 9001
        private const val EXTRA_USER   = "usuario_id"

        /**
         * inicia el servicio en primer plano desde cualquier punto de la aplicación.
         *
         * @param context contexto de la aplicación para iniciar el servicio.
         * @param userId identificador opcional del usuario activo para suscribir sus tópicos familiares.
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

    /**
     * ciclo de vida inicial del servicio. configura el gestor de sesión, crea los canales de
     * notificación requeridos, inicia la notificación en primer plano (foreground), inicializa el repositorio
     * y establece la conexión mqtt.
     */
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
     * invocado cuando el servicio es iniciado explícitamente a través de [iniciar].
     * extrae el `userId` recibido y actualiza la suscripción a los tópicos de familiares en caso de cambio de usuario.
     *
     * @param intent intent suministrado a `startService`.
     * @param flags datos adicionales sobre la solicitud de inicio.
     * @param startId id único que representa esta solicitud de inicio.
     * @return `START_STICKY` para indicar que el sistema debe recrear el servicio si es destruido.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra(EXTRA_USER)
            ?: sessionManager.obtenerUsuarioId()

        if (userId != null && userId != familiarUserId) {
            familiarUserId = userId
            Log.d("AlertaMqttSvc", "onStartCommand → userId: $userId")
            if (mqttClient?.isConnected == true) {
                scope.launch { suscribirFamiliar(userId) }
            }
        }
        return START_STICKY
    }

    /**
     * servicio no enlazado (unbound), retorna null.
     */
    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * ciclo de vida final del servicio. cancela las corrutinas asociadas y desconecta el cliente mqtt.
     */
    override fun onDestroy() {
        scope.cancel()
        try { mqttClient?.disconnect() } catch (_: Exception) {}
        super.onDestroy()
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * obtiene la instancia de la base de datos de room e inicializa el [AlertaPhoneRepository]
     * con todas sus dependencias necesarias.
     */
    private fun inicializarRepo() {
        val db = AppDatabase.getInstance(applicationContext)
        repository = AlertaPhoneRepository(
            alertaDao         = db.alertaDao(),
            ubicacionDao      = db.ubicacionDao(),
            audioDao          = db.audioDao(),
            notificacionDao   = db.notificacionDao(),
            familiaUsuarioDao = db.familiaUsuarioDao(),
            dispositivoDao    = db.dispositivoDao(),   // ← NUEVO
            sessionManager    = sessionManager,
            context           = applicationContext
        )
    }

    // ── MQTT ──────────────────────────────────────────────────────────────────

    /**
     * establece la conexión asíncrona con el broker mqtt y se suscribe a los tópicos globales
     * del reloj inteligente (`compasos/alerta/+/sos` y `compasos/alerta/+/audio`).
     * posteriormente suscribe los tópicos propios del familiar si existe una sesión activa.
     */
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

                // El reloj — no se toca
                mqttClient!!.subscribe("${MqttConfig.TOPIC_ALERTA}/+/sos", 1) { _, msg ->
                    scope.launch { manejarSOS(String(msg.payload, Charsets.UTF_8)) }
                }
                mqttClient!!.subscribe("${MqttConfig.TOPIC_ALERTA}/+/audio", 1) { _, msg ->
                    scope.launch {
                        repository.procesarAudio(String(msg.payload, Charsets.UTF_8))
                    }
                }

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

    /**
     * realiza la suscripción a los tópicos específicos de un familiar para escuchar alertas entrantes
     * y actualizaciones de ubicación geográfica en tiempo real.
     *
     * @param userId identificador del usuario cuya familia se debe monitorear.
     */
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

    /**
     * atiende las señales sos enviadas por el reloj, inicia el rastreo gps continuo
     * y dispara la notificación visual de máxima prioridad en la barra de estado.
     *
     * @param payloadJson mensaje estructurado en formato json con la información de la emergencia.
     */
    private suspend fun manejarSOS(payloadJson: String) {
        Log.d("AlertaMqttSvc", "SOS recibido: $payloadJson")
        val alerta = repository.procesarSOS(payloadJson) ?: return
        // procesarSOS() ya se encarga de notificarFamiliares() y notificarTvs()
        repository.iniciarRastreoEnVivo(alerta.id, scope)
        mostrarNotifSOS(
            alertaId    = alerta.id,
            dispositivo = alerta.dispositivoId ?: "Reloj",
            titulo      = "Alerta ${alerta.tipoAlerta} recibida"
        )
    }

    /**
     * atiende el evento de una alerta proveniente de un familiar registrado, procesa la entidad
     * en el almacenamiento local y notifica al usuario en la barra de estado.
     *
     * @param payloadJson mensaje estructurado en formato json enviado por el familiar emisor.
     */
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

    /**
     * muestra una notificación de alta prioridad con alerta sonora/vibratoria para situaciones de emergencia sos.
     * vincula un `PendingIntent` que redirige directamente a la vista detallada de la alerta dentro de la app.
     *
     * @param alertaId identificador único de la alerta para la navegación.
     * @param dispositivo identificador o nombre del dispositivo emisor.
     * @param titulo título que se mostrará en la cabecera de la notificación.
     */
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

    /**
     * construye la notificación persistente requerida para el funcionamiento del servicio en primer plano.
     *
     * @return objeto [Notification] configurado con baja prioridad.
     */
    private fun notifServicio() = NotificationCompat.Builder(this, CANAL_SVC)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS activo")
        .setContentText("Escuchando alertas del reloj vinculado")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    /**
     * crea los canales de notificación requeridos para android o (api 26) o superior:
     * uno de baja prioridad para el servicio persistente y otro de alta prioridad para emergencias sos.
     */
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