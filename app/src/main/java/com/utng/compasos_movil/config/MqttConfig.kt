package com.utng.compasos_movil.config

/**
 * objeto singleton de configuración centralizada para el servicio mqtt.
 * contiene las constantes de conexión, tiempos de espera, niveles de qos
 * y las funciones generadoras de tópicos para la comunicación entre el teléfono,
 * el reloj inteligente y los dispositivos tv.
 */
object MqttConfig {

    /**
     * dirección url del servidor o broker mqtt (protocolo tcp y puerto 1883).
     * ⚠️ ESTA IP DEBE SER IDÉNTICA A LA DEL MÓDULO TV.
     * Tenías 192.168.1.102 aquí y 192.168.1.5 en la TV: los dos se conectan
     * "bien" a brokers distintos y nunca se ven.
     */
    const val BROKER_URL = "tcp://192.168.1.5:1883"

    /** tiempo límite de espera en segundos para establecer la conexión con el broker. */
    const val TIMEOUT_CONEXION = 10

    /** intervalo en segundos para el envío de paquetes keep-alive que mantienen la conexión activa. */
    const val KEEP_ALIVE       = 60

    /** nivel de calidad de servicio (quality of service): 1 garantiza la entrega al menos una vez. */
    const val QOS              = 1

    /** tópico base utilizado para los procesos de vinculación de nuevos dispositivos. */
    const val TOPIC_VINCULACION = "compasos/vinculacion"

    /** tópico base utilizado para el registro y gestión de información de dispositivos. */
    const val TOPIC_DISPOSITIVO = "compasos/dispositivo"

    /** tópico raíz para la recepción de señales de alerta enviadas desde el reloj inteligente. */
    const val TOPIC_ALERTA      = "compasos/alerta"   // reloj — no se toca

    /** tópico raíz para la comunicación y difusión de eventos entre miembros de la familia. */
    const val TOPIC_FAMILIA     = "compasos/familia"  // familiares — no se toca

    /** tópico raíz para la transmisión de información hacia las pantallas tv. */
    const val TOPIC_TV          = "compasos/tv"

    // ── Topics hacia la TV ────────────────────────────────────────────────────
    // La TV se suscribe a "compasos/tv/{tvId}/#", así que agregar un subtopic
    // nuevo aquí no obliga a tocar nada del otro lado.

    /**
     * genera la ruta del tópico para transmitir el snapshot completo de la sesión.
     * incluye los datos del usuario y de los familiares con sus ubicaciones. mensaje retenido (retained).
     *
     * @param tvId identificador único de la tv destinataria.
     * @return cadena con el tópico formateado "compasos/tv/{tvId}/sesion".
     */
    fun topicTvSesion(tvId: String) = "$TOPIC_TV/$tvId/sesion"

    /**
     * genera la ruta del tópico para enviar la posición geográfica específica de un usuario/familiar.
     * el `usuarioId` va dentro del tópico para permitir que el broker retenga la última posición
     * de cada integrante de manera independiente.
     *
     * @param tvId identificador único de la tv destinataria.
     * @param usuarioId identificador único del usuario del cual se reporta la ubicación.
     * @return cadena con el tópico formateado "compasos/tv/{tvId}/ubicacion/{usuarioId}".
     */
    fun topicTvUbicacion(tvId: String, usuarioId: String) =
        "$TOPIC_TV/$tvId/ubicacion/$usuarioId"

    /**
     * genera la ruta del tópico para enviar eventos instantáneos de alerta de emergencia a la tv.
     * no retenido por tratarse de un evento puntual.
     *
     * @param tvId identificador único de la tv destinataria.
     * @return cadena con el tópico formateado "compasos/tv/{tvId}/alerta".
     */
    fun topicTvAlerta(tvId: String) = "$TOPIC_TV/$tvId/alerta"

    /**
     * genera la ruta del tópico para transmitir notificaciones informativas breves hacia la tv.
     * no retenido.
     *
     * @param tvId identificador único de la tv destinataria.
     * @return cadena con el tópico formateado "compasos/tv/{tvId}/notificacion".
     */
    fun topicTvNotificacion(tvId: String) = "$TOPIC_TV/$tvId/notificacion"

    /**
     * genera la ruta del tópico para reportar el estado de presencia/conexión del dispositivo móvil.
     * mensaje retenido (retained) y configurado como la última voluntad (last will) del cliente.
     *
     * @param tvId identificador único de la tv destinataria.
     * @return cadena con el tópico formateado "compasos/tv/{tvId}/telefono_estado".
     */
    fun topicTvEstadoTelefono(tvId: String) = "$TOPIC_TV/$tvId/telefono_estado"

    /**
     * intervalo de tiempo en milisegundos para la emisión periódica del pulso de estado (heartbeat)
     * y actualización del snapshot hacia las pantallas tv.
     */
    const val INTERVALO_LATIDO_MS = 20_000L
}