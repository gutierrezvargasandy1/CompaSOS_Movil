package com.utng.compasos_movil.ProfileModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.data.repository.UsuarioRepository
import com.utng.compasos_movil.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estados para la carga de perfil
 */
sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(
        val usuario: UsuarioEntity,
        val perfil: PerfilMedicoEntity?
    ) : ProfileState()
    data class Error(val mensaje: String) : ProfileState()
}

/**
 * ViewModel para mostrar el perfil del usuario
 * Carga datos reales de la BD usando SessionManager
 */
class ProfileViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val perfilMedicoRepository: PerfilMedicoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    init {
        cargarPerfil()
    }

    /**
     * Carga el perfil del usuario actual desde la BD
     */
    fun cargarPerfil() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading

            try {
                val usuarioId = sessionManager.obtenerUsuarioId()

                if (usuarioId != null) {
                    // Obtener usuario de la BD
                    val usuario = usuarioRepository.obtenerPorId(usuarioId)

                    if (usuario != null) {
                        // Obtener perfil médico
                        val perfil = perfilMedicoRepository.obtenerPerfilDelUsuario(usuarioId)

                        _profileState.value = ProfileState.Success(usuario, perfil)
                    } else {
                        _profileState.value = ProfileState.Error("Usuario no encontrado en la BD")
                    }
                } else {
                    _profileState.value = ProfileState.Error("Sesión no válida. Por favor, inicia sesión de nuevo.")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(
                    e.message ?: "Error al cargar el perfil"
                )
            }
        }
    }

    /**
     * Recarga el perfil (útil después de editar)
     */
    fun recargarPerfil() {
        cargarPerfil()
    }
}