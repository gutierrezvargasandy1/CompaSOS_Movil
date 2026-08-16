package com.utng.compasos_movil.utils

import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Se conservan tus firmas originales para no romper llamadas existentes,
 * pero ahora hay dos rutas:
 *
 *   • aTodasLasTvs.*   → RECOMENDADO. Delega en TvSyncService, que ya sabe
 *     qué TVs están vinculadas, aplica retained donde toca y no revienta si
 *     el broker está caído.
 *
 *   • enviarAlertaATv / enviarUbicacionATv → siguen existiendo, publican a UNA
 *     TV concreta con el MqttManager que tú le pases.
 */
object TvMqttPublisher {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── Ruta recomendada ──────────────────────────────────────────────────────

    object aTodasLasTvs {

        fun ubicacion(usuarioId: String, latitud: Double, longitud: Double) =
            TvSyncService.publicarUbicacion(usuarioId, latitud, longitud)

        fun alerta(
            alertaId: String,
            tipoAlerta: String,
            descripcion: String?,
            emisorId: String,
            emisorNombre: String? = null,
            latitud: Double? = null,
            longitud: Double? = null
        ) = TvSyncService.publicarAlerta(
            alertaId, tipoAlerta, descripcion, emisorId, emisorNombre, latitud, longitud
        )

        fun notificacion(
            notificacionId: String = UUID.randomUUID().toString(),
            alertaId: String? = null,
            titulo: String,
            mensaje: String,
            tipo: String = "info"
        ) = TvSyncService.publicarNotificacion(notificacionId, alertaId, titulo, mensaje, tipo)

        /** Fuerza el snapshot completo (usuario + familiares) ya mismo. */
        fun sincronizar() = TvSyncService.sincronizarAhora()
    }

    // ── Ruta directa (tus firmas originales) ──────────────────────────────────

    /** Topic: compasos/tv/{tvId}/alerta */
    fun enviarAlertaATv(
        mqtt: MqttManager,
        tvId: String,
        alertaId: String = UUID.randomUUID().toString(),
        tipoAlerta: String,
        descripcion: String,
        emisorNombre: String,
        emisorId: String,
        latitud: Double?,
        longitud: Double?
    ) {
        val payload = JSONObject().apply {
            put("alertaId",     alertaId)
            put("tipoAlerta",   tipoAlerta)
            put("descripcion",  descripcion)
            put("emisorNombre", emisorNombre)
            put("emisorId",     emisorId)
            latitud?.let  { put("latitud", it) }
            longitud?.let { put("longitud", it) }
            put("fecha", fmt.format(Date()))
        }.toString()

        // Sin retained: una alerta es un evento.
        mqtt.publicarSeguro(MqttConfig.topicTvAlerta(tvId), payload)
    }

    /**
     * Topic: compasos/tv/{tvId}/ubicacion/{usuarioId}
     *
     * ⚠️ CAMBIO: el usuarioId ahora va EN EL TOPIC. Antes todos compartían
     * `.../ubicacion`, así que con retained el broker solo podía guardar la
     * posición del último. La TV se suscribe con `#`, no tuvo que cambiar.
     */
    fun enviarUbicacionATv(
        mqtt: MqttManager,
        tvId: String,
        usuarioId: String,
        latitud: Double,
        longitud: Double
    ) {
        val payload = JSONObject().apply {
            put("usuarioId", usuarioId)
            put("latitud",   latitud)
            put("longitud",  longitud)
            put("fecha",     fmt.format(Date()))
            put("enLinea",   true)
        }.toString()

        mqtt.publicarSeguro(
            MqttConfig.topicTvUbicacion(tvId, usuarioId), payload, retained = true
        )
    }

    /** Topic: compasos/tv/{tvId}/notificacion */
    fun enviarNotificacionATv(
        mqtt: MqttManager,
        tvId: String,
        notificacionId: String = UUID.randomUUID().toString(),
        alertaId: String?,
        titulo: String,
        mensaje: String,
        tipo: String = "info"
    ) {
        val payload = JSONObject().apply {
            put("notificacionId", notificacionId)
            put("alertaId",       alertaId ?: "")
            put("titulo",         titulo)
            put("mensaje",        mensaje)
            put("tipo",           tipo)
            put("fecha",          fmt.format(Date()))
        }.toString()

        mqtt.publicarSeguro(MqttConfig.topicTvNotificacion(tvId), payload)
    }
}