package com.utng.compasos_movil.AlertaPhoneRepository

import android.content.Context
import android.util.Log
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
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
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.data.entity.UbicacionEntity
import com.utng.compasos_movil.utils.SessionManager
import com.utng.compasos_movil.utils.TvMqttPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AlertaPhoneRepository(
    private val alertaDao:         AlertaDao,
    private val ubicacionDao:      UbicacionDao,
    private val audioDao:          AudioDao,
    private val notificacionDao:   NotificacionDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val dispositivoDao:    DispositivoDao,   // ← NUEVO
    private val sessionManager:    SessionManager,
    private val context:           Context
) {
    private val mqtt         = MqttManager()
    private val locationRepo = LocationRepository(context)
    private val fmt          = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── SOS ──────────────────────────────────────────────────────────────────

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
            }

            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud) // ← NUEVO
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando SOS: ${e.message}")
            null
        }
    }

    // ── Notificación a familiares ─────────────────────────────────────────────

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

            val payloadBase = JSONObject().apply {
                put("alertaId",      alerta.id)
                put("dispositivoId", alerta.dispositivoId)
                put("tipoAlerta",    alerta.tipoAlerta)
                put("descripcion",   alerta.descripcion)
                put("estado",        alerta.estado)
                put("fecha",         alerta.fecha)
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

    // ── NUEVO: Notificación a TVs vinculadas ──────────────────────────────────

    private suspend fun notificarTvs(
        alerta:    AlertaEntity,
        usuarioId: String,
        latitud:   Double?,
        longitud:  Double?
    ) {
        try {
            val tvs = dispositivoDao.obtenerTvsVinculados(usuarioId)
            if (tvs.isEmpty()) return

            if (!mqtt.estaConectado) mqtt.conectar()

            val nombre = sessionManager.obtenerUsuarioNombre() ?: ""

            for (tv in tvs) {
                TvMqttPublisher.enviarAlertaATv(
                    mqtt         = mqtt,
                    tvId         = tv.id,
                    alertaId     = alerta.id,
                    tipoAlerta   = alerta.tipoAlerta   ?: "SOS",                  // ← fix
                    descripcion  = alerta.descripcion ?: "Alerta de emergencia",
                    emisorNombre = nombre,
                    emisorId     = usuarioId,
                    latitud      = latitud,
                    longitud     = longitud
                )
                Log.d("AlertaPhoneRepo", "Alerta enviada a TV: ${tv.id} (${tv.modelo})")
            }
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error notificando TVs: ${e.message}")
        }
    }

    // ── Rastreo continuo de ubicación ─────────────────────────────────────────

    fun iniciarRastreoEnVivo(alertaId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
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
                        publicarUbicacionAFamiliares(alertaId, ubicacion)
                        publicarUbicacionATvs(ubicacion) // ← NUEVO
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
                put("alertaId", alertaId)
                put("tipo",     "ubicacion_viva")
                put("latitud",  ubicacion.latitud)
                put("longitud", ubicacion.longitud)
                put("fecha",    fmt.format(Date()))
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

    // ← NUEVO: publica ubicación del dueño del SOS a cada TV vinculada
    private suspend fun publicarUbicacionATvs(ubicacion: UbicacionActual) {
        try {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: return
            val tvs = dispositivoDao.obtenerTvsVinculados(usuarioId)
            if (tvs.isEmpty()) return

            if (!mqtt.estaConectado) mqtt.conectar()

            for (tv in tvs) {
                TvMqttPublisher.enviarUbicacionATv(
                    mqtt      = mqtt,
                    tvId      = tv.id,
                    usuarioId = usuarioId,
                    latitud   = ubicacion.latitud,
                    longitud  = ubicacion.longitud
                )
            }
            Log.d("AlertaPhoneRepo", "Ubicación en vivo enviada a ${tvs.size} TV(s)")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error publicando ubicación a TVs: ${e.message}")
        }
    }

    // ── Audio del reloj ───────────────────────────────────────────────────────

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

    // ── Alerta recibida como familiar ─────────────────────────────────────────

    suspend fun procesarAlertaFamiliar(payloadJson: String): AlertaEntity? =
        withContext(Dispatchers.IO) {
            try {
                val json      = JSONObject(payloadJson)
                val usuarioId = sessionManager.obtenerUsuarioId() ?: return@withContext null
                val alertaId  = json.optString("alertaId").ifBlank {
                    UUID.randomUUID().toString()
                }

                alertaDao.obtenerPorId(alertaId)?.let { return@withContext it }

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
                }
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

            ubicacionDao.insertar(
                UbicacionEntity(
                    id        = UUID.randomUUID().toString(),
                    alertaId  = alertaId,
                    latitud   = lat,
                    longitud  = lng,
                    precision = null,
                    velocidad = null,
                    fecha     = json.optString("fecha", fmt.format(Date()))
                )
            )
            Log.d("AlertaPhoneRepo", "Ubicación de familiar guardada para alerta: $alertaId")
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error procesando ubicación de familiar: ${e.message}")
        }
    }

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
            }
            notificarFamiliares(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud)
            notificarTvs(alerta, usuarioId, ubicacion?.latitud, ubicacion?.longitud) // ← NUEVO
            alerta
        } catch (e: Exception) {
            Log.e("AlertaPhoneRepo", "Error en crearSOSDesdeMovil: ${e.message}")
            null
        }
    }
}