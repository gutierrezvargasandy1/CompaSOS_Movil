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
 * Estados para la carga de datos
 */
sealed class ProfileLoadState {
    object Idle : ProfileLoadState()
    object Loading : ProfileLoadState()
    data class Success(val usuario: UsuarioEntity, val perfil: PerfilMedicoEntity?) : ProfileLoadState()
    data class Error(val mensaje: String) : ProfileLoadState()
}

/**
 * Estados para la actualización de datos
 */
sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Saving : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val mensaje: String) : ProfileUpdateState()
}

/**
 * ViewModel para editar el perfil del usuario
 */
class EditProfileViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val perfilMedicoRepository: PerfilMedicoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Estado de carga de datos
    private val _profileLoadState = MutableStateFlow<ProfileLoadState>(ProfileLoadState.Idle)
    val profileLoadState: StateFlow<ProfileLoadState> = _profileLoadState

    // Estado de actualización
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState

    // Datos del usuario en edición
    private val _usuarioEnEdicion = MutableStateFlow<UsuarioEntity?>(null)
    val usuarioEnEdicion: StateFlow<UsuarioEntity?> = _usuarioEnEdicion

    // Datos del perfil médico en edición
    private val _perfilEnEdicion = MutableStateFlow<PerfilMedicoEntity?>(null)
    val perfilEnEdicion: StateFlow<PerfilMedicoEntity?> = _perfilEnEdicion

    // Mensaje de error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Carga los datos actuales del usuario y su perfil médico
     */
    fun cargarDatos() {
        viewModelScope.launch {
            _profileLoadState.value = ProfileLoadState.Loading

            try {
                val usuarioId = sessionManager.obtenerUsuarioId()

                if (usuarioId != null) {
                    val usuario = usuarioRepository.obtenerPorId(usuarioId)
                    val perfil = perfilMedicoRepository.obtenerPerfilDelUsuario(usuarioId)

                    if (usuario != null) {
                        _usuarioEnEdicion.value = usuario
                        _perfilEnEdicion.value = perfil
                        _profileLoadState.value = ProfileLoadState.Success(usuario, perfil)
                        _errorMessage.value = null
                    } else {
                        _profileLoadState.value = ProfileLoadState.Error("Usuario no encontrado")
                        _errorMessage.value = "Usuario no encontrado"
                    }
                } else {
                    _profileLoadState.value = ProfileLoadState.Error("Sesión no válida")
                    _errorMessage.value = "Sesión no válida"
                }
            } catch (e: Exception) {
                _profileLoadState.value = ProfileLoadState.Error(e.message ?: "Error desconocido")
                _errorMessage.value = e.message ?: "Error al cargar datos"
            }
        }
    }

    /**
     * Actualiza los datos personales del usuario
     */
    fun actualizarDatosPersonales(
        nombre: String,
        apellidoPaterno: String?,
        apellidoMaterno: String?,
        telefono: String?,
        fechaNacimiento: String?,
        sexo: String?
    ) {
        val usuarioActual = _usuarioEnEdicion.value ?: return

        val usuarioActualizado = usuarioActual.copy(
            nombre = nombre,
            apellidoPaterno = apellidoPaterno,
            apellidoMaterno = apellidoMaterno,
            telefono = telefono,
            fechaNacimiento = fechaNacimiento,
            sexo = sexo
        )

        _usuarioEnEdicion.value = usuarioActualizado
    }

    /**
     * Actualiza los datos médicos del usuario
     */
    fun actualizarDatosMedicos(
        tipoSangre: String?,
        alergias: String?,
        padecimientos: String?,
        medicamentos: String?,
        peso: Double?,
        altura: Double?,
        observaciones: String?
    ) {
        val perfilActual = _perfilEnEdicion.value

        val usuarioId = sessionManager.obtenerUsuarioId() ?: return

        val perfilActualizado = if (perfilActual != null) {
            perfilActual.copy(
                tipoSangre = tipoSangre,
                alergias = alergias,
                padecimientos = padecimientos,
                medicamentos = medicamentos,
                peso = peso,
                altura = altura,
                observaciones = observaciones
            )
        } else {
            PerfilMedicoEntity(
                id = "perfil_${usuarioId}_${System.currentTimeMillis()}",
                usuarioId = usuarioId,
                tipoSangre = tipoSangre,
                alergias = alergias,
                padecimientos = padecimientos,
                medicamentos = medicamentos,
                peso = peso,
                altura = altura,
                observaciones = observaciones
            )
        }

        _perfilEnEdicion.value = perfilActualizado
    }

    /**
     * Guarda todos los cambios en la base de datos
     */
    fun guardarCambios() {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Saving

            try {
                val usuarioActualizado = _usuarioEnEdicion.value
                val perfilActualizado = _perfilEnEdicion.value

                if (usuarioActualizado != null) {
                    // Actualizar usuario
                    usuarioRepository.actualizarUsuario(usuarioActualizado)

                    // Actualizar o crear perfil médico
                    if (perfilActualizado != null) {
                        if (perfilMedicoRepository.tienePerfil(usuarioActualizado.id)) {
                            perfilMedicoRepository.actualizarPerfil(perfilActualizado)
                        } else {
                            perfilMedicoRepository.crearPerfil(perfilActualizado)
                        }
                    }

                    _profileUpdateState.value = ProfileUpdateState.Success
                    _errorMessage.value = null
                } else {
                    _profileUpdateState.value = ProfileUpdateState.Error("Error: usuario no encontrado")
                    _errorMessage.value = "Error: usuario no encontrado"
                }
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Error desconocido")
                _errorMessage.value = e.message ?: "Error al guardar cambios"
            }
        }
    }

    /**
     * Descarta los cambios y recarga los datos originales
     */
    fun descartarCambios() {
        cargarDatos()
        _profileUpdateState.value = ProfileUpdateState.Idle
        _errorMessage.value = null
    }

    /**
     * Limpia los mensajes de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reinicia el estado de actualización
     */
    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }
}