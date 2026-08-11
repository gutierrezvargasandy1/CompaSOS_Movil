package com.utng.compasos_movil.FamiliaModule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.AppDatabase
import com.utng.compasos_movil.data.dao.FamiliaDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
import com.utng.compasos_movil.data.dao.MiembroConDatos
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.FamiliaEntity
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// Wrapper: familia + sus miembros (mismo patrón que usas en las otras screens)
data class FamiliaConMiembros(
    val familia: FamiliaEntity,
    val miembros: List<MiembroConDatos>
)

data class FamiliaUiState(
    val cargando: Boolean = true,
    val usuarioActual: UsuarioEntity? = null,
    val familias: List<FamiliaConMiembros> = emptyList(),
    val resultadosBusqueda: List<UsuarioEntity> = emptyList(),
    val buscando: Boolean = false,
    val mensaje: String? = null
)

class FamiliaViewModel(
    private val usuarioDao: UsuarioDao,
    private val familiaDao: FamiliaDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamiliaUiState())
    val uiState: StateFlow<FamiliaUiState> = _uiState.asStateFlow()

    init {
        cargarFamilias()
    }

    fun cargarFamilias() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId()
            if (userId == null) {
                _uiState.update { it.copy(cargando = false, mensaje = "No hay sesión activa") }
                return@launch
            }

            val usuario = usuarioDao.obtenerPorId(userId)
            val relaciones = familiaUsuarioDao.obtenerFamiliasDeUsuario(userId)

            val familias = relaciones.mapNotNull { relacion ->
                val familia = familiaDao.obtenerPorId(relacion.familiaId) ?: return@mapNotNull null
                val miembros = familiaUsuarioDao.obtenerMiembrosConDatos(familia.id)
                FamiliaConMiembros(familia, miembros)
            }

            _uiState.update {
                it.copy(cargando = false, usuarioActual = usuario, familias = familias)
            }
        }
    }

    fun crearFamilia(nombre: String) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return

        viewModelScope.launch {
            val userId = sessionManager.obtenerUsuarioId() ?: return@launch

            val nuevaFamilia = FamiliaEntity(
                id = UUID.randomUUID().toString(),
                nombre = nombreLimpio
            )
            familiaDao.insertar(nuevaFamilia)

            // El creador queda como administrador
            familiaUsuarioDao.insertar(
                FamiliaUsuarioEntity(
                    familiaId = nuevaFamilia.id,
                    usuarioId = userId,
                    rol = "Administrador"
                )
            )

            cargarFamilias()
        }
    }

    fun buscarUsuarios(texto: String, familiaId: String) {
        viewModelScope.launch {
            val consulta = texto.trim()
            if (consulta.isBlank()) {
                _uiState.update { it.copy(resultadosBusqueda = emptyList(), buscando = false) }
                return@launch
            }

            _uiState.update { it.copy(buscando = true) }

            val encontrados = usuarioDao.buscar(consulta)
            // No mostrar a quienes ya son miembros de esta familia
            val idsMiembros = familiaUsuarioDao.obtenerMiembros(familiaId)
                .map { it.usuarioId }
                .toSet()

            val filtrados = encontrados.filter { it.id !in idsMiembros }

            _uiState.update { it.copy(buscando = false, resultadosBusqueda = filtrados) }
        }
    }

    fun agregarMiembro(familiaId: String, usuario: UsuarioEntity, rol: String = "Miembro") {
        viewModelScope.launch {
            val yaExiste = familiaUsuarioDao.existeMiembro(familiaId, usuario.id) > 0
            if (!yaExiste) {
                familiaUsuarioDao.insertar(
                    FamiliaUsuarioEntity(familiaId = familiaId, usuarioId = usuario.id, rol = rol)
                )
            }
            // Quita al recién agregado de los resultados para poder seguir agregando otros
            _uiState.update { estado ->
                estado.copy(resultadosBusqueda = estado.resultadosBusqueda.filter { it.id != usuario.id })
            }
            cargarFamilias()
        }
    }

    fun limpiarBusqueda() {
        _uiState.update { it.copy(resultadosBusqueda = emptyList()) }
    }
}

class FamiliaViewModelFactory(
    private val usuarioDao: UsuarioDao,
    private val familiaDao: FamiliaDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FamiliaViewModel(
            usuarioDao = usuarioDao,
            familiaDao = familiaDao,
            familiaUsuarioDao = familiaUsuarioDao,
            sessionManager = sessionManager
        ) as T
    }
}
