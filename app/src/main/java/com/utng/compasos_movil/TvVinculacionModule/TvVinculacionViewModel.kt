package com.utng.compasos_movil.TvVinculacionModule

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.config.MqttConfig
import com.utng.compasos_movil.config.MqttManager
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.dao.DispositivoDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
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

/**
 * representa los distintos estados posibles durante el proceso de vinculación con una android tv.
 */
sealed class EstadoVinculacionTv {
    /**
     * estado inicial inactivo antes de iniciar el proceso de vinculación.
     */
    object Inactivo : EstadoVinculacionTv()

    /**
     * estado activo en el que se ha generado un código de vinculación y se espera la respuesta de la tv.
     *
     * @property codigo código alfanumérico generado para que el usuario ingrese en la tv.
     */
    data class Generando(val codigo: String) : EstadoVinculacionTv()

    /**
     * estado final de vinculación exitosa.
     *
     * @property modeloTv nombre o modelo del dispositivo tv vinculado.
     */
    data class Exitosa(val modeloTv: String) : EstadoVinculacionTv()

    /**
     * estado de fallo durante el flujo de vinculación.
     *
     * @property mensaje mensaje explicativo del error ocurrido.
     */
    data class Error(val mensaje: String) : EstadoVinculacionTv()
}

/**
 * Cambios respecto a tu versión:
 *
 * 1. La sesión inicial ahora incluye la ÚLTIMA UBICACIÓN de cada familiar
 *    (sacada de historial_ubicacion). Antes mandabas nombre + enLinea=false y
 *    nada más, por eso la TV pintaba a todos "sin ubicación" para siempre.
 *
 * 2. Al terminar de vincular llama a TvSyncService.sincronizarAhora(), para que
 *    la pantalla reciba el snapshot completo de inmediato en vez de esperar
 *    al siguiente latido.
 *
 * 3. onCleared() ya NO desconecta ciegamente: se desuscribe del topic de
 *    solicitud primero. Antes, al salir de la pantalla, matabas la conexión
 *    y con ella cualquier suscripción viva.
 */
/**
 * viewmodel encargado de la generación de códigos de vinculación y sincronización de datos con dispositivos android tv vía mqtt.
 *
 * @property usuarioDao acceso a los datos de usuarios en la base de datos local.
 * @property dispositivoDao acceso a los datos de dispositivos vinculados.
 * @property familiaUsuarioDao acceso a la relación entre familiares y usuarios.
 * @property historialDao acceso al historial de ubicaciones para consultar la última posición conocida.
 * @property sessionManager gestor de la sesión activa del usuario.
 */
class TvVinculacionViewModel(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val historialDao:      HistorialUbicacionDao,   // ← NUEVO
    private val sessionManager:    SessionManager
) : ViewModel() {

    companion object {
        /**
         * etiqueta utilizada para los registros de log en cat.
         */
        private const val TAG = "TvVinculacionVM"
    }

    /**
     * gestor de conexión y mensajería mqtt.
     */
    private val mqtt = MqttManager()

    /**
     * formateador de fechas para los registros transmitidos a la tv.
     */
    private val fmt  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * flujo interno mutable que contiene el estado actual del proceso de vinculación.
     */
    private val _estado = MutableStateFlow<EstadoVinculacionTv>(EstadoVinculacionTv.Inactivo)

    /**
     * flujo observable público del estado de vinculación.
     */
    val estado: StateFlow<EstadoVinculacionTv> = _estado.asStateFlow()

    /**
     * código alfanumérico temporal generado para la sesión de vinculación activa.
     */
    private var codigoActivo: String? = null

    /**
     * genera un nuevo código alfanumérico, establece la conexión con el broker mqtt y se suscribe
     * al tópico de solicitudes de vinculación para esperar la respuesta de la tv.
     */
    fun iniciarVinculacion() {
        val codigo = generarCodigo()
        codigoActivo = codigo
        _estado.value = EstadoVinculacionTv.Generando(codigo)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!mqtt.estaConectado) {
                    mqtt.conectar(clientId = "compasos_vinc_${System.currentTimeMillis()}")
                }
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

    /**
     * procesa la solicitud recibida desde la tv, responde con la información del usuario,
     * registra el dispositivo en la base de datos local y transmite la lista de familiares con sus ubicaciones.
     *
     * @param payload contenido json de la solicitud enviada por la tv.
     * @param codigoEsperado código de vinculación con el que se debe validar la recepción.
     */
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
            val nombre  = usuario?.nombre ?: sessionManager.obtenerUsuarioNombre() ?: ""
            val email   = usuario?.correo ?: sessionManager.obtenerUsuarioEmail()  ?: ""

            // 1. Responder a la TV con los datos de sesión
            mqtt.publicar(
                "${MqttConfig.TOPIC_VINCULACION}/tv/$codigoEsperado/respuesta",
                JSONObject().apply {
                    put("usuarioId", userId)
                    put("nombre",    nombre)
                    put("email",     email)
                    put("tvId",      idTv)
                    put("aceptada",  true)
                }.toString()
            )

            // 2. Guardar la TV en Room — TvSyncService la lee de aquí
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

            // 3. Sesión inicial CON ubicaciones reales
            val familiaresArray = JSONArray()
            familiaresArray.put(jsonFamiliar(userId, nombre, usuario?.apellidoPaterno, "Yo"))

            for (rel in familiaUsuarioDao.obtenerTodosFamiliares(userId)) {
                val u = usuarioDao.obtenerPorId(rel.usuarioId) ?: continue
                familiaresArray.put(
                    jsonFamiliar(u.id, u.nombre, u.apellidoPaterno, rel.rol ?: "Miembro")
                )
            }
            Log.d(TAG, "Sesión con ${familiaresArray.length()} familiar(es)")

            mqtt.publicar(
                MqttConfig.topicTvSesion(idTv),
                JSONObject().apply {
                    put("usuarioId",  userId)
                    put("nombre",     nombre)
                    put("email",      email)
                    put("fecha",      fmt.format(Date()))
                    put("familiares", familiaresArray)
                }.toString(),
                retained = true
            )

            // 4. Que el servicio empuje el snapshot completo ya mismo
            TvSyncService.sincronizarAhora()

            _estado.update { EstadoVinculacionTv.Exitosa(modelo) }
            Log.d(TAG, "✅ TV vinculada: $modelo")
        } catch (e: Exception) {
            Log.e(TAG, "Error procesando solicitud de TV: ${e.message}", e)
            _estado.update { EstadoVinculacionTv.Error("Error al procesar la solicitud de la TV") }
        }
    }

    /**
     * construye un objeto json con la información personal, rol y última ubicación registrada de un familiar.
     *
     * @param id identificador del usuario o familiar.
     * @param nombre nombre del familiar.
     * @param apellido apellido paterno del familiar.
     * @param rol rol o parentesco que desempeña en el grupo familiar.
     * @return [JSONObject] estructurado con los datos del familiar.
     */
    private suspend fun jsonFamiliar(
        id: String, nombre: String, apellido: String?, rol: String
    ): JSONObject {
        val ubi = historialDao.obtenerUltimaDeUsuario(id)
        return JSONObject().apply {
            put("usuarioId", id)
            put("nombre",    nombre)
            put("apellido",  apellido ?: "")
            ubi?.latitud?.let  { put("latitud",  it) }
            ubi?.longitud?.let { put("longitud", it) }
            put("fecha",   ubi?.fecha ?: "")
            put("enLinea", ubi != null)
            put("rol",     rol)
        }
    }

    /**
     * cancela la suscripción mqtt activa, limpia el código generado y reinicia el estado a inactivo.
     */
    fun reiniciar() {
        codigoActivo?.let { cod ->
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    mqtt.desuscribir("${MqttConfig.TOPIC_VINCULACION}/tv/$cod/solicitud")
                }
            }
        }
        codigoActivo  = null
        _estado.value = EstadoVinculacionTv.Inactivo
    }

    /**
     * libera los recursos y desconecta el cliente mqtt al destruir la instancia del viewmodel.
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) { mqtt.desconectar() }
    }

    /**
     * genera una cadena alfanumérica aleatoria de 6 caracteres para ser utilizada como código de vinculación.
     *
     * @return código alfanumérico generado.
     */
    private fun generarCodigo(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}

/**
 * fábrica de proveedores para instanciar [TvVinculacionViewModel] inyectando sus dependencias necesarias.
 *
 * @property usuarioDao acceso a los datos de usuarios.
 * @property dispositivoDao acceso a la entidad de dispositivos.
 * @property familiaUsuarioDao acceso a la relación de familiares.
 * @property historialDao acceso al historial de ubicaciones.
 * @property sessionManager gestor de sesión del usuario.
 */
class TvVinculacionViewModelFactory(
    private val usuarioDao:        UsuarioDao,
    private val dispositivoDao:    DispositivoDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val historialDao:      HistorialUbicacionDao,   // ← NUEVO
    private val sessionManager:    SessionManager
) : ViewModelProvider.Factory {
    /**
     * crea una nueva instancia de la clase viewmodel requerida.
     *
     * @param modelClass la clase del viewmodel a instanciar.
     * @return una nueva instancia de [TvVinculacionViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        TvVinculacionViewModel(
            usuarioDao, dispositivoDao, familiaUsuarioDao, historialDao, sessionManager
        ) as T
}