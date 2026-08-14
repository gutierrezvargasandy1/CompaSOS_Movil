package com.utng.compasos_movil.TvVinculacionModule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao   // ← NUEVO
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.DispositivoEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

sealed class EstadoVinculacionTv {
    object Inactivo : EstadoVinculacionTv()
    data class Generando(val codigo: String) : EstadoVinculacionTv()
    data class Exitosa(val modeloTv: String) : EstadoVinculacionTv()
    data class Error(val mensaje: String) : EstadoVinculacionTv()
}

class TvVinculacionViewModel(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,   // ← NUEVO
    private val sessionManager:    SessionManager
) : ViewModel() {

    companion object { private const val TAG = "TvVinculacionVM" }

    private val mqtt = MqttManager()
    private val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private val _estado = MutableStateFlow<EstadoVinculacionTv>(EstadoVinculacionTv.Inactivo)
    val estado: StateFlow<EstadoVinculacionTv> = _estado.asStateFlow()

    private var codigoActivo: String? = null

    fun iniciarVinculacion() {
        val codigo = generarCodigo()
        codigoActivo = codigo
        _estado.value = EstadoVinculacionTv.Generando(codigo)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!mqtt.estaConectado) mqtt.conectar()
                val topicSolicitud = "${MqttConfig.TOPIC_VINCULACION}/tv/$codigo/solicitud"
                mqtt.suscribir(topicSolicitud) { _, payload ->
                    viewModelScope.launch(Dispatchers.IO) {
                        procesarSolicitudTv(payload, codigo)
                    }
                }
                Log.d(TAG, "Esperando solicitud de la TV con código: $codigo")
            } catch (e: Exception) {
                Log.e(TAG, "Error al iniciar vinculación TV: ${e.message}")
                _estado.value = EstadoVinculacionTv.Error("Sin conexión al servidor MQTT")
            }
        }
    }

    private suspend fun procesarSolicitudTv(payload: String, codigoEsperado: String) {
        if (codigoActivo != codigoEsperado) return
        try {
            val json   = JSONObject(payload)
            val modelo = json.optString("modelo", "Android TV")
            val idTv   = json.optString("tvId",
                json.optString("dispositivoId", "tv_${UUID.randomUUID()}"))

            val userId = sessionManager.obtenerUsuarioId() ?: run {
                _estado.update { EstadoVinculacionTv.Error("Sin sesión activa") }
                return
            }
            val usuario = usuarioDao.obtenerPorId(userId)
            val nombre  = usuario?.nombre  ?: sessionManager.obtenerUsuarioNombre() ?: ""
            val email   = usuario?.correo  ?: sessionManager.obtenerUsuarioEmail()  ?: ""

            // 1. Responder a la TV con datos de sesión
            mqtt.publicar(
                "${MqttConfig.TOPIC_VINCULACION}/tv/$codigoEsperado/respuesta",
                JSONObject().apply {
                    put("usuarioId", userId)
                    put("nombre",    nombre)
                    put("email",     email)
                }.toString()
            )

            // 2. Guardar la TV en Room (teléfono)
            dispositivoDao.insertar(
                DispositivoEntity(
                    id               = idTv,
                    usuarioId        = userId,
                    tipo             = "tv",
                    modelo           = modelo,
                    fabricante       = json.optString("fabricante").ifBlank { null },
                    numeroSerie      = json.optString("numeroSerie").ifBlank { null },
                    tokenFcm         = null,
                    bateria          = null,
                    conectado        = true,
                    fechaVinculacion = fmt.format(Date())
                )
            )
            Log.d(TAG, "TV guardada en Room: $idTv")

            // 3. ── NUEVO: construir familiares reales ─────────────────────────
            val familiaresArray = JSONArray()

            // El propietario aparece primero — así el TV rastrea su posición en el mapa
            familiaresArray.put(JSONObject().apply {
                put("usuarioId", userId)
                put("nombre",    nombre)
                put("apellido",  "")
                put("enLinea",   true)
            })

            // Familiares registrados en la app
            val familiares = familiaUsuarioDao.obtenerTodosFamiliares(userId)
            for (f in familiares) {
                val datosFamiliar = usuarioDao.obtenerPorId(f.usuarioId) ?: continue
                familiaresArray.put(JSONObject().apply {
                    put("usuarioId", f.usuarioId)
                    put("nombre",    datosFamiliar.nombre ?: "")
                    put("apellido",  "")
                    put("enLinea",   false)
                })
            }
            Log.d(TAG, "Sesión con ${familiaresArray.length()} familiar(es)")

            // 4. Enviar sesión inicial a la TV
            mqtt.publicar(
                "${MqttConfig.TOPIC_TV}/$idTv/sesion",
                JSONObject().apply {
                    put("usuarioId",  userId)
                    put("nombre",     nombre)
                    put("email",      email)
                    put("familiares", familiaresArray)   // ← ahora con datos reales
                }.toString()
            )

            _estado.update { EstadoVinculacionTv.Exitosa(modelo) }
            Log.d(TAG, "✅ TV vinculada: $modelo")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando solicitud de TV: ${e.message}")
            _estado.update { EstadoVinculacionTv.Error("Error al procesar la solicitud de la TV") }
        }
    }

    fun reiniciar() {
        codigoActivo  = null
        _estado.value = EstadoVinculacionTv.Inactivo
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) { mqtt.desconectar() }
    }

    private fun generarCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

class TvVinculacionViewModelFactory(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,   // ← NUEVO
    private val sessionManager:    SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TvVinculacionViewModel(usuarioDao, dispositivoDao, familiaUsuarioDao, sessionManager) as T
}