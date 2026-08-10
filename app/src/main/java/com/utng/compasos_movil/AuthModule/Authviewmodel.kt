package com.utng.compasos_movil.AuthModule

import com.utng.compasos_movil.utils.SessionManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.data.entity.UsuarioEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estados posibles de la autenticación
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val usuario: UsuarioEntity) : AuthState()
    data class Error(val mensaje: String) : AuthState()
    object LoggedOut : AuthState()
}

/**
 * ViewModel de Autenticación
 * Maneja el estado de login, registro, perfil y eliminación de usuario
 * Integrado con SessionManager para guardar ID del usuario
 */
class AuthViewModel(
    private val authService: AuthService,
    private val sessionManager: SessionManager
) : ViewModel() {

    // Estado actual de la autenticación
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // Usuario actualmente autenticado
    private val _usuarioActual = MutableStateFlow<UsuarioEntity?>(null)
    val usuarioActual: StateFlow<UsuarioEntity?> = _usuarioActual

    // Mensajes de error
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ID del usuario en sesión (para fácil acceso)
    private val _usuarioIdSesion = MutableStateFlow<String?>(null)
    val usuarioIdSesion: StateFlow<String?> = _usuarioIdSesion

    init {
        // Verificar si hay una sesión activa al iniciar
        cargarSesionGuardada()
    }

    /**
     * Carga la sesión guardada al iniciar la app
     */
    private fun cargarSesionGuardada() {
        if (sessionManager.estaSesionActiva()) {
            _usuarioIdSesion.value = sessionManager.obtenerUsuarioId()
            _authState.value = AuthState.Success(
                UsuarioEntity(
                    id = sessionManager.obtenerUsuarioId() ?: "",
                    nombre = sessionManager.obtenerUsuarioNombre() ?: "",
                    apellidoPaterno = null,
                    apellidoMaterno = null,
                    correo = sessionManager.obtenerUsuarioEmail() ?: "",
                    password = "",
                    telefono = null,
                    foto = null,
                    fechaNacimiento = null,
                    sexo = null,
                    activo = true,
                    fechaRegistro = ""
                )
            )
        }
    }

    // ============================================================
    // LOGIN
    // ============================================================

    /**
     * Realiza el login de un usuario
     * @param correo Email del usuario
     * @param password Contraseña
     */
    fun login(correo: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val usuario = authService.login(correo, password)

                if (usuario != null) {
                    // Guardar sesión en SharedPreferences
                    sessionManager.guardarSesion(
                        usuarioId = usuario.id,
                        email = usuario.correo,
                        nombre = usuario.nombre
                    )

                    // Actualizar el ViewModel
                    _usuarioActual.value = usuario
                    _usuarioIdSesion.value = usuario.id
                    _authState.value = AuthState.Success(usuario)
                    _errorMessage.value = null
                } else {
                    _authState.value = AuthState.Error("Correo o contraseña incorrectos")
                    _errorMessage.value = "Correo o contraseña incorrectos"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error desconocido")
                _errorMessage.value = e.message ?: "Error en el login"
            }
        }
    }

    // ============================================================
    // REGISTRO
    // ============================================================

    /**
     * Registra un nuevo usuario
     */
    fun registrar(
        nombre: String,
        apellidoPaterno: String? = null,
        apellidoMaterno: String? = null,
        correo: String,
        password: String,
        telefono: String? = null,
        fechaNacimiento: String? = null,
        sexo: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val usuario = authService.registrar(
                    nombre = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    correo = correo,
                    password = password,
                    telefono = telefono,
                    fechaNacimiento = fechaNacimiento,
                    sexo = sexo
                )

                if (usuario != null) {
                    // Guardar sesión en SharedPreferences
                    sessionManager.guardarSesion(
                        usuarioId = usuario.id,
                        email = usuario.correo,
                        nombre = usuario.nombre
                    )

                    _usuarioActual.value = usuario
                    _usuarioIdSesion.value = usuario.id
                    _authState.value = AuthState.Success(usuario)
                    _errorMessage.value = null
                } else {
                    _authState.value = AuthState.Error("Falló el registro. Verifica los datos.")
                    _errorMessage.value = "Falló el registro. Verifica los datos."
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error en el registro")
                _errorMessage.value = e.message ?: "Error en el registro"
            }
        }
    }

    // ============================================================
    // PERFIL
    // ============================================================

    /**
     * Obtiene el perfil del usuario actual
     */
    fun obtenerPerfil(usuarioId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val usuario = authService.obtenerPerfil(usuarioId)

                if (usuario != null) {
                    _usuarioActual.value = usuario
                    _authState.value = AuthState.Success(usuario)
                    _errorMessage.value = null
                } else {
                    _authState.value = AuthState.Error("Usuario no encontrado")
                    _errorMessage.value = "Usuario no encontrado"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al obtener perfil")
                _errorMessage.value = e.message ?: "Error al obtener perfil"
            }
        }
    }

    // ============================================================
    // ELIMINAR USUARIO
    // ============================================================

    /**
     * Elimina/desactiva el usuario actual
     */
    fun eliminarCuenta(usuarioId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            try {
                val resultado = authService.eliminarUsuario(usuarioId)

                if (resultado) {
                    sessionManager.cerrarSesion()
                    _usuarioActual.value = null
                    _usuarioIdSesion.value = null
                    _authState.value = AuthState.LoggedOut
                    _errorMessage.value = null
                } else {
                    _authState.value = AuthState.Error("Falló la eliminación de la cuenta")
                    _errorMessage.value = "Falló la eliminación de la cuenta"
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Error al eliminar cuenta")
                _errorMessage.value = e.message ?: "Error al eliminar cuenta"
            }
        }
    }

    // ============================================================
    // LOGOUT
    // ============================================================

    /**
     * Cierra la sesión del usuario actual
     */
    fun logout() {
        sessionManager.cerrarSesion()
        _usuarioActual.value = null
        _usuarioIdSesion.value = null
        _authState.value = AuthState.LoggedOut
        _errorMessage.value = null
    }

    // ============================================================
    // UTILIDADES
    // ============================================================

    /**
     * Obtiene el ID del usuario en sesión
     */
    fun obtenerUsuarioIdSesion(): String? {
        return _usuarioIdSesion.value ?: sessionManager.obtenerUsuarioId()
    }

    /**
     * Limpia los mensajes de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Reinicia el estado a Idle
     */
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}