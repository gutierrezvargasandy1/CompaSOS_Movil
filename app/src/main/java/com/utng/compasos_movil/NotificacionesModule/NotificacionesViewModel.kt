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

data class NotificacionConAlerta(
    val notificacion:       NotificacionEntity,
    val alerta:             AlertaEntity?,
    val destinatarioNombre: String? = null   // ← nombre real en lugar del userId
)

data class NotificacionesUiState(
    val cargando:       Boolean                    = true,
    val notificaciones: List<NotificacionConAlerta> = emptyList()
)

class NotificacionesViewModel(
    private val notificacionDao: NotificacionDao,
    private val alertaDao:       AlertaDao,
    private val usuarioDao:      UsuarioDao,       // ← nuevo
    private val sessionManager:  SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    init { cargar() }

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

class NotificacionesViewModelFactory(
    private val notificacionDao: NotificacionDao,
    private val alertaDao:       AlertaDao,
    private val usuarioDao:      UsuarioDao,       // ← nuevo
    private val sessionManager:  SessionManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificacionesViewModel(
            notificacionDao = notificacionDao,
            alertaDao       = alertaDao,
            usuarioDao      = usuarioDao,
            sessionManager  = sessionManager
        ) as T
}