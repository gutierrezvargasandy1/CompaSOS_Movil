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

data class DispositivoConUsuario(
    val dispositivo: DispositivoEntity,
    val usuario: UsuarioEntity? = null
)

sealed class EstadoVinculacion {
    object Inactivo : EstadoVinculacion()
    data class EsperandoWearOS(val codigo: String) : EstadoVinculacion()
    data class Exitosa(val nombreDispositivo: String) : EstadoVinculacion()
    data class Error(val mensaje: String) : EstadoVinculacion()
}

data class DispositivosUiState(
    val cargando: Boolean = true,
    val dispositivos: List<DispositivoConUsuario> = emptyList(),
    val estadoVinculacion: EstadoVinculacion = EstadoVinculacion.Inactivo,
    val mqttConectado: Boolean = false
)

// ============================================================
// VIEWMODEL
// ============================================================

class DispositivosViewModel(
    private val dispositivoDao: DispositivoDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    companion object {
        private const val TAG = "DispositivosVM"
        private const val TIMEOUT_VINCULACION_MS = 120_000L  // 2 minutos
    }

    private val mqtt = MqttManager()

    private val _uiState = MutableStateFlow(DispositivosUiState())
    val uiState: StateFlow<DispositivosUiState> = _uiState.asStateFlow()

    private var codigoActivo: String? = null
    private var timeoutJob: Job? = null

    init {
        cargarDispositivos()
        conectarMqtt()
    }

    // ============================================================
    // MQTT
    // ============================================================

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

    fun cancelarVinculacion() {
        val codigo = codigoActivo ?: return
        limpiarVinculacion(codigo)
        _uiState.update { it.copy(estadoVinculacion = EstadoVinculacion.Inactivo) }
    }

    fun cerrarDialogoVinculacion() {
        codigoActivo = null
        _uiState.update { it.copy(estadoVinculacion = EstadoVinculacion.Inactivo) }
    }

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

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) { mqtt.desconectar() }
    }
}

// ============================================================
// FACTORY
// ============================================================

class DispositivosViewModelFactory(
    private val dispositivoDao: DispositivoDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DispositivosViewModel(
            dispositivoDao = dispositivoDao,
            usuarioDao     = usuarioDao,
            sessionManager = sessionManager
        ) as T
    }
}