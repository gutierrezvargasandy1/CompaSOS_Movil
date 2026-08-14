package com.utng.compasos_movil.config

object MqttConfig {
    const val BROKER_URL = "tcp://192.168.1.102:1883"

    const val TIMEOUT_CONEXION = 10
    const val KEEP_ALIVE       = 60
    const val QOS              = 1

    const val TOPIC_VINCULACION = "compasos/vinculacion"
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"
    const val TOPIC_ALERTA      = "compasos/alerta"   // ← NUEVO
    const val TOPIC_FAMILIA     = "compasos/familia"  // ← NUEVO
    const val TOPIC_TV          = "compasos/tv"          // ← Agregado para la TV
}