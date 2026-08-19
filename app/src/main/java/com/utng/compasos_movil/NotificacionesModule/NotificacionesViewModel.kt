package com.utng.compasos_movil.NotificacionesModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * modelo de datos que combina la información de una notificación con su alerta correspondiente
 * y el nombre legible del destinatario.
 *
 * @property notificacion entidad [NotificacionEntity] que contiene los datos de la notificación.
 * @property alerta entidad [AlertaEntity] opcional vinculada a la notificación.
 * @property destinatarioNombre nombre legible del destinatario resuelto a partir de su identificador.
 */
data class NotificacionConAlerta(
    val notificacion:       NotificacionEntity,
    val alerta:             AlertaEntity?,
    val destinatarioNombre: String? = null   // ← nombre real en lugar del userId
)

/**
 * estado de la interfaz de usuario para la pantalla de notificaciones.
 *
 * @property cargando indica si la lista de notificaciones se encuentra en proceso de carga.
 * @property notificaciones lista de notificaciones combinadas con datos de alerta y destinatario.
 */
data class NotificacionesUiState(
    val cargando:       Boolean                    = true,
    val notificaciones: List<NotificacionConAlerta> = emptyList()
)

/**
 * viewmodel encargado de la gestión y consulta del historial de notificaciones del usuario,
 * vinculando las alertas asociadas y resolviendo nombres de destinatarios.
 *
 * @property notificacionDao acceso a los datos de notificaciones en la base de datos local.
 * @property alertaDao acceso a los datos de alertas en la base de datos local.
 * @property usuarioDao acceso a los datos de usuarios en la base de datos local.
 * @property sessionManager gestor de la sesión activa del usuario.
 */
class NotificacionesViewModel(
    private val notificacionDao: NotificacionDao,
    private val alertaDao:       AlertaDao,
    private val usuarioDao:      UsuarioDao,       // ← nuevo
    private val sessionManager:  SessionManager
) : ViewModel() {

    /**
     * flujo interno mutable para el estado de la interfaz de usuario.
     */
    private val _uiState = MutableStateFlow(NotificacionesUiState())

    /**
     * flujo observable público con el estado actual de la interfaz de usuario.
     */
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    init { cargar() }

    /**
     * consulta la lista de notificaciones del usuario en sesión desde la base de datos,
     * relacionando cada una con su alerta correspondiente y el nombre del destinatario.
     */
    private fun cargar() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId() ?: run {
                _uiState.update { it.copy(cargando = false) }
                return@launch
            }

            val notificaciones = notificacionDao.obtenerPorUsuario(userId)

            val items = notificaciones.map { notif ->
                val alerta = alertaDao.obtenerPorId(notif.alertaId)

                // Resuelve el userId del destinatario a nombre legible
                val nombre = notif.destinatario?.let { destId ->
                    val u = usuarioDao.obtenerPorId(destId)
                    if (u != null) "${u.nombre} ${u.apellidoPaterno ?: ""}".trim()
                    else null
                }

                NotificacionConAlerta(notif, alerta, nombre)
            }

            _uiState.update { it.copy(cargando = false, notificaciones = items) }
        }
    }
}

/**
 * fábrica de proveedores para instanciar [NotificacionesViewModel] inyectando sus dependencias necesarias.
 *
 * @property notificacionDao acceso a la entidad de notificaciones.
 * @property alertaDao acceso a la entidad de alertas.
 * @property usuarioDao acceso a la entidad de usuarios.
 * @property sessionManager gestor de la sesión del usuario.
 */
class NotificacionesViewModelFactory(
    private val notificacionDao: NotificacionDao,
    private val alertaDao:       AlertaDao,
    private val usuarioDao:      UsuarioDao,       // ← nuevo
    private val sessionManager:  SessionManager
) : ViewModelProvider.Factory {
    /**
     * crea una nueva instancia de la clase viewmodel requerida.
     *
     * @param modelClass la clase del viewmodel a instanciar.
     * @return una nueva instancia de [NotificacionesViewModel].
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificacionesViewModel(
            notificacionDao = notificacionDao,
            alertaDao       = alertaDao,
            usuarioDao      = usuarioDao,
            sessionManager  = sessionManager
        ) as T
}