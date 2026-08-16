package com.utng.compasos_movil.config


import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * ═══════════════════════════════════════════════════════════════════════════
 *  EL SERVICIO QUE PEDISTE: después de vincular, manda a la pantalla los
 *  familiares, sus ubicaciones y las notificaciones — constantemente.
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * Corre APARTE de AlertaMqttService, con su propio clientId y su propia
 * conexión. No toca nada del flujo del reloj.
 *
 * Hace tres cosas:
 *
 *  1. LATIDO. Cada 20 s arma un snapshot (usuario + familiares + última
 *     ubicación de cada uno, sacada de `historial_ubicacion`) y lo publica
 *     RETAINED a cada TV de `dispositivos WHERE tipo='tv'`.
 *     Retained es lo que hace que la TV se recupere sola: si la apagas y la
 *     prendes, el broker le entrega el último snapshot al instante.
 *
 *  2. GPS PROPIO. En cada latido toma la última posición del teléfono y la
 *     guarda en `historial_ubicacion`, para que el dueño también aparezca
 *     moviéndose en el mapa de la TV aunque no haya ninguna alerta activa.
 *
 *  3. EMPUJE INMEDIATO. AlertaPhoneRepository llama a los métodos estáticos
 *     de abajo cuando entra una alerta, ubicación o notificación nueva, y se
 *     publica en el momento sin esperar al latido.
 */
class TvSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mqtt  = MqttManager()
    private val fmt   = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private lateinit var db: AppDatabase
    private lateinit var session: SessionManager
    private lateinit var locationRepo: LocationRepository

    @Volatile private var usuarioId: String? = null
    private var jobLatido: Job? = null

    companion object {
        private const val TAG        = "TvSyncSvc"
        private const val CANAL      = "compasos_tv_sync"
        private const val NOTIF_ID   = 9101
        private const val EXTRA_USER = "usuario_id"

        /** Ventana para considerar a alguien "en línea" (5 min sin reportar = offline). */
        private const val VENTANA_EN_LINEA_MS = 5 * 60_000L

        /**
         * Referencia estática para poder empujar datos desde el repositorio sin
         * pasar el servicio por medio proyecto. Es null mientras el servicio no
         * esté vivo — por eso todos los helpers toleran null y nunca revientan.
         */
        @Volatile private var instancia: TvSyncService? = null

        fun iniciar(context: Context, userId: String? = null) {
            val intent = Intent(context, TvSyncService::class.java).apply {
                userId?.let { putExtra(EXTRA_USER, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, TvSyncService::class.java))
        }

        // ── API pública — llamar desde AlertaPhoneRepository ──────────────────

        /** Fuerza el snapshot completo ya mismo (tras vincular, tras editar familia). */
        fun sincronizarAhora() {
            val svc = instancia ?: return
            svc.scope.launch { svc.sincronizarTvs() }
        }

        /** Ubicación en vivo de un familiar → todas las TVs. */
        fun publicarUbicacion(
            usuarioId: String,
            latitud: Double,
            longitud: Double,
            fecha: String? = null
        ) {
            val svc = instancia ?: run {
                Log.w(TAG, "publicarUbicacion ignorada: TvSyncService no corre"); return
            }
            svc.scope.launch { svc.enviarUbicacion(usuarioId, latitud, longitud, fecha) }
        }

        /** Alerta de emergencia → todas las TVs. */
        fun publicarAlerta(
            alertaId: String,
            tipoAlerta: String,
            descripcion: String?,
            emisorId: String,
            emisorNombre: String?,
            latitud: Double?,
            longitud: Double?
        ) {
            val svc = instancia ?: run {
                Log.w(TAG, "publicarAlerta ignorada: TvSyncService no corre"); return
            }
            svc.scope.launch {
                val nombre = emisorNombre?.takeIf { it.isNotBlank() }
                    ?: svc.nombreDe(emisorId)

                val payload = JSONObject().apply {
                    put("alertaId",     alertaId)
                    put("tipoAlerta",   tipoAlerta)
                    put("descripcion",  descripcion ?: "")
                    put("emisorId",     emisorId)
                    put("emisorNombre", nombre)
                    latitud?.let  { put("latitud",  it) }
                    longitud?.let { put("longitud", it) }
                    put("fecha", svc.fmt.format(Date()))
                }.toString()

                // Sin retained: una alerta es un evento puntual.
                svc.paraCadaTv { tv ->
                    svc.mqtt.publicarSeguro(MqttConfig.topicTvAlerta(tv.id), payload)
                }
                // Y refresca el snapshot para que la ubicación del emisor
                // llegue al mapa sin esperar el latido.
                svc.sincronizarTvs()
            }
        }

        /** Notificación informativa → todas las TVs. */
        fun publicarNotificacion(
            notificacionId: String,
            alertaId: String?,
            titulo: String,
            mensaje: String,
            tipo: String = "info"
        ) {
            val svc = instancia ?: return
            svc.scope.launch {
                val payload = JSONObject().apply {
                    put("notificacionId", notificacionId)
                    put("alertaId",       alertaId ?: "")
                    put("titulo",         titulo)
                    put("mensaje",        mensaje)
                    put("tipo",           tipo)
                    put("fecha",          svc.fmt.format(Date()))
                }.toString()
                svc.paraCadaTv { tv ->
                    svc.mqtt.publicarSeguro(MqttConfig.topicTvNotificacion(tv.id), payload)
                }
            }
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        db           = AppDatabase.getInstance(applicationContext)
        session      = SessionManager(applicationContext)
        locationRepo = LocationRepository(applicationContext)
        // instancia se asigna DESPUÉS de db/session/repo: si se asigna antes,
        // una llamada que entre en ese microsegundo encontraría los lateinit
        // sin inicializar y reventaría.
        instancia    = this

        crearCanal()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID, notif(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIF_ID, notif())
        }

        usuarioId = session.obtenerUsuarioId()
        conectar()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nuevo = intent?.getStringExtra(EXTRA_USER) ?: session.obtenerUsuarioId()
        if (nuevo != null && nuevo != usuarioId) {
            usuarioId = nuevo
            Log.d(TAG, "userId actualizado: $nuevo")
            scope.launch { sincronizarTvs() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // Avísale a las TVs que este teléfono se va, en vez de dejarlas
        // mostrando datos viejos como si estuvieran vivos.
        try {
            runBlocking {
                withTimeoutOrNull(1500) {
                    paraCadaTv { tv ->
                        mqtt.publicarSeguro(
                            MqttConfig.topicTvEstadoTelefono(tv.id),
                            JSONObject().put("online", false).toString(),
                            retained = true
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        jobLatido?.cancel()
        scope.cancel()
        mqtt.desconectar()
        instancia = null
        super.onDestroy()
    }

    // ── Conexión ──────────────────────────────────────────────────────────────

    private fun conectar() {
        scope.launch {
            var intentos = 0
            while (isActive) {
                try {
                    mqtt.alConectar { reconectado ->
                        scope.launch {
                            anunciarPresencia()
                            sincronizarTvs()
                        }
                        if (reconectado) Log.d(TAG, "Reconectado — snapshot reenviado")
                    }

                    mqtt.conectar(clientId = "compasos_tvsync_${System.currentTimeMillis()}")

                    anunciarPresencia()
                    iniciarLatido()
                    Log.d(TAG, "✅ TvSyncService listo")
                    return@launch

                } catch (e: Exception) {
                    intentos++
                    Log.e(TAG, "Error conectando (intento $intentos): ${e.message}")
                    delay(minOf(5_000L * intentos, 30_000L))
                }
            }
        }
    }

    private fun iniciarLatido() {
        jobLatido?.cancel()
        jobLatido = scope.launch {
            // Limpieza única al arrancar: el historial crece rápido.
            runCatching {
                db.historialUbicacionDao().limpiarViejas(
                    fmt.format(Date(System.currentTimeMillis() - 24 * 60 * 60_000L))
                )
            }
            while (isActive) {
                try {
                    registrarUbicacionPropia()
                    sincronizarTvs()
                } catch (e: Exception) {
                    Log.e(TAG, "Error en latido: ${e.message}")
                }
                delay(MqttConfig.INTERVALO_LATIDO_MS)
            }
        }
    }

    // ── Publicación ───────────────────────────────────────────────────────────

    /** Snapshot completo a todas las TVs, retained. */
    private suspend fun sincronizarTvs() {
        val uid = usuarioId ?: session.obtenerUsuarioId() ?: return
        if (!mqtt.estaConectado) return

        val tvs = db.dispositivoDao().obtenerTvsVinculados(uid)
        if (tvs.isEmpty()) return

        val payload = construirSesion(uid) ?: return
        tvs.forEach { tv ->
            mqtt.publicarSeguro(MqttConfig.topicTvSesion(tv.id), payload, retained = true)
        }
        Log.d(TAG, "↻ Snapshot enviado a ${tvs.size} TV(s)")
    }

    private suspend fun enviarUbicacion(
        idUsuario: String, lat: Double, lng: Double, fecha: String?
    ) {
        val payload = JSONObject().apply {
            put("usuarioId", idUsuario)
            put("latitud",   lat)
            put("longitud",  lng)
            put("fecha",     fecha ?: fmt.format(Date()))
            put("enLinea",   true)
        }.toString()

        paraCadaTv { tv ->
            // retained por familiar: el broker guarda la última posición de
            // CADA uno, no solo la del último que se movió.
            mqtt.publicarSeguro(
                MqttConfig.topicTvUbicacion(tv.id, idUsuario), payload, retained = true
            )
        }
    }

    /** Guarda el GPS del propio teléfono y lo empuja a las TVs. */
    private suspend fun registrarUbicacionPropia() {
        val uid = usuarioId ?: return
        val loc = locationRepo.obtenerUltimaUbicacion() ?: return
        val ahora = fmt.format(Date())

        runCatching {
            db.historialUbicacionDao().insertar(
                HistorialUbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    usuarioId = uid,
                    latitud   = loc.latitud,
                    longitud  = loc.longitud,
                    fecha     = ahora
                )
            )
        }
        enviarUbicacion(uid, loc.latitud, loc.longitud, ahora)
    }

    private suspend fun anunciarPresencia() {
        val online = JSONObject().apply {
            put("online", true)
            put("ts", System.currentTimeMillis())
        }.toString()
        paraCadaTv { tv ->
            mqtt.publicarSeguro(
                MqttConfig.topicTvEstadoTelefono(tv.id), online, retained = true
            )
        }
    }

    private suspend fun paraCadaTv(bloque: (DispositivoEntity) -> Unit) {
        val uid = usuarioId ?: session.obtenerUsuarioId() ?: return
        if (!mqtt.estaConectado) return
        db.dispositivoDao().obtenerTvsVinculados(uid).forEach(bloque)
    }

    // ── Construcción del snapshot ─────────────────────────────────────────────

    /**
     * {
     *   "usuarioId":"u1","nombre":"Ana","email":"ana@x.com",
     *   "fecha":"2026-08-16 12:00:00",
     *   "familiares":[
     *     {"usuarioId","nombre","apellido","latitud","longitud","fecha","enLinea"}
     *   ]
     * }
     */
    private suspend fun construirSesion(uid: String): String? {
        return try {
            val usuario = db.usuarioDao().obtenerPorId(uid) ?: run {
                Log.w(TAG, "Usuario $uid todavía no está en Room"); return null
            }

            val familiares = JSONArray()

            // El dueño va primero: la TV lo quiere ver en el mapa también.
            familiares.put(jsonFamiliar(uid, usuario.nombre, usuario.apellidoPaterno, "Yo"))

            for (rel in db.familiaUsuarioDao().obtenerTodosFamiliares(uid)) {
                val u = db.usuarioDao().obtenerPorId(rel.usuarioId) ?: continue
                familiares.put(
                    jsonFamiliar(u.id, u.nombre, u.apellidoPaterno, rel.rol ?: "Miembro")
                )
            }

            JSONObject().apply {
                put("usuarioId",  usuario.id)
                put("nombre",     usuario.nombre)
                put("email",      usuario.correo)
                put("fecha",      fmt.format(Date()))
                put("familiares", familiares)
            }.toString()

        } catch (e: Exception) {
            Log.e(TAG, "Error construyendo snapshot: ${e.message}", e)
            null
        }
    }

    private suspend fun jsonFamiliar(
        id: String, nombre: String, apellido: String?, rol: String
    ): JSONObject {
        val ubi = db.historialUbicacionDao().obtenerUltimaDeUsuario(id)
        return JSONObject().apply {
            put("usuarioId", id)
            put("nombre",    nombre)
            put("apellido",  apellido ?: "")
            ubi?.latitud?.let  { put("latitud",  it) }
            ubi?.longitud?.let { put("longitud", it) }
            put("fecha",   ubi?.fecha ?: "")
            put("enLinea", esReciente(ubi?.fecha))
            put("rol",     rol)
        }
    }

    private suspend fun nombreDe(id: String): String {
        val u = db.usuarioDao().obtenerPorId(id) ?: return "Familiar"
        return listOfNotNull(u.nombre, u.apellidoPaterno).joinToString(" ").trim()
            .ifBlank { "Familiar" }
    }

    private fun esReciente(fecha: String?): Boolean {
        if (fecha.isNullOrBlank()) return false
        return try {
            val t = fmt.parse(fecha)?.time ?: return false
            System.currentTimeMillis() - t < VENTANA_EN_LINEA_MS
        } catch (_: Exception) { false }
    }

    // ── Notificación persistente ──────────────────────────────────────────────

    private fun notif() = NotificationCompat.Builder(this, CANAL)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("CompaSOS — TV")
        .setContentText("Enviando ubicaciones y alertas a la pantalla")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        CANAL, "Sincronización con TV",
                        NotificationManager.IMPORTANCE_LOW
                    )
                )
        }
    }
}