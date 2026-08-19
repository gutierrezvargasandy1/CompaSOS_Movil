package com.utng.compasos_movil.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * gestor principal del cliente mqtt encargado de administrar la conexión, publicación,
 * suscripción y re-suscripción automática de mensajes con el broker.
 *
 * Mantiene la misma API pública que tenías (conectar/publicar/suscribir/
 * desuscribir/desconectar/estaConectado), así que DispositivosViewModel,
 * TvVinculacionViewModel y AlertaPhoneRepository siguen compilando igual.
 *
 * Lo que se agregó:
 *
 * 1. RE-SUSCRIPCIÓN AUTOMÁTICA. Con isAutomaticReconnect + cleanSession=true,
 *    cuando parpadea el WiFi Paho reconecta pero el broker ya olvidó tus
 *    suscripciones: la app dice "conectado" y no vuelve a recibir nada.
 * 2. retained — para que la TV reciba el último snapshot al encender.
 * 3. Last Will — para que la TV sepa cuándo el teléfono se murió.
 * 4. publicarSeguro() — no revienta si el broker está caído.
 */
class MqttManager {

    companion object { private const val TAG = "MqttManager" }

    private var client: MqttClient? = null
    private val suscripciones = ConcurrentHashMap<String, (String, String) -> Unit>()
    private var onConexion: ((reconectado: Boolean) -> Unit)? = null

    /**
     * indica si el cliente mqtt se encuentra actualmente conectado al broker.
     */
    val estaConectado: Boolean
        get() = client?.isConnected == true

    /**
     * asigna una función de callback que se ejecutará al concretarse una conexión o reconexión exitosa.
     *
     * @param bloque lambda que recibe `true` si se trata de una reconexión automática o `false` si es la primera conexión.
     */
    fun alConectar(bloque: (reconectado: Boolean) -> Unit) { onConexion = bloque }

    /**
     * establece la conexión con el broker mqtt utilizando los parámetros especificados y configura los callbacks de eventos.
     * Llamar siempre desde Dispatchers.IO.
     *
     * @param clientId identificador único del cliente para el broker.
     * @param lwtTopic tópico opcional para el mensaje de última voluntad (last will and testament).
     * @param lwtPayload contenido del mensaje opcional de última voluntad.
     */
    @JvmOverloads
    fun conectar(
        clientId: String = "compasos_movil_${System.currentTimeMillis()}",
        lwtTopic: String? = null,
        lwtPayload: String? = null
    ) {
        if (estaConectado) return
        try {
            val c = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())

            c.setCallback(object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    Log.d(TAG, if (reconnect) "🔄 Reconectado" else "✅ Conectado a $serverURI")
                    // Este callback corre en el hilo interno de Paho y subscribe()
                    // es bloqueante: si lo llamo aquí directo puedo trabar ese hilo.
                    if (reconnect) Thread { reaplicarSuscripciones() }.start()
                    onConexion?.invoke(reconnect)
                }
                override fun connectionLost(cause: Throwable?) {
                    Log.w(TAG, "⚠️ Conexión perdida: ${cause?.message}")
                }
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            c.connect(MqttConnectOptions().apply {
                isCleanSession       = true
                connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
                keepAliveInterval    = MqttConfig.KEEP_ALIVE
                isAutomaticReconnect = true
                if (lwtTopic != null && lwtPayload != null) {
                    setWill(lwtTopic, lwtPayload.toByteArray(Charsets.UTF_8), 1, true)
                }
            })

            client = c
            Log.d(TAG, "Conectado a: ${MqttConfig.BROKER_URL} como $clientId")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al conectar: código=${e.reasonCode}, ${e.message}")
            throw e
        }
    }

    /**
     * re-suscribe de forma automática todos los tópicos almacenados en el mapa de [suscripciones]
     * tras una reconexión con el broker mqtt.
     */
    private fun reaplicarSuscripciones() {
        val c = client ?: return
        suscripciones.forEach { (topic, cb) ->
            try {
                c.subscribe(topic, MqttConfig.QOS) { t, msg ->
                    cb(t, String(msg.payload, Charsets.UTF_8))
                }
                Log.d(TAG, "↻ Re-suscrito a: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error re-suscribiendo a $topic: ${e.message}")
            }
        }
    }

    /**
     * desconecta de forma limpia el cliente del broker mqtt y vacía el mapa de suscripciones activas.
     */
    fun desconectar() {
        try {
            client?.takeIf { it.isConnected }?.disconnect()
            Log.d(TAG, "Desconectado del broker")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
        } finally {
            suscripciones.clear()
            client = null
        }
    }

    /**
     * publica un mensaje en un tópico específico del broker.
     *
     * @param topic canal o tópico donde se enviará el mensaje.
     * @param payload contenido en formato texto del mensaje.
     * @param qos nivel de calidad de servicio (por defecto el definido en [MqttConfig.QOS]).
     * @param retained true = el broker guarda el mensaje y se lo entrega a quien
     *        se suscriba después. Úsalo para ESTADO (sesión, última ubicación),
     *        nunca para EVENTOS (alertas), o la TV re-mostraría la misma alerta
     *        cada vez que reinicia.
     * @throws IllegalStateException si el cliente no se encuentra conectado.
     */
    @JvmOverloads
    fun publicar(
        topic: String,
        payload: String,
        qos: Int = MqttConfig.QOS,
        retained: Boolean = false
    ) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.publish(topic, MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
            this.qos = qos
            this.isRetained = retained
        })
        Log.d(TAG, "► [$topic]${if (retained) "(retained)" else ""}: $payload")
    }

    /**
     * intenta publicar un mensaje de forma segura capturando cualquier excepción para evitar fallos en la aplicación.
     *
     * @param topic canal o tópico de destino.
     * @param payload contenido en texto del mensaje.
     * @param qos nivel de calidad de servicio.
     * @param retained indica si el mensaje debe ser retenido por el broker.
     * @return true si la publicación fue exitosa, false si ocurrió una excepción.
     */
    @JvmOverloads
    fun publicarSeguro(
        topic: String, payload: String,
        qos: Int = MqttConfig.QOS, retained: Boolean = false
    ): Boolean = try {
        publicar(topic, payload, qos, retained); true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo publicar en $topic: ${e.message}"); false
    }

    /**
     * se suscribe a un tópico específico en el broker y registra la función callback para procesar los mensajes entrantes.
     *
     * @param topic canal o tópico al que se desea suscribir.
     * @param onMensaje lambda que se ejecutará al recibir un mensaje con el tópico y el payload recibidos.
     * @throws IllegalStateException si el cliente no se encuentra conectado.
     */
    fun suscribir(topic: String, onMensaje: (topic: String, payload: String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        suscripciones[topic] = onMensaje
        c.subscribe(topic, MqttConfig.QOS) { t, message ->
            val payload = String(message.payload, Charsets.UTF_8)
            Log.d(TAG, "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
        Log.d(TAG, "Suscrito a: $topic")
    }

    /**
     * cancela la suscripción a un tópico en el broker y remueve el callback correspondiente.
     *
     * @param topic canal o tópico del cual se desea cancelar la suscripción.
     */
    fun desuscribir(topic: String) {
        try {
            suscripciones.remove(topic)
            client?.unsubscribe(topic)
            Log.d(TAG, "Desuscrito de: $topic")
        } catch (e: MqttException) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }
}