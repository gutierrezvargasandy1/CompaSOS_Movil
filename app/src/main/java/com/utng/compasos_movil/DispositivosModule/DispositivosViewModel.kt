package com.utng.compasos_movil.DispositivosModule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// MODELOS DE ESTADO
// ============================================================

/**
 * modelo de datos que combina la información de un dispositivo con los datos del usuario asociado.
 *
 * @property dispositivo entidad [DispositivoEntity] que contiene los detalles del dispositivo.
 * @property usuario entidad [UsuarioEntity] opcional del usuario vinculado al dispositivo.
 */
data class DispositivoConUsuario(
    val dispositivo: DispositivoEntity,
    val usuario: UsuarioEntity? = null
)

/**
 * clase sellada que representa las distintas fases del proceso de vinculación de un dispositivo Wear OS.
 */
sealed class EstadoVinculacion {
    /**
     * estado que indica que no hay ningún proceso de vinculación en curso.
     */
    object Inactivo : EstadoVinculacion()

    /**
     * estado que indica la espera de la confirmación desde el dispositivo Wear OS con un código generado.
     *
     * @property codigo código alfanumérico generado para realizar el emparejamiento.
     */
    data class EsperandoWearOS(val codigo: String) : EstadoVinculacion()

    /**
     * estado que confirma la vinculación exitosa de un dispositivo.
     *
     * @property nombreDispositivo nombre o descripción del dispositivo vinculado.
     */
    data class Exitosa(val nombreDispositivo: String) : EstadoVinculacion()

    /**
     * estado que notifica un error durante el proceso de vinculación.
     *
     * @property mensaje descripción del fallo ocurrido.
     */
    data class Error(val mensaje: String) : EstadoVinculacion()
}

/**
 * estado de la interfaz de usuario para la pantalla de gestión de dispositivos.
 *
 * @property cargando indica si la lista de dispositivos está cargando información.
 * @property dispositivos lista de dispositivos vinculados con su información de usuario.
 * @property estadoVinculacion estado actual del proceso de emparejamiento.
 * @property mqttConectado estado de la conexión con el servicio broker mqtt.
 */
data class DispositivosUiState(
    val cargando: Boolean = true,
    val dispositivos: List<DispositivoConUsuario> = emptyList(),
    val estadoVinculacion: EstadoVinculacion = EstadoVinculacion.Inactivo,
    val mqttConectado: Boolean = false
)

// ============================================================
// VIEWMODEL
// ============================================================

/**
 * viewmodel responsable de gestionar la lógica de negocio para la administración de dispositivos,
 * el flujo de vinculación mediante mqtt y la actualización del estado de los dispositivos.
 *
 * @property dispositivoDao acceso a los datos de dispositivos en la base de datos local.
 * @property usuarioDao acceso a los datos del usuario en la base de datos local.
 * @property sessionManager gestor para obtener los datos de la sesión actual del usuario.
 */
class DispositivosViewModel(
    private val dispositivoDao: DispositivoDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        /**
         * etiqueta utilizada para los registros de log del viewmodel.
         */
        private const val TAG = "DispositivosVM"

        /**
         * tiempo máximo de espera para completar la vinculación en milisegundos (2 minutos).
         */
        private const val TIMEOUT_VINCULACION_MS = 120_000L  // 2 minutos
    }

    /**
     * gestor de conexión y suscripciones mqtt.
     */
    private val mqtt = MqttManager()

    /**
     * flujo interno mutable para el estado de la interfaz de usuario.
     */
    private val _uiState = MutableStateFlow(DispositivosUiState())

    /**
     * flujo observable público con el estado actual de la interfaz de usuario.
     */
    val uiState: StateFlow<DispositivosUiState> = _uiState.asStateFlow()

    /**
     * código de vinculación activo en el proceso actual.
     */
    private var codigoActivo: String? = null

    /**
     * trabajo en corrutina que controla el tiempo límite de la vinculación.
     */
    private var timeoutJob: Job? = null

    init {
        cargarDispositivos()
        conectarMqtt()
    }

    // ============================================================
    // MQTT
    // ============================================================

    /**
     * establece la conexión con el broker mqtt e inicia las suscripciones correspondientes.
     */
    private fun conectarMqtt() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mqtt.conectar()
                _uiState.update { it.copy(mqttConectado = true) }
                suscribirEstadoDispositivos()
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo conectar al broker: ${e.message}")
                _uiState.update { it.copy(mqttConectado = false) }
            }
        }
    }

    /** Escucha actualizaciones de batería y conexión de todos los dispositivos */
    private fun suscribirEstadoDispositivos() {
        // Wildcard: compasos/dispositivo/+/estado
        mqtt.suscribir("${MqttConfig.TOPIC_DISPOSITIVO}/+/estado") { topic, payload ->
            viewModelScope.launch(Dispatchers.IO) {
                actualizarEstadoDesdePayload(topic, payload)
            }
        }
    }

    /**
     * procesa el mensaje mqtt recibido sobre el estado de un dispositivo y actualiza la base de datos local.
     *
     * @param topic tópico mqtt en el que se recibió el mensaje.
     * @param payload cadena json con los datos del estado del dispositivo.
     */
    private suspend fun actualizarEstadoDesdePayload(topic: String, payload: String) {
        try {
            // Topic: compasos/dispositivo/{deviceId}/estado
            val deviceId = topic.split("/").getOrNull(2) ?: return
            val json = JSONObject(payload)
            val bateria   = if (json.has("bateria")) json.getInt("bateria") else null
            val conectado = json.optBoolean("conectado", true)

            dispositivoDao.actualizarEstado(deviceId, bateria, conectado)
            cargarDispositivos()
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar estado: ${e.message}")
        }
    }

    // ============================================================
    // ROOM
    // ============================================================

    /**
     * consulta y carga los dispositivos pertenecientes al usuario actual desde la base de datos.
     */
    fun cargarDispositivos() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId()
            if (userId == null) {
                _uiState.update { it.copy(cargando = false) }
                return@launch
            }

            val usuario     = usuarioDao.obtenerPorId(userId)
            val dispositivos = dispositivoDao.obtenerPorUsuario(userId)
            val items       = dispositivos.map { DispositivoConUsuario(it, usuario) }

            _uiState.update { it.copy(cargando = false, dispositivos = items) }
        }
    }

    // ============================================================
    // VINCULACIÓN WEAR OS
    // ============================================================

    /**
     * inicia el proceso de vinculación con un reloj Wear OS enviando una solicitud por mqtt y esperando respuesta.
     */
    fun iniciarVinculacion() {
        val userId = sessionManager.obtenerUsuarioId() ?: return
        val codigo = generarCodigo()
        codigoActivo = codigo

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!mqtt.estaConectado) mqtt.conectar()

                // Suscribirse ANTES de publicar para no perder la respuesta
                val topicRespuesta = "${MqttConfig.TOPIC_VINCULACION}/$codigo/respuesta"
                mqtt.suscribir(topicRespuesta) { _, payload ->
                    viewModelScope.launch(Dispatchers.IO) {
                        procesarRespuestaWearOS(payload, codigo)
                    }
                }

                // Publicar solicitud para que el Wear OS la muestre al usuario
                val solicitud = JSONObject().apply {
                    put("code", codigo)
                    put("usuarioId", userId)
                    put("timestamp", System.currentTimeMillis())
                }.toString()
                mqtt.publicar("${MqttConfig.TOPIC_VINCULACION}/$codigo/solicitud", solicitud)

                _uiState.update {
                    it.copy(estadoVinculacion = EstadoVinculacion.EsperandoWearOS(codigo))
                }
                iniciarTimeout(codigo)

            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar vinculación: ${e.message}")
                _uiState.update {
                    it.copy(estadoVinculacion = EstadoVinculacion.Error("Sin conexión al servidor MQTT"))
                }
            }
        }
    }

    /**
     * inicia el contador de tiempo límite para cancelar automáticamente el proceso de vinculación si no hay respuesta.
     *
     * @param codigo código de vinculación que se está validando.
     */
    private fun iniciarTimeout(codigo: String) {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            delay(TIMEOUT_VINCULACION_MS)
            if (_uiState.value.estadoVinculacion is EstadoVinculacion.EsperandoWearOS) {
                limpiarVinculacion(codigo)
                _uiState.update {
                    it.copy(
                        estadoVinculacion = EstadoVinculacion.Error(
                            "Tiempo de espera agotado. Intenta de nuevo."
                        )
                    )
                }
            }
        }
    }

    /**
     * Procesa la respuesta del Wear OS.
     *
     * Payload esperado (publicado por Wear OS):
     * {
     *   "deviceId":  "wear_abc123",
     *   "tipo":      "reloj",
     *   "modelo":    "Galaxy Watch 6",
     *   "fabricante":"Samsung",
     *   "bateria":   85
     * }
     *
     * @param payload datos de respuesta en formato json enviados por el dispositivo.
     * @param codigoEsperado código alfanumérico que debe coincidir con el proceso en curso.
     */
    private suspend fun procesarRespuestaWearOS(payload: String, codigoEsperado: String) {
        if (codigoActivo != codigoEsperado) return

        try {
            val json       = JSONObject(payload)
            val deviceId   = json.getString("deviceId")
            val tipo       = json.optString("tipo", "reloj")
            val modelo     = json.optString("modelo", "").takeIf { it.isNotBlank() }
            val fabricante = json.optString("fabricante", "").takeIf { it.isNotBlank() }
            val bateria    = if (json.has("bateria")) json.getInt("bateria") else null
            val userId     = sessionManager.obtenerUsuarioId() ?: return

            val fecha = SimpleDateFormat("dd MMM yyyy", Locale("es", "MX")).format(Date())

            dispositivoDao.insertar(
                DispositivoEntity(
                    id               = deviceId,
                    usuarioId        = userId,
                    tipo             = tipo,
                    modelo           = modelo,
                    fabricante       = fabricante,
                    numeroSerie      = null,
                    tokenFcm         = null,
                    bateria          = bateria,
                    conectado        = true,
                    fechaVinculacion = fecha
                )
            )

            val nombre = listOfNotNull(fabricante, modelo).joinToString(" ").ifBlank { tipo }
            limpiarVinculacion(codigoEsperado)
            _uiState.update {
                it.copy(estadoVinculacion = EstadoVinculacion.Exitosa(nombre))
            }
            cargarDispositivos()

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar respuesta Wear OS: ${e.message}")
            _uiState.update {
                it.copy(estadoVinculacion = EstadoVinculacion.Error("Respuesta inválida del dispositivo"))
            }
        }
    }

    /**
     * cancela el proceso de vinculación activo y limpia el estado actual.
     */
    fun cancelarVinculacion() {
        val codigo = codigoActivo ?: return
        limpiarVinculacion(codigo)
        _uiState.update { it.copy(estadoVinculacion = EstadoVinculacion.Inactivo) }
    }

    /**
     * cierra el diálogo de vinculación restableciendo el estado a inactivo.
     */
    fun cerrarDialogoVinculacion() {
        codigoActivo = null
        _uiState.update { it.copy(estadoVinculacion = EstadoVinculacion.Inactivo) }
    }

    /**
     * limpia las tareas temporales y quita la suscripción del canal mqtt de vinculación.
     *
     * @param codigo código de vinculación a desuscribir.
     */
    private fun limpiarVinculacion(codigo: String) {
        timeoutJob?.cancel()
        timeoutJob = null
        codigoActivo = null
        mqtt.desuscribir("${MqttConfig.TOPIC_VINCULACION}/$codigo/respuesta")
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /** Evita caracteres ambiguos: O/0, I/1 */
    private fun generarCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    /**
     * cancela corrutinas activas y desconecta el cliente mqtt al destruirse el viewmodel.
     */
    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { mqtt.desconectar() }
    }
}

// ============================================================
// FACTORY
// ============================================================

/**
 * fábrica de proveedores para instanciar [DispositivosViewModel] con sus dependencias necesarias.
 *
 * @property dispositivoDao acceso a la entidad de dispositivos en base de datos.
 * @property usuarioDao acceso a la entidad de usuarios en base de datos.
 * @property sessionManager administrador de la sesión de usuario activa.
 */
class DispositivosViewModelFactory(
    private val dispositivoDao: DispositivoDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    /**
     * crea una nueva instancia de la clase de viewmodel requerida.
     *
     * @param modelClass la clase del viewmodel a instanciar.
     * @return una nueva instancia de [DispositivosViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DispositivosViewModel(
            dispositivoDao = dispositivoDao,
            usuarioDao     = usuarioDao,
            sessionManager = sessionManager
        ) as T
    }
}