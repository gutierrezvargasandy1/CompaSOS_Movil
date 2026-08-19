package com.utng.compasos_movil.FamiliaModule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.utng.compasos_movil.dao.MiembroConDatos
import com.utng.compasos_movil.data.dao.FamiliaDao
import com.utng.compasos_movil.data.dao.FamiliaUsuarioDao
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

/**
 * modelo de datos que combina la información de un grupo familiar con la lista de sus miembros y sus datos detallados.
 *
 * @property familia entidad [FamiliaEntity] que representa el grupo familiar.
 * @property miembros lista de objetos [MiembroConDatos] que contiene la información de los integrantes del grupo.
 */
data class FamiliaConMiembros(
    val familia: FamiliaEntity,
    val miembros: List<MiembroConDatos>
)

/**
 * estado de la interfaz de usuario para el módulo de gestión de familias.
 *
 * @property cargando indica si los datos de la familia están en proceso de carga.
 * @property usuarioActual entidad [UsuarioEntity] que representa al usuario autenticado en la sesión.
 * @property familias lista de grupos familiares a los que pertenece el usuario.
 * @property resultadosBusqueda lista de usuarios encontrados en las búsquedas para agregar a un grupo.
 * @property buscando indica si hay una consulta de búsqueda de usuarios en progreso.
 * @property mensaje mensaje de notificación o error para mostrar en la interfaz.
 */
data class FamiliaUiState(
    val cargando: Boolean = true,
    val usuarioActual: UsuarioEntity? = null,
    val familias: List<FamiliaConMiembros> = emptyList(),
    val resultadosBusqueda: List<UsuarioEntity> = emptyList(),
    val buscando: Boolean = false,
    val mensaje: String? = null
)

/**
 * viewmodel encargado de la lógica de negocio para la gestión de grupos familiares,
 * creación de familias, búsqueda de usuarios y adición de miembros.
 *
 * @property usuarioDao acceso a la entidad de usuarios en la base de datos local.
 * @property familiaDao acceso a la entidad de familias en la base de datos local.
 * @property familiaUsuarioDao acceso a la relación entre familias y usuarios en la base de datos local.
 * @property sessionManager gestor para la administración de la sesión del usuario.
 */
class FamiliaViewModel(
    private val usuarioDao:        UsuarioDao,
    private val familiaDao:        FamiliaDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val sessionManager:    SessionManager
) : ViewModel() {

    /**
     * flujo interno mutable para el estado de la interfaz de usuario.
     */
    private val _uiState = MutableStateFlow(FamiliaUiState())

    /**
     * flujo observable público con el estado actual de la interfaz de usuario.
     */
    val uiState: StateFlow<FamiliaUiState> = _uiState.asStateFlow()

    init { cargarFamilias() }

    /**
     * carga las familias vinculadas al usuario en sesión junto con sus miembros asociados.
     */
    fun cargarFamilias() {
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true) }

            val userId = sessionManager.obtenerUsuarioId()
            if (userId == null) {
                _uiState.update { it.copy(cargando = false, mensaje = "No hay sesión activa") }
                return@launch
            }

            val usuario   = usuarioDao.obtenerPorId(userId)
            val relaciones = familiaUsuarioDao.obtenerFamiliasDeUsuario(userId)

            // ✅ for loop: permite llamar suspend functions en cada iteración
            val familias = mutableListOf<FamiliaConMiembros>()
            for (relacion in relaciones) {
                val familia  = familiaDao.obtenerPorId(relacion.familiaId) ?: continue
                val miembros = familiaUsuarioDao.obtenerMiembrosConDatos(familia.id)
                familias.add(FamiliaConMiembros(familia, miembros))
            }

            _uiState.update {
                it.copy(cargando = false, usuarioActual = usuario, familias = familias)
            }
        }
    }

    /**
     * crea un nuevo grupo familiar registrando al usuario actual como su administrador.
     *
     * @param nombre nombre asignado al nuevo grupo familiar.
     */
    fun crearFamilia(nombre: String) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return

        viewModelScope.launch {
            val userId = sessionManager.obtenerUsuarioId() ?: return@launch

            val nuevaFamilia = FamiliaEntity(
                id     = UUID.randomUUID().toString(),
                nombre = nombreLimpio
            )
            familiaDao.insertar(nuevaFamilia)

            familiaUsuarioDao.insertar(
                FamiliaUsuarioEntity(
                    familiaId = nuevaFamilia.id,
                    usuarioId = userId,
                    rol       = "Administrador"
                )
            )
            cargarFamilias()
        }
    }

    /**
     * busca usuarios registrados que coincidan con la consulta y excluye a los que ya integran la familia especificada.
     *
     * @param texto término o criterio de búsqueda ingresado por el usuario.
     * @param familiaId identificador del grupo familiar para filtrar miembros existentes.
     */
    fun buscarUsuarios(texto: String, familiaId: String) {
        viewModelScope.launch {
            val consulta = texto.trim()
            if (consulta.isBlank()) {
                _uiState.update { it.copy(resultadosBusqueda = emptyList(), buscando = false) }
                return@launch
            }

            _uiState.update { it.copy(buscando = true) }

            val encontrados = usuarioDao.buscar(consulta)
            val idsMiembros = familiaUsuarioDao.obtenerMiembros(familiaId)
                .map { it.usuarioId }
                .toSet()

            _uiState.update {
                it.copy(
                    buscando            = false,
                    resultadosBusqueda  = encontrados.filter { u -> u.id !in idsMiembros }
                )
            }
        }
    }

    /**
     * agrega un nuevo integrante a un grupo familiar asignándole un rol específico.
     *
     * @param familiaId identificador de la familia a la que se integrará el usuario.
     * @param usuario entidad [UsuarioEntity] del usuario que será agregado.
     * @param rol rol dentro de la familia (por defecto "Miembro").
     */
    fun agregarMiembro(familiaId: String, usuario: UsuarioEntity, rol: String = "Miembro") {
        viewModelScope.launch {
            if (familiaUsuarioDao.existeMiembro(familiaId, usuario.id) == 0) {
                familiaUsuarioDao.insertar(
                    FamiliaUsuarioEntity(
                        familiaId = familiaId,
                        usuarioId = usuario.id,
                        rol       = rol
                    )
                )
            }
            _uiState.update { estado ->
                estado.copy(
                    resultadosBusqueda = estado.resultadosBusqueda.filter { it.id != usuario.id }
                )
            }
            cargarFamilias()
        }
    }

    /**
     * limpia el listado de resultados de búsqueda de la interfaz de usuario.
     */
    fun limpiarBusqueda() {
        _uiState.update { it.copy(resultadosBusqueda = emptyList()) }
    }
}

/**
 * fábrica de proveedores para instanciar [FamiliaViewModel] inyectando sus dependencias.
 *
 * @property usuarioDao acceso a la entidad de usuarios.
 * @property familiaDao acceso a la entidad de familias.
 * @property familiaUsuarioDao acceso a la relación entre familias y usuarios.
 * @property sessionManager gestor de la sesión del usuario.
 */
class FamiliaViewModelFactory(
    private val usuarioDao:        UsuarioDao,
    private val familiaDao:        FamiliaDao,
    private val familiaUsuarioDao: FamiliaUsuarioDao,
    private val sessionManager:    SessionManager
) : ViewModelProvider.Factory {
    /**
     * crea una nueva instancia de la clase viewmodel requerida.
     *
     * @param modelClass la clase del viewmodel a instanciar.
     * @return una nueva instancia de [FamiliaViewModel].
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return FamiliaViewModel(
            usuarioDao        = usuarioDao,
            familiaDao        = familiaDao,
            familiaUsuarioDao = familiaUsuarioDao,
            sessionManager    = sessionManager
        ) as T
    }
}