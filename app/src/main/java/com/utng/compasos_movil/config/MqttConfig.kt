package com.utng.compasos_movil.config

object MqttConfig {

    /**
     * ⚠️ ESTA IP DEBE SER IDÉNTICA A LA DEL MÓDULO TV.
     * Tenías 192.168.1.102 aquí y 192.168.1.5 en la TV: los dos se conectan
     * "bien" a brokers distintos y nunca se ven.
     */
    const val BROKER_URL = "tcp://192.168.1.102:1883"

    const val TIMEOUT_CONEXION = 10
    const val KEEP_ALIVE       = 60
    const val QOS              = 1

    const val TOPIC_VINCULACION = "compasos/vinculacion"
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"
    const val TOPIC_ALERTA      = "compasos/alerta"   // reloj — no se toca
    const val TOPIC_FAMILIA     = "compasos/familia"  // familiares — no se toca
    const val TOPIC_TV          = "compasos/tv"

    // ── Topics hacia la TV ────────────────────────────────────────────────────
    // La TV se suscribe a "compasos/tv/{tvId}/#", así que agregar un subtopic
    // nuevo aquí no obliga a tocar nada del otro lado.

    /** Snapshot completo: usuario + familiares con ubicación. RETAINED. */
    fun topicTvSesion(tvId: String) = "$TOPIC_TV/$tvId/sesion"

    /**
     * Ubicación de UN familiar. El usuarioId va EN EL TOPIC para que el broker
     * retenga la última posición de CADA uno. Con el topic compartido que tenías
     * solo podía retener la del último que se movió.
     */
    fun topicTvUbicacion(tvId: String, usuarioId: String) =
        "$TOPIC_TV/$tvId/ubicacion/$usuarioId"

    /** Alerta de emergencia. NO retained (es evento, no estado). */
    fun topicTvAlerta(tvId: String) = "$TOPIC_TV/$tvId/alerta"

    /** Notificación informativa. NO retained. */
    fun topicTvNotificacion(tvId: String) = "$TOPIC_TV/$tvId/notificacion"

    /** Presencia del teléfono. RETAINED + es su Last Will. */
    fun topicTvEstadoTelefono(tvId: String) = "$TOPIC_TV/$tvId/telefono_estado"

    /** Cada cuánto el teléfono manda el snapshot completo a las TVs. */
    const val INTERVALO_LATIDO_MS = 20_000L
}