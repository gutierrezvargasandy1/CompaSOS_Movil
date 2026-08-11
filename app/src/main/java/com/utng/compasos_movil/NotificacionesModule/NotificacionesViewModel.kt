package com.utng.compasos_movil.NotificacionesModule


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.dao.AlertaDao
import com.utng.compasos_movil.data.dao.NotificacionDao
import com.utng.compasos_movil.data.entity.AlertaEntity
import com.utng.compasos_movil.data.entity.NotificacionEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Movido aquí desde NotificacionesScreen para que AppNavigation ya no lo necesite
data class NotificacionConAlerta(
    val notificacion: NotificacionEntity,
    val alerta: AlertaEntity? = null
)

data class NotificacionesUiState(
    val cargando: Boolean = true,
    val notificaciones: List<NotificacionConAlerta> = emptyList(),
    val mensaje: String? = null
)

class NotificacionesViewModel(
    private val notificacionDao: NotificacionDao,
    private val alertaDao: AlertaDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificacionesUiState())
    val uiState: StateFlow<NotificacionesUiState> = _uiState.asStateFlow()

    init {
        cargarNotificaciones()
    }

    fun cargarNotificaciones() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId()
            if (userId == null) {
                _uiState.update { it.copy(cargando = false, mensaje = "No hay sesión activa") }
                return@launch
            }

            // Trae todas las notificaciones cuya alerta pertenece al usuario
            val notificaciones = notificacionDao.obtenerPorUsuario(userId)

            // Para cada notificación, carga su alerta relacionada
            val items = notificaciones.map { notif ->
                val alerta = alertaDao.obtenerPorId(notif.alertaId)
                NotificacionConAlerta(notif, alerta)
            }

            _uiState.update { it.copy(cargando = false, notificaciones = items) }
        }
    }
}

class NotificacionesViewModelFactory(
    private val notificacionDao: NotificacionDao,
    private val alertaDao: AlertaDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return NotificacionesViewModel(
            notificacionDao = notificacionDao,
            alertaDao       = alertaDao,
            sessionManager  = sessionManager
        ) as T
    }
}