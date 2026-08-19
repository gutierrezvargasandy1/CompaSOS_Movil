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

/**
 * viewmodel encargado de la gestión del historial de ubicaciones del usuario y la captura
 * en tiempo real de la posición gps actual para actualizar el mapa y almacenar el registro.
 *
 * @property application contexto de la aplicación android necesario para servicios de ubicación.
 * @property historialDao acceso a los datos del historial de ubicaciones en la base de datos local.
 * @property usuarioDao acceso a los datos de los usuarios en la base de datos local.
 * @property sessionManager gestor de la sesión activa del usuario.
 */
class HistorialUbicacionesViewModel(
    application: Application,
    private val historialDao: HistorialUbicacionDao,
    private val usuarioDao: UsuarioDao,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    /**
     * repositorio encargado de la obtención y seguimiento de lecturas gps en tiempo real.
     */
    private val locationRepository = LocationRepository(application)

    /**
     * formateador de fecha para dar formato estándar a los registros del historial de ubicación.
     */
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Wrapper completo con usuario + lista historial
    /**
     * flujo interno mutable del wrapper con la información del usuario y su historial de ubicaciones.
     */
    private val _state = MutableStateFlow<UsuarioConUbicacionesWrapper?>(null)

    /**
     * flujo observable público del wrapper con los datos del usuario y su historial.
     */
    val state: StateFlow<UsuarioConUbicacionesWrapper?> = _state.asStateFlow()

    // Última posición GPS — la pantalla la usa para mover la cámara
    /**
     * flujo interno mutable de la posición gps actual en tiempo real.
     */
    private val _ubicacionActual = MutableStateFlow<UbicacionActual?>(null)

    /**
     * flujo observable público de la última posición gps registrada.
     */
    val ubicacionActual: StateFlow<UbicacionActual?> = _ubicacionActual.asStateFlow()

    init {
        cargarDatosIniciales()
        iniciarSeguimientoYGuardado()
    }

    /**
     * obtiene de la base de datos local los datos del usuario en sesión y su historial inicial de ubicaciones.
     */
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

    /**
     * inicia la recolección continua de posiciones gps, persiste las nuevas coordenadas en la base de datos
     * y actualiza el estado de la interfaz de usuario.
     */
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

    /**
     * fábrica de proveedores para instanciar [HistorialUbicacionesViewModel] inyectando sus dependencias requeridas.
     *
     * @property application contexto de la aplicación.
     * @property historialDao acceso a los datos del historial de ubicaciones.
     * @property usuarioDao acceso a la entidad de usuarios.
     * @property sessionManager gestor de la sesión del usuario.
     */
    class Factory(
        private val application: Application,
        private val historialDao: HistorialUbicacionDao,
        private val usuarioDao: UsuarioDao,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        /**
         * crea una nueva instancia de la clase viewmodel requerida.
         *
         * @param modelClass la clase del viewmodel a instanciar.
         * @return una nueva instancia de [HistorialUbicacionesViewModel].
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistorialUbicacionesViewModel(
                application, historialDao, usuarioDao, sessionManager
            ) as T
    }
}