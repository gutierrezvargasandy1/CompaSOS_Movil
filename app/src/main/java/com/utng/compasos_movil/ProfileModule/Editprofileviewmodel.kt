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
 * representa los estados posibles durante el proceso de carga de datos del perfil.
 */
sealed class ProfileLoadState {
    /**
     * estado inicial de reposo antes de solicitar la carga de datos.
     */
    object Idle : ProfileLoadState()

    /**
     * estado que indica que los datos del usuario y perfil están siendo recuperados.
     */
    object Loading : ProfileLoadState()

    /**
     * estado de éxito que entrega las entidades cargadas.
     *
     * @property usuario entidad [UsuarioEntity] obtenida.
     * @property perfil entidad [PerfilMedicoEntity] opcional obtenida.
     */
    data class Success(val usuario: UsuarioEntity, val perfil: PerfilMedicoEntity?) : ProfileLoadState()

    /**
     * estado de error producido durante la consulta de datos.
     *
     * @property mensaje mensaje descriptivo del fallo presentado.
     */
    data class Error(val mensaje: String) : ProfileLoadState()
}

/**
 * representa los estados posibles durante la actualización de los datos del perfil.
 */
sealed class ProfileUpdateState {
    /**
     * estado inicial de reposo previo a una acción de guardado.
     */
    object Idle : ProfileUpdateState()

    /**
     * estado que indica que se están guardando los cambios en la base de datos.
     */
    object Saving : ProfileUpdateState()

    /**
     * estado de éxito que confirma el guardado correcto de los datos.
     */
    object Success : ProfileUpdateState()

    /**
     * estado de error producido al intentar persistir las modificaciones.
     *
     * @property mensaje mensaje descriptivo de la falla al actualizar.
     */
    data class Error(val mensaje: String) : ProfileUpdateState()
}

/**
 * viewmodel encargado de gestionar la edición y actualización del perfil personal
 * y médico del usuario activo.
 *
 * @property usuarioRepository repositorio para la persistencia de datos personales de usuario.
 * @property perfilMedicoRepository repositorio para la persistencia de datos médicos.
 * @property sessionManager gestor de sesión para obtener las credenciales e identificadores del usuario activo.
 */
class EditProfileViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val perfilMedicoRepository: PerfilMedicoRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Estado de carga de datos
    /**
     * flujo interno mutable para el estado de carga de la información del perfil.
     */
    private val _profileLoadState = MutableStateFlow<ProfileLoadState>(ProfileLoadState.Idle)

    /**
     * flujo observable público para seguir los cambios en la carga del perfil.
     */
    val profileLoadState: StateFlow<ProfileLoadState> = _profileLoadState

    // Estado de actualización
    /**
     * flujo interno mutable para el estado del proceso de guardado/actualización.
     */
    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)

    /**
     * flujo observable público para monitorear el resultado de la actualización.
     */
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState

    // Datos del usuario en edición
    /**
     * flujo interno mutable con la información temporal en edición del usuario.
     */
    private val _usuarioEnEdicion = MutableStateFlow<UsuarioEntity?>(null)

    /**
     * flujo observable público de los datos personales en edición del usuario.
     */
    val usuarioEnEdicion: StateFlow<UsuarioEntity?> = _usuarioEnEdicion

    // Datos del perfil médico en edición
    /**
     * flujo interno mutable con la información temporal en edición del perfil médico.
     */
    private val _perfilEnEdicion = MutableStateFlow<PerfilMedicoEntity?>(null)

    /**
     * flujo observable público de los datos médicos en edición.
     */
    val perfilEnEdicion: StateFlow<PerfilMedicoEntity?> = _perfilEnEdicion

    // Mensaje de error
    /**
     * flujo interno mutable que contiene mensajes de error presentados durante las operaciones.
     */
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * flujo observable público con los mensajes de error.
     */
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * carga los datos actuales del usuario y su perfil médico registrados en la base de datos local.
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
     * actualiza localmente en el flujo de edición los datos personales del usuario.
     *
     * @param nombre nombre principal del usuario.
     * @param apellidoPaterno apellido paterno del usuario.
     * @param apellidoMaterno apellido materno del usuario.
     * @param telefono número telefónico de contacto.
     * @param fechaNacimiento fecha de nacimiento registrada.
     * @param sexo género o sexo con el que se identifica.
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
     * actualiza localmente en el flujo de edición los datos médicos correspondientes.
     *
     * @param tipoSangre tipo de sangre y factor rh del usuario.
     * @param alergias lista o descripción de alergias conocidas.
     * @param padecimientos enfermedades o condiciones médicas registradas.
     * @param medicamentos medicamentos o tratamientos recurrentes.
     * @param peso peso corporal en kilogramos.
     * @param altura estatura o altura en metros o centímetros.
     * @param observaciones notas médicas adicionales relevantes.
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
     * guarda de manera permanente las modificaciones de usuario y perfil médico en la base de datos.
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
     * deshace las modificaciones en edición y reestablece los valores originales de la base de datos.
     */
    fun descartarCambios() {
        cargarDatos()
        _profileUpdateState.value = ProfileUpdateState.Idle
        _errorMessage.value = null
    }

    /**
     * borra o resetea cualquier mensaje de error existente.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * reestablece el estado de actualización al valor por defecto ([ProfileUpdateState.Idle]).
     */
    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }
}