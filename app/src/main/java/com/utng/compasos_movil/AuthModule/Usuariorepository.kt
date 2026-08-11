package com.utng.compasos_movil.data.repository

import com.utng.compasos_movil.data.dao.UsuarioDao
import com.utng.compasos_movil.data.entity.UsuarioEntity

/**
 * Repositorio para gestionar operaciones de Usuario
 * Encapsula la lógica de acceso a datos usando Room
 */
class UsuarioRepository(
    private val usuarioDao: UsuarioDao
) {

    /**
     * Registra un nuevo usuario en la base de datos
     * @param usuario Entidad del usuario a insertar
     */
    suspend fun registrarUsuario(usuario: UsuarioEntity) {
        usuarioDao.insertar(usuario)
    }

    /**
     * Actualiza los datos de un usuario existente
     * @param usuario Entidad del usuario con datos actualizados
     */
    suspend fun actualizarUsuario(usuario: UsuarioEntity) {
        usuarioDao.actualizar(usuario)
    }

    /**
     * Obtiene un usuario por su correo
     * @param correo Email del usuario
     * @return UsuarioEntity si existe, null si no
     */
    suspend fun obtenerPorCorreo(correo: String): UsuarioEntity? {
        return usuarioDao.obtenerPorCorreo(correo)
    }

    /**
     * Obtiene un usuario por su ID
     * @param usuarioId ID del usuario
     * @return UsuarioEntity si existe, null si no
     */
    suspend fun obtenerPorId(usuarioId: String): UsuarioEntity? {
        return usuarioDao.obtenerPorId(usuarioId)
    }

    /**
     * Obtiene todos los usuarios registrados
     * @return Lista de UsuarioEntity
     */
    suspend fun obtenerTodos(): List<UsuarioEntity> {
        return usuarioDao.obtenerTodos()
    }

    /**
     * Verifica si un correo ya existe en la base de datos
     * @param correo Email a verificar
     * @return true si existe, false si no
     */
    suspend fun correoExiste(correo: String): Boolean {
        return usuarioDao.obtenerPorCorreo(correo) != null
    }

    /**
     * Elimina un usuario de la base de datos
     * (Nota: Room no tiene métodos Delete por defecto en el DAO)
     * Puedes agregar esta funcionalidad al DAO si lo necesitas
     * Por ahora, solo marcamos el usuario como inactivo
     */
    suspend fun eliminarUsuario(usuarioId: String) {
        val usuario = usuarioDao.obtenerPorId(usuarioId)
        if (usuario != null) {
            // Crear una copia del usuario marcado como inactivo
            val usuarioInactivo = usuario.copy(activo = false)
            usuarioDao.actualizar(usuarioInactivo)
        }
    }

    /**
     * Marca un usuario como inactivo (soft delete)
     */
    suspend fun desactivarUsuario(usuarioId: String) {
        eliminarUsuario(usuarioId)
    }
}