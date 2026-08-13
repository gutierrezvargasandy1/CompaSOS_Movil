package com.utng.compasos_movil.HistorialModule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.UbicacionActual
import com.utng.compasos_movil.data.dao.HistorialUbicacionDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.wrapper.UsuarioConUbicacionesWrapper
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HistorialUbicacionesViewModel(
    application: Application,
    private val historialDao: HistorialUbicacionDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Wrapper completo con usuario + lista historial
    private val _state = MutableStateFlow<UsuarioConUbicacionesWrapper?>(null)
    val state: StateFlow<UsuarioConUbicacionesWrapper?> = _state.asStateFlow()

    // Última posición GPS — la pantalla la usa para mover la cámara
    private val _ubicacionActual = MutableStateFlow<UbicacionActual?>(null)
    val ubicacionActual: StateFlow<UbicacionActual?> = _ubicacionActual.asStateFlow()

    init {
        cargarDatosIniciales()
        iniciarSeguimientoYGuardado()
    }

    private fun cargarDatosIniciales() {
        viewModelScope.launch {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: return@launch
            val usuario = usuarioDao.obtenerPorId(usuarioId) ?: return@launch
            val historial = historialDao.obtenerPorUsuario(usuarioId)

            _state.value = UsuarioConUbicacionesWrapper(
                usuario = usuario,
                historialUbicaciones = historial
            )
        }
    }

    private fun iniciarSeguimientoYGuardado() {
        viewModelScope.launch {
            val usuarioId = sessionManager.obtenerUsuarioId() ?: return@launch

            locationRepository.ubicacionEnVivo().collect { ubicacion ->

                // 1. Actualiza la posición actual → la pantalla mueve la cámara
                _ubicacionActual.value = ubicacion

                // 2. Guarda en Room
                historialDao.insertar(
                    HistorialUbicacionEntity(
                        id = UUID.randomUUID().toString(),
                        usuarioId = usuarioId,
                        latitud = ubicacion.latitud,
                        longitud = ubicacion.longitud,
                        fecha = dateFormat.format(Date())
                    )
                )

                // 3. Recarga lista (respeta ORDER BY fecha DESC del DAO)
                val historialActualizado = historialDao.obtenerPorUsuario(usuarioId)
                _state.value = _state.value?.copy(
                    historialUbicaciones = historialActualizado
                )
            }
        }
    }

    // ── Factory ──────────────────────────────────────────────────────────────

    class Factory(
        private val application: Application,
        private val historialDao: HistorialUbicacionDao,
        private val usuarioDao: UsuarioDao,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistorialUbicacionesViewModel(
                application, historialDao, usuarioDao, sessionManager
            ) as T
    }
}