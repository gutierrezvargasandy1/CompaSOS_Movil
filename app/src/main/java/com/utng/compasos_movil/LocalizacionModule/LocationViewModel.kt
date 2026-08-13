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

class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository(application)

    // Coordenadas por defecto hasta que llegue el primer GPS real
    private val _ubicacion = MutableStateFlow(UbicacionActual(21.1619, -101.1925))
    val ubicacion: StateFlow<UbicacionActual> = _ubicacion.asStateFlow()

    fun iniciarSeguimiento() {
        viewModelScope.launch {
            repository.ubicacionEnVivo().collect { _ubicacion.value = it }
        }
    }
}