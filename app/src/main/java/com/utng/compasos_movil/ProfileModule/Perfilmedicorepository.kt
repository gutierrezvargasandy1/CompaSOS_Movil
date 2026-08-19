package com.utng.compasos_movil.ProfileModule

import com.utng.compasos_movil.data.dao.PerfilMedicoDao
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity

/**
 * Repositorio para gestionar operaciones del Perfil Médico
 * Encapsula la lógica de acceso a datos usando Room
 */
class PerfilMedicoRepository(
    private val perfilMedicoDao: PerfilMedicoDao
) {

    /**
     * Crea un nuevo perfil médico
     * @param perfil Entidad del perfil médico
     */
    suspend fun crearPerfil(perfil: PerfilMedicoEntity) {
        perfilMedicoDao.insertar(perfil)
    }

    /**
     * Actualiza un perfil médico existente
     * @param perfil Entidad del perfil médico con datos actualizados
     */
    suspend fun actualizarPerfil(perfil: PerfilMedicoEntity) {
        perfilMedicoDao.actualizar(perfil)
    }

    /**
     * Obtiene el perfil médico de un usuario
     * @param usuarioId ID del usuario
     * @return PerfilMedicoEntity si existe, null si no
     */
    suspend fun obtenerPerfilDelUsuario(usuarioId: String): PerfilMedicoEntity? {
        return perfilMedicoDao.obtenerPorUsuario(usuarioId)
    }

    /**
     * Obtiene todos los perfiles médicos registrados
     * @return Lista de PerfilMedicoEntity
     */
    suspend fun obtenerTodos(): List<PerfilMedicoEntity> {
        return perfilMedicoDao.obtenerTodos()
    }

    /**
     * Elimina el perfil médico de un usuario
     * @param usuarioId ID del usuario
     */
    suspend fun eliminarPerfilDelUsuario(usuarioId: String) {
        perfilMedicoDao.eliminarPorUsuario(usuarioId)
    }

    /**
     * Verifica si un usuario tiene perfil médico
     * @param usuarioId ID del usuario
     * @return true si tiene perfil, false si no
     */
    suspend fun tienePerfil(usuarioId: String): Boolean {
        return perfilMedicoDao.obtenerPorUsuario(usuarioId) != null
    }
}