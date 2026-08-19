package com.utng.compasos_movil.LocalizacionModule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.LocationRepository
import com.utng.compasos_movil.data.UbicacionActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * viewmodel encargado de gestionar el rastreo de ubicación geográfica en tiempo real
 * utilizando el repositorio de localización.
 *
 * @property application contexto de la aplicación android para acceder a los servicios de localización.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * repositorio para obtener y gestionar las lecturas de localización gps.
     */
    private val repository = LocationRepository(application)

    // Coordenadas por defecto hasta que llegue el primer GPS real
    /**
     * flujo interno mutable que almacena la ubicación actual registrada.
     */
    private val _ubicacion = MutableStateFlow(UbicacionActual(21.1619, -101.1925))

    /**
     * flujo observable público que emite la ubicación geográfica actual.
     */
    val ubicacion: StateFlow<UbicacionActual> = _ubicacion.asStateFlow()

    /**
     * inicia la recolección continua de actualizaciones de ubicación gps en tiempo real.
     */
    fun iniciarSeguimiento() {
        viewModelScope.launch {
            repository.ubicacionEnVivo().collect { _ubicacion.value = it }
        }
    }
}