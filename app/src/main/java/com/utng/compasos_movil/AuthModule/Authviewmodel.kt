package com.utng.compasos_movil.AuthModule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.config.AlertaMqttService
import com.utng.compasos_movil.config.TvSyncService
import com.utng.compasos_movil.data.entity.UsuarioEntity
import com.utng.compasos_movil.utils.SessionManager
import dagger.hilt.android.internal.Contexts.getApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * representa los distintos estados en los que se puede encontrar el proceso de autenticación.
 */
sealed class AuthState {
    /** estado inicial o en reposo. */
    object Idle      : AuthState()

    /** indica que se está procesando una operación asíncrona de autenticación. */
    object Loading   : AuthState()

    /**
     * indica que la operación fue exitosa.
     * @property usuario la entidad [UsuarioEntity] autenticada o registrada.
     */
    data class Success(val usuario: UsuarioEntity) : AuthState()

    /**
     * indica que la operación falló.
     * @property mensaje descripción del error ocurrido.
     */
    data class Error(val mensaje: String)          : AuthState()

    /** indica que el usuario ha cerrado sesión o eliminado su cuenta. */
    object LoggedOut : AuthState()
}

/**
 * viewmodel encargado de gestionar el estado de la interfaz de usuario para la autenticación.
 * coordina las llamadas al servicio [AuthService], mantiene el estado de la sesión activa
 * mediante [SessionManager] e inicia los servicios de alertas mqtt y sincronización con tv.
 *
 * @param application instancia de la aplicación necesaria para el contexto del [AndroidViewModel].
 * @property authService servicio que contiene la lógica de negocio de autenticación.
 * @property sessionManager gestor de preferencias para el almacenamiento local de la sesión.
 */
class AuthViewModel(
    application: Application,                  // ← AndroidViewModel necesita esto
    private val authService:    AuthService,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {           // ← cambio aquí

    private val _authState      = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _usuarioActual  = MutableStateFlow<UsuarioEntity?>(null)
    val usuarioActual: StateFlow<UsuarioEntity?> = _usuarioActual

    private val _errorMessage   = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _usuarioIdSesion = MutableStateFlow<String?>(null)
    val usuarioIdSesion: StateFlow<String?> = _usuarioIdSesion

    init { cargarSesionGuardada() }

    /**
     * verifica si existe una sesión activa almacenada localmente en [SessionManager].
     * de ser así, restaura el estado de autenticación a [AuthState.Success] recreando
     * los datos básicos del usuario almacenados.
     */
    private fun cargarSesionGuardada() {
        if (sessionManager.estaSesionActiva()) {
            _usuarioIdSesion.value = sessionManager.obtenerUsuarioId()
            _authState.value = AuthState.Success(
                UsuarioEntity(
                    id              = sessionManager.obtenerUsuarioId() ?: "",
                    nombre          = sessionManager.obtenerUsuarioNombre() ?: "",
                    apellidoPaterno = null,
                    apellidoMaterno = null,
                    correo          = sessionManager.obtenerUsuarioEmail() ?: "",
                    password        = "",
                    telefono        = null,
                    foto            = null,
                    fechaNacimiento = null,
                    sexo            = null,
                    activo          = true,
                    fechaRegistro   = ""
                )
            )
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * ejecuta el inicio de sesión de forma asíncrona.
     * si es exitoso, guarda la sesión, inicia los servicios de sincronización en segundo plano
     * ([AlertaMqttService] y [TvSyncService]) y actualiza el estado a [AuthState.Success].
     *
     * @param correo correo electrónico ingresado.
     * @param password contraseña ingresada.
     */
    fun login(correo: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val usuario = authService.login(correo, password)
                if (usuario != null) {
                    sessionManager.guardarSesion(
                        usuarioId = usuario.id,
                        email     = usuario.correo,
                        nombre    = usuario.nombre
                    )
                    _usuarioActual.value   = usuario
                    _usuarioIdSesion.value = usuario.id

                    // ✅ context disponible via getApplication()
                    AlertaMqttService.iniciar(getApplication(), usuario.id)
                    TvSyncService.iniciar(getApplication(), usuario.id)   // ← agregar (+ el import)
                    _authState.value  = AuthState.Success(usuario)
                    _errorMessage.value = null
                } else {
                    _authState.value    = AuthState.Error("Correo o contraseña incorrectos")
                    _errorMessage.value = "Correo o contraseña incorrectos"
                }
            } catch (e: Exception) {
                _authState.value    = AuthState.Error(e.message ?: "Error desconocido")
                _errorMessage.value = e.message ?: "Error en el login"
            }
        }
    }

    // ── Registro ──────────────────────────────────────────────────────────────

    /**
     * ejecuta el proceso de registro de un nuevo usuario de forma asíncrona.
     * al registrarse correctamente, persiste la sesión activa y actualiza el estado a [AuthState.Success].
     *
     * @param nombre nombre del usuario.
     * @param apellidoPaterno apellido paterno (opcional).
     * @param apellidoMaterno apellido materno (opcional).
     * @param correo correo electrónico.
     * @param password contraseña.
     * @param telefono número de teléfono (opcional).
     * @param fechaNacimiento fecha de nacimiento (opcional).
     * @param sexo género o sexo (opcional).
     */
    fun registrar(
        nombre:          String,
        apellidoPaterno: String? = null,
        apellidoMaterno: String? = null,
        correo:          String,
        password:        String,
        telefono:        String? = null,
        fechaNacimiento: String? = null,
        sexo:            String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val usuario = authService.registrar(
                    nombre          = nombre,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    correo          = correo,
                    password        = password,
                    telefono        = telefono,
                    fechaNacimiento = fechaNacimiento,
                    sexo            = sexo
                )
                if (usuario != null) {
                    sessionManager.guardarSesion(
                        usuarioId = usuario.id,
                        email     = usuario.correo,
                        nombre    = usuario.nombre
                    )
                    _usuarioActual.value   = usuario
                    _usuarioIdSesion.value = usuario.id
                    _authState.value       = AuthState.Success(usuario)
                    _errorMessage.value    = null
                } else {
                    _authState.value    = AuthState.Error("Falló el registro. Verifica los datos.")
                    _errorMessage.value = "Falló el registro. Verifica los datos."
                }
            } catch (e: Exception) {
                _authState.value    = AuthState.Error(e.message ?: "Error en el registro")
                _errorMessage.value = e.message ?: "Error en el registro"
            }
        }
    }

    // ── Perfil ────────────────────────────────────────────────────────────────

    /**
     * consulta la información detallada del perfil del usuario de forma asíncrona.
     *
     * @param usuarioId id único del usuario a consultar.
     */
    fun obtenerPerfil(usuarioId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val usuario = authService.obtenerPerfil(usuarioId)
                if (usuario != null) {
                    _usuarioActual.value = usuario
                    _authState.value     = AuthState.Success(usuario)
                    _errorMessage.value  = null
                } else {
                    _authState.value    = AuthState.Error("Usuario no encontrado")
                    _errorMessage.value = "Usuario no encontrado"
                }
            } catch (e: Exception) {
                _authState.value    = AuthState.Error(e.message ?: "Error al obtener perfil")
                _errorMessage.value = e.message ?: "Error al obtener perfil"
            }
        }
    }

    // ── Eliminar cuenta ───────────────────────────────────────────────────────

    /**
     * solicita la eliminación o desactivación de la cuenta del usuario.
     * si es exitosa, cierra la sesión guardada y cambia el estado a [AuthState.LoggedOut].
     *
     * @param usuarioId id único de la cuenta a eliminar.
     */
    fun eliminarCuenta(usuarioId: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val resultado = authService.eliminarUsuario(usuarioId)
                if (resultado) {
                    sessionManager.cerrarSesion()
                    _usuarioActual.value   = null
                    _usuarioIdSesion.value = null
                    _authState.value       = AuthState.LoggedOut
                    _errorMessage.value    = null
                } else {
                    _authState.value    = AuthState.Error("Falló la eliminación de la cuenta")
                    _errorMessage.value = "Falló la eliminación de la cuenta"
                }
            } catch (e: Exception) {
                _authState.value    = AuthState.Error(e.message ?: "Error al eliminar cuenta")
                _errorMessage.value = e.message ?: "Error al eliminar cuenta"
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    /**
     * cierra la sesión activa, destruye las credenciales en [SessionManager],
     * limpia las referencias de usuario local y cambia el estado a [AuthState.LoggedOut].
     */
    fun logout() {
        sessionManager.cerrarSesion()
        _usuarioActual.value   = null
        _usuarioIdSesion.value = null
        _authState.value       = AuthState.LoggedOut
        _errorMessage.value    = null
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * obtiene el id del usuario de la sesión actual desde el estado en memoria o el [SessionManager].
     *
     * @return el id del usuario o null si no hay sesión activa.
     */
    fun obtenerUsuarioIdSesion(): String? =
        _usuarioIdSesion.value ?: sessionManager.obtenerUsuarioId()

    /** limpia la cadena de mensaje de error actual restableciéndola a null. */
    fun clearError()  { _errorMessage.value = null }

    /** reinicia el estado de la autenticación al valor predeterminado [AuthState.Idle]. */
    fun resetState()  { _authState.value    = AuthState.Idle }
}