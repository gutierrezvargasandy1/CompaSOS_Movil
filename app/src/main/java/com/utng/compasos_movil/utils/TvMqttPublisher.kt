package com.utng.compasos_movil.utils

import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object TvMqttPublisher {

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Envía una alerta de emergencia a una TV vinculada
     * Topic: compasos/tv/{tvId}/alerta
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
            put("alertaId", alertaId)
            put("tipoAlerta", tipoAlerta)
            put("descripcion", descripcion)
            put("emisorNombre", emisorNombre)
            put("emisorId", emisorId)
            latitud?.let { put("latitud", it) }
            longitud?.let { put("longitud", it) }
            put("fecha", fmt.format(Date()))
        }.toString()

        mqtt.publicar("${MqttConfig.TOPIC_TV}/$tvId/alerta", payload)
    }

    /**
     * Envía la ubicación en vivo de un familiar a la TV vinculada
     * Topic: compasos/tv/{tvId}/ubicacion
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
            put("latitud", latitud)
            put("longitud", longitud)
            put("fecha", fmt.format(Date()))
        }.toString()

        mqtt.publicar("${MqttConfig.TOPIC_TV}/$tvId/ubicacion", payload)
    }
}