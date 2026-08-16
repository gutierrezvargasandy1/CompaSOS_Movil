package com.utng.compasos_movil.AlertaPhoneRepository

import android.content.Context
import android.util.Log
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.UbicacionActual
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.AudioDao
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.UbicacionDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.AudioEntity
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.data.entity.UbicacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * ⚠️ EL CONSTRUCTOR NO CAMBIÓ — AlertaMqttService y DashboardViewModel
 * siguen construyéndolo exactamente igual.
 *
 * El historialUbicacionDao lo saco de la instancia singleton de AppDatabase
 * usando el `context` que ya recibías, para no tocar las firmas.
 *
 * ⚠️ EL FLUJO DEL RELOJ NO CAMBIÓ: procesarSOS, procesarAudio,
 * iniciarRastreoEnVivo y los topics compasos/alerta y compasos/familia
 * hacen exactamente lo mismo que antes. Solo se AGREGAN llamadas a las TVs
 * y el guardado de ubicación por usuario.
 */
class AlertaPhoneRepository(
    private val alertaDao:         AlertaDao,
    private val ubicacionDao:      UbicacionDao,
    private val audioDao:          AudioDao,
    private val notificacionDao:   NotificacionDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val sessionManager:    SessionManager,
    private val context:           Context
) {
    private val mqtt         = MqttManager()
    private val locationRepo = LocationRepository(context)
    private val fmt          = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Sin cambiar el constructor: mismo singleton que ya usa el servicio.
    private val historialDao = AppDatabase.getInstance(context).historialUbicacionDao()

    // ── SOS (viene del reloj) — SIN CAMBIOS DE LÓGICA ────────────────────────

    suspend fun procesarSOS(payloadJson: String): AlertaEntity? = withContext(Dispatchers.IO) {
        try {
            val json      = JSONObject(payloadJson)
            val usuarioId = sessionManager.obtenerUsuarioId() ?: run {
                Log.e("AlertaPhoneRepo", "No hay sesión activa")
                return@withContext null
            }

            val alertaId = json.optString("alertaId").ifBlank { UUID.randomUUID().toString() }

            val alerta = AlertaEntity(
                id            = alertaId,
                usuarioId     = usuarioId,
                dispositivoId = json.optString("dispositivoId", "desconocido"),
                tipoAlerta    = json.optString("tipoAlerta",    "SOS"),
                descripcion   = json.optString("descripcion",   "Alerta de pánico"),
                estado        = "activa",
                fecha         = json.optString("fecha", fmt.format(Date()))
            )
            alertaDao.insertar(alerta)
            Log.d("AlertaPhoneRepo", "Alerta guardada en Room: $alertaId")

            val ubicacion = locationRepo.obtenerUltimaUbicacion()
            if (ubicacion != null) {
                ubicacionDao.insertar(
                    UbicacionEntity(
                        id        = UUID.randomUUID().toString(),
                        alertaId  = alertaId,
                        latitud   = ubicacion.latitud,
                        longitud  = ubicacion.longitud,
                        precision = null,
                        velocidad = null,
                        fecha     = fmt.format(Date())
                    )
                )
                // ← NUEVO: también al historial por usuario, que es lo que lee la TV
                guardarEnHistorial(usuarioId, ubicacion.latitud, ubicacion.longitud)
            }

            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando SOS: ${e.message}")
            null
        }
    }

    // ── Notificación a familiares (topic compasos/familia) ───────────────────

    private suspend fun notificarFamiliares(
        alerta:    AlertaEntity,
        usuarioId: String,
        latitud:   Double?,
        longitud:  Double?
    ) {
        try {
            val familiares = familiaUsuarioDao.obtenerTodosFamiliares(usuarioId)
            if (familiares.isEmpty()) {
                Log.d("AlertaPhoneRepo", "Sin familiares registrados para notificar")
                return
            }
            Log.d("AlertaPhoneRepo", "Notificando a ${familiares.size} familiar(es)")

            if (!mqtt.estaConectado) mqtt.conectar()

            val nombreEmisor = sessionManager.obtenerUsuarioNombre() ?: ""

            val payloadBase = JSONObject().apply {
                put("alertaId",      alerta.id)
                put("dispositivoId", alerta.dispositivoId)
                put("tipoAlerta",    alerta.tipoAlerta)
                put("descripcion",   alerta.descripcion)
                put("estado",        alerta.estado)
                put("fecha",         alerta.fecha)
                // ← NUEVO: sin esto, el receptor no puede saber DE QUIÉN es la
                //   ubicación que le llega después (la alerta se guarda con el
                //   usuarioId del receptor, no el del emisor).
                put("emisorId",      usuarioId)
                put("emisorNombre",  nombreEmisor)
                latitud?.let  { put("latitud",  it) }
                longitud?.let { put("longitud", it) }
            }.toString()

            for (familiar in familiares) {
                notificacionDao.insertar(
                    NotificacionEntity(
                        id           = UUID.randomUUID().toString(),
                        alertaId     = alerta.id,
                        destinatario = familiar.usuarioId,
                        tipo         = "SOS",
                        estado       = "enviada",
                        fecha        = fmt.format(Date())
                    )
                )
                mqtt.publicar(
                    "${MqttConfig.TOPIC_FAMILIA}/${familiar.usuarioId}/alerta",
                    payloadBase
                )
                Log.d("AlertaPhoneRepo", "Alerta enviada a familiar: ${familiar.usuarioId}")
            }
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error notificando familiares: ${e.message}")
        }
    }

    // ── Notificación a TVs vinculadas ─────────────────────────────────────────

    private suspend fun notificarTvs(
        alerta:    AlertaEntity,
        usuarioId: String,
        latitud:   Double?,
        longitud:  Double?
    ) {
        try {
            val nombre = sessionManager.obtenerUsuarioNombre() ?: ""

            // Ahora va por TvSyncService: una sola conexión, con reintento y
            // sin necesidad de resolver el tvId a mano.
            TvSyncService.publicarAlerta(
                alertaId     = alerta.id,
                tipoAlerta   = alerta.tipoAlerta ?: "SOS",
                descripcion  = alerta.descripcion ?: "Alerta de emergencia",
                emisorId     = usuarioId,
                emisorNombre = nombre,
                latitud      = latitud,
                longitud     = longitud
            )

            TvSyncService.publicarNotificacion(
                notificacionId = UUID.randomUUID().toString(),
                alertaId       = alerta.id,
                titulo         = "⚠️ ${alerta.tipoAlerta ?: "SOS"}",
                mensaje        = "$nombre activó una alerta de emergencia",
                tipo           = "sos"
            )
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error notificando TVs: ${e.message}")
        }
    }

    // ── Rastreo continuo de ubicación — SIN CAMBIOS DE LÓGICA ────────────────

    fun iniciarRastreoEnVivo(alertaId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            val usuarioId = sessionManager.obtenerUsuarioId()
            var contador = 0
            locationRepo.ubicacionEnVivo()
                .catch { e -> Log.e("AlertaPhoneRepo", "Error en rastreo: ${e.message}") }
                .collect { ubicacion ->
                    ubicacionDao.insertar(
                        UbicacionEntity(
                            id        = UUID.randomUUID().toString(),
                            alertaId  = alertaId,
                            latitud   = ubicacion.latitud,
                            longitud  = ubicacion.longitud,
                            precision = null,
                            velocidad = null,
                            fecha     = fmt.format(Date())
                        )
                    )
                    if (++contador % 3 == 0) {
                        usuarioId?.let {
                            guardarEnHistorial(it, ubicacion.latitud, ubicacion.longitud)
                        }
                        publicarUbicacionAFamiliares(alertaId, ubicacion)
                        publicarUbicacionATvs(ubicacion)
                    }
                }
        }
    }

    private suspend fun publicarUbicacionAFamiliares(
        alertaId: String,
        ubicacion: UbicacionActual
    ) {
        try {
            val usuarioId  = sessionManager.obtenerUsuarioId() ?: return
            val familiares = familiaUsuarioDao.obtenerTodosFamiliares(usuarioId)
            if (familiares.isEmpty()) return

            if (!mqtt.estaConectado) mqtt.conectar()

            val payload = JSONObject().apply {
                put("alertaId",  alertaId)
                put("tipo",      "ubicacion_viva")
                put("usuarioId", usuarioId)   // ← NUEVO: identifica al emisor
                put("emisorId",  usuarioId)   // ← alias, por compatibilidad
                put("latitud",   ubicacion.latitud)
                put("longitud",  ubicacion.longitud)
                put("fecha",     fmt.format(Date()))
            }.toString()

            for (familiar in familiares) {
                mqtt.publicar(
                    "${MqttConfig.TOPIC_FAMILIA}/${familiar.usuarioId}/ubicacion",
                    payload
                )
            }
            Log.d("AlertaPhoneRepo", "Ubicación en vivo publicada a ${familiares.size} familiar(es)")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error publicando ubicación a familiares: ${e.message}")
        }
    }

    private suspend fun publicarUbicacionATvs(ubicacion: UbicacionActual) {
        try {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: return
            TvSyncService.publicarUbicacion(usuarioId, ubicacion.latitud, ubicacion.longitud)
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error publicando ubicación a TVs: ${e.message}")
        }
    }

    // ── Audio del reloj — SIN CAMBIOS ────────────────────────────────────────

    suspend fun procesarAudio(payloadJson: String) = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(payloadJson)
            audioDao.insertar(
                AudioEntity(
                    id       = json.optString("id", UUID.randomUUID().toString()),
                    alertaId = json.getString("alertaId"),
                    urlAudio = json.optString("audio"),
                    duracion = null,
                    fecha    = json.optString("fecha", fmt.format(Date()))
                )
            )
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando audio: ${e.message}")
        }
    }

    // ── Alerta recibida como familiar ────────────────────────────────────────

    suspend fun procesarAlertaFamiliar(payloadJson: String): AlertaEntity? =
        withContext(Dispatchers.IO) {
            try {
                val json      = JSONObject(payloadJson)
                val usuarioId = sessionManager.obtenerUsuarioId() ?: return@withContext null
                val alertaId  = json.optString("alertaId").ifBlank {
                    UUID.randomUUID().toString()
                }

                alertaDao.obtenerPorId(alertaId)?.let { return@withContext it }

                val emisorId     = json.optString("emisorId")
                val emisorNombre = json.optString("emisorNombre")

                val alerta = AlertaEntity(
                    id            = alertaId,
                    usuarioId     = usuarioId,
                    dispositivoId = json.optString("dispositivoId"),
                    tipoAlerta    = json.optString("tipoAlerta", "SOS"),
                    descripcion   = json.optString("descripcion"),
                    estado        = json.optString("estado", "activa"),
                    fecha         = json.optString("fecha", fmt.format(Date()))
                )
                alertaDao.insertar(alerta)

                notificacionDao.insertar(
                    NotificacionEntity(
                        id           = UUID.randomUUID().toString(),
                        alertaId     = alertaId,
                        destinatario = usuarioId,
                        tipo         = "recibida",
                        estado       = "recibida",
                        fecha        = fmt.format(Date())
                    )
                )

                val lat = json.optDouble("latitud")
                val lng = json.optDouble("longitud")
                if (!lat.isNaN() && !lng.isNaN()) {
                    ubicacionDao.insertar(
                        UbicacionEntity(
                            id        = UUID.randomUUID().toString(),
                            alertaId  = alertaId,
                            latitud   = lat,
                            longitud  = lng,
                            precision = null,
                            velocidad = null,
                            fecha     = fmt.format(Date())
                        )
                    )
                    // ← NUEVO: la ubicación del EMISOR, indexada por su usuarioId,
                    //   para que la TV pueda pintarlo en el mapa
                    if (emisorId.isNotBlank()) guardarEnHistorial(emisorId, lat, lng)
                }

                // ← NUEVO: reenviar a las TVs
                TvSyncService.publicarAlerta(
                    alertaId     = alertaId,
                    tipoAlerta   = alerta.tipoAlerta ?: "SOS",
                    descripcion  = alerta.descripcion,
                    emisorId     = emisorId.ifBlank { usuarioId },
                    emisorNombre = emisorNombre.ifBlank { null },
                    latitud      = lat.takeIf { !it.isNaN() },
                    longitud     = lng.takeIf { !it.isNaN() }
                )
                TvSyncService.publicarNotificacion(
                    notificacionId = UUID.randomUUID().toString(),
                    alertaId       = alertaId,
                    titulo         = "⚠️ Un familiar necesita ayuda",
                    mensaje        = emisorNombre.ifBlank { "Un familiar" } +
                            " activó su alerta de emergencia",
                    tipo           = "sos"
                )

                alerta
            } catch (e: Exception) {
                Log.e("AlertaPhoneRepo", "Error procesando alerta de familiar: ${e.message}")
                null
            }
        }

    suspend fun procesarUbicacionFamiliar(payloadJson: String) = withContext(Dispatchers.IO) {
        try {
            val json     = JSONObject(payloadJson)
            val alertaId = json.optString("alertaId")
            if (alertaId.isBlank()) return@withContext

            val lat = json.optDouble("latitud")
            val lng = json.optDouble("longitud")
            if (lat.isNaN() || lng.isNaN()) return@withContext

            val fecha = json.optString("fecha", fmt.format(Date()))

            ubicacionDao.insertar(
                UbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    alertaId  = alertaId,
                    latitud   = lat,
                    longitud  = lng,
                    precision = null,
                    velocidad = null,
                    fecha     = fecha
                )
            )

            // ← NUEVO: guardar por usuarioId y empujar a la TV.
            //   Este es el camino que hace que la ubicación EN VIVO de un
            //   familiar llegue a la pantalla, no solo la del dueño.
            val emisorId = json.optString("usuarioId")
                .ifBlank { json.optString("emisorId") }

            if (emisorId.isNotBlank()) {
                guardarEnHistorial(emisorId, lat, lng, fecha)
                TvSyncService.publicarUbicacion(emisorId, lat, lng, fecha)
            } else {
                Log.w("AlertaPhoneRepo",
                    "Ubicación de familiar sin usuarioId — no se puede mandar a la TV")
            }

            Log.d("AlertaPhoneRepo", "Ubicación de familiar guardada para alerta: $alertaId")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando ubicación de familiar: ${e.message}")
        }
    }

    // ── SOS desde el propio móvil — SIN CAMBIOS DE LÓGICA ────────────────────

    suspend fun crearSOSDesdeMovil(): AlertaEntity? = withContext(Dispatchers.IO) {
        try {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: run {
                Log.e("AlertaPhoneRepo", "crearSOSDesdeMovil: sin sesión activa")
                return@withContext null
            }
            val alertaId      = UUID.randomUUID().toString()
            val dispositivoId = "movil_$usuarioId"

            val alerta = AlertaEntity(
                id            = alertaId,
                usuarioId     = usuarioId,
                dispositivoId = dispositivoId,
                tipoAlerta    = "SOS",
                descripcion   = "Alerta de pánico desde el teléfono",
                estado        = "activa",
                fecha         = fmt.format(Date())
            )
            alertaDao.insertar(alerta)

            val ubicacion = locationRepo.obtenerUltimaUbicacion()
            if (ubicacion != null) {
                ubicacionDao.insertar(
                    UbicacionEntity(
                        id        = UUID.randomUUID().toString(),
                        alertaId  = alertaId,
                        latitud   = ubicacion.latitud,
                        longitud  = ubicacion.longitud,
                        precision = null,
                        velocidad = null,
                        fecha     = fmt.format(Date())
                    )
                )
                guardarEnHistorial(usuarioId, ubicacion.latitud, ubicacion.longitud)
            }
            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error en crearSOSDesdeMovil: ${e.message}")
            null
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * `historial_ubicacion` es la única tabla indexada por usuarioId, así que
     * es la fuente de "última ubicación conocida de cada persona" que arma el
     * snapshot para la TV.
     */
    private suspend fun guardarEnHistorial(
        usuarioId: String, lat: Double, lng: Double, fecha: String = fmt.format(Date())
    ) {
        runCatching {
            historialDao.insertar(
                HistorialUbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    usuarioId = usuarioId,
                    latitud   = lat,
                    longitud  = lng,
                    fecha     = fecha
                )
            )
        }.onFailure {
            Log.e("AlertaPhoneRepo", "No se pudo guardar en historial: ${it.message}")
        }
    }
}