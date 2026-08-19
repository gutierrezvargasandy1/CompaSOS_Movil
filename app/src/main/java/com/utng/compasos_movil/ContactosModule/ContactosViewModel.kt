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

/**
 * estado de la interfaz de usuario para la pantalla de contactos de emergencia.
 *
 * @property cargando indica si se está realizando una carga asíncrona de datos.
 * @property usuarioActual entidad [UsuarioEntity] asociada a la sesión activa.
 * @property contactos lista de entidades [ContactoEmergenciaEntity] registradas por el usuario.
 * @property mensaje mensaje de información o error para mostrar en la interfaz.
 */
data class ContactosUiState(
    val cargando: Boolean = true,
    val usuarioActual: UsuarioEntity? = null,
    val contactos: List<ContactoEmergenciaEntity> = emptyList(),
    val mensaje: String? = null
)

/**
 * viewmodel encargado de la gestión de contactos de emergencia del usuario.
 * consulta y persiste la información en la base de datos local a través de [UsuarioDao] y [ContactoEmergenciaDao],
 * gestionando el estado con [ContactosUiState].
 *
 * @property usuarioDao acceso a los datos del usuario local.
 * @property contactoEmergenciaDao acceso a la tabla de contactos de emergencia.
 * @property sessionManager gestor para obtener el identificador del usuario con sesión activa.
 */
class ContactosViewModel(
    private val usuarioDao: UsuarioDao,
    private val contactoEmergenciaDao: ContactoEmergenciaDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactosUiState())

    /** flujo de estado observable que expone el [ContactosUiState] actualizado para la interfaz de usuario. */
    val uiState: StateFlow<ContactosUiState> = _uiState.asStateFlow()

    init {
        cargarContactos()
    }

    /**
     * obtiene de forma asíncrona los datos del usuario activo y su lista de contactos de emergencia
     * ordenados por prioridad desde la base de datos local y actualiza el [uiState].
     */
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

    /**
     * crea e inserta un nuevo contacto de emergencia en la base de datos asignando un uuid único
     * y recarga la lista de contactos al finalizar.
     *
     * @param nombre nombre completo del contacto.
     * @param telefono número telefónico del contacto.
     * @param correo correo electrónico opcional del contacto.
     * @param parentesco relación o parentesco con el usuario (opcional).
     * @param prioridad nivel de prioridad asignado al contacto (opcional).
     */
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

/**
 * fábrica de viewmodel para instanciar [ContactosViewModel] pasando sus dependencias requeridas.
 *
 * @property usuarioDao dao para consultas de usuario.
 * @property contactoEmergenciaDao dao para operaciones de contactos de emergencia.
 * @property sessionManager gestor de la sesión actual del usuario.
 */
class ContactosViewModelFactory(
    private val usuarioDao: UsuarioDao,
    private val contactoEmergenciaDao: ContactoEmergenciaDao,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {

    /**
     * crea una nueva instancia del [ContactosViewModel] con las dependencias suministradas.
     *
     * @param modelClass clase del viewmodel solicitado.
     * @return una instancia de [ContactosViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ContactosViewModel(
            usuarioDao            = usuarioDao,
            contactoEmergenciaDao = contactoEmergenciaDao,
            sessionManager        = sessionManager
        ) as T
    }
}