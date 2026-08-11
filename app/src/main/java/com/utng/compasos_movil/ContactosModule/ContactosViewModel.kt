package com.utng.compasos_movil.ContactosModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.dao.ContactoEmergenciaDao
import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.ContactoEmergenciaEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ContactosUiState(
    val cargando: Boolean = true,
    val usuarioActual: UsuarioEntity? = null,
    val contactos: List<ContactoEmergenciaEntity> = emptyList(),
    val mensaje: String? = null
)

class ContactosViewModel(
    private val usuarioDao: UsuarioDao,
    private val contactoEmergenciaDao: ContactoEmergenciaDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactosUiState())
    val uiState: StateFlow<ContactosUiState> = _uiState.asStateFlow()

    init {
        cargarContactos()
    }

    fun cargarContactos() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId()
            if (userId == null) {
                _uiState.update { it.copy(cargando = false, mensaje = "No hay sesión activa") }
                return@launch
            }

            val usuario  = usuarioDao.obtenerPorId(userId)
            // obtenerPorUsuario ya viene ordenado por prioridad (ORDER BY prioridad en el DAO)
            val contactos = contactoEmergenciaDao.obtenerPorUsuario(userId)

            _uiState.update {
                it.copy(cargando = false, usuarioActual = usuario, contactos = contactos)
            }
        }
    }

    fun agregarContacto(
        nombre: String,
        telefono: String,
        correo: String?,
        parentesco: String?,
        prioridad: Int?
    ) {
        viewModelScope.launch {
            val userId = sessionManager.obtenerUsuarioId() ?: return@launch

            contactoEmergenciaDao.insertar(
                ContactoEmergenciaEntity(
                    id          = UUID.randomUUID().toString(),
                    usuarioId   = userId,
                    nombre      = nombre.trim(),
                    telefono    = telefono.trim(),
                    correo      = correo?.trim()?.ifBlank { null },
                    parentesco  = parentesco?.trim()?.ifBlank { null },
                    prioridad   = prioridad
                )
            )
            cargarContactos()
        }
    }
}

class ContactosViewModelFactory(
    private val usuarioDao: UsuarioDao,
    private val contactoEmergenciaDao: ContactoEmergenciaDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactosViewModel(
            usuarioDao            = usuarioDao,
            contactoEmergenciaDao = contactoEmergenciaDao,
            sessionManager        = sessionManager
        ) as T
    }
}