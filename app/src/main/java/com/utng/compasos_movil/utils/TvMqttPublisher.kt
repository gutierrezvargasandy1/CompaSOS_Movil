package com.utng.compasos_movil.utils

import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * objeto singleton encargado de estructurar y publicar mensajes mqtt (ubicaciones, alertas y notificaciones)
 * dirigidos a las pantallas inteligentes compasos tv.
 */
object TvMqttPublisher {

    /** formateador de fecha utilizado para generar las marcas de tiempo de los eventos publicables. */
    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // ── Ruta recomendada ──────────────────────────────────────────────────────

    /**
     * objeto anidado que consolida las rutas de emisión globales para enviar eventos
     * a todas las pantallas tv vinculadas mediante [TvSyncService].
     */
    object aTodasLasTvs {

        /**
         * publica la ubicación geográfica actual de un usuario a todas las pantallas tv vinculadas.
         *
         * @param usuarioId identificador único del usuario.
         * @param latitud coordenada de latitud actual.
         * @param longitud coordenada de longitud actual.
         */
        fun ubicacion(usuarioId: String, latitud: Double, longitud: Double) =
            TvSyncService.publicarUbicacion(usuarioId, latitud, longitud)

        /**
         * publica un mensaje de alerta de emergencia hacia todas las pantallas tv vinculadas.
         *
         * @param alertaId identificador único de la alerta.
         * @param tipoAlerta clasificación o tipo de la alerta.
         * @param descripcion detalle o mensaje aclaratorio de la emergencia.
         * @param emisorId identificador del usuario que emite la alerta.
         * @param emisorNombre nombre opcional del usuario emisor.
         * @param latitud coordenada opcional de latitud.
         * @param longitud coordenada opcional de longitud.
         */
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

        /**
         * publica una notificación de estado o informativa a todas las pantallas tv vinculadas.
         *
         * @param notificacionId identificador único de la notificación.
         * @param alertaId identificador opcional de la alerta asociada.
         * @param titulo título de la notificación.
         * @param mensaje cuerpo o contenido descriptivo del mensaje.
         * @param tipo categoría o nivel de prioridad de la notificación.
         */
        fun notificacion(
            notificacionId: String = UUID.randomUUID().toString(),
            alertaId: String? = null,
            titulo: String,
            mensaje: String,
            tipo: String = "info"
        ) = TvSyncService.publicarNotificacion(notificacionId, alertaId, titulo, mensaje, tipo)

        /**
         * fuerza la sincronización e integración inmediata del snapshot completo de datos
         * (usuario y grupo familiar) hacia las pantallas tv.
         */
        fun sincronizar() = TvSyncService.sincronizarAhora()
    }

    // ── Ruta directa (tus firmas originales) ──────────────────────────────────

    /**
     * publica una alerta de emergencia dirigida a una pantalla tv específica mediante un [MqttManager].
     *
     * @param mqtt gestor de cliente mqtt para realizar el envío seguro.
     * @param tvId identificador de la pantalla tv de destino.
     * @param alertaId identificador único de la alerta generada.
     * @param tipoAlerta tipo o clasificación de la alerta.
     * @param descripcion mensaje detallado de la alerta.
     * @param emisorNombre nombre legible del emisor.
     * @param emisorId identificador único del emisor.
     * @param latitud posición en latitud del emisor.
     * @param longitud posición en longitud del emisor.
     */
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
     * envía la ubicación geográfica de un usuario a una pantalla tv específica especificando el canal del usuario.
     * el mensaje se envía con bandera 'retained' activa para mantener el último estado disponible en el broker.
     *
     * @param mqtt gestor de cliente mqtt para realizar la publicación.
     * @param tvId identificador de la pantalla tv destinataria.
     * @param usuarioId identificador del usuario cuya ubicación se transmite.
     * @param latitud coordenada de latitud.
     * @param longitud coordenada de longitud.
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

    /**
     * envía una notificación individual y directa a una pantalla tv específica.
     *
     * @param mqtt gestor de cliente mqtt encargado de transmitir el mensaje.
     * @param tvId identificador de la pantalla tv de destino.
     * @param notificacionId identificador único de la notificación.
     * @param alertaId identificador opcional de la alerta relacionada.
     * @param titulo título legible de la notificación.
     * @param mensaje texto explicativo o informativo.
     * @param tipo clasificación del tipo de mensaje emitido.
     */
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