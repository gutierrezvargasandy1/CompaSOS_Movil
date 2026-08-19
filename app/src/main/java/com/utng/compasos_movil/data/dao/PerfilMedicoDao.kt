package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los perfiles médicos de emergencia
 * representados por la entidad [PerfilMedicoEntity].
 */
@Dao
interface PerfilMedicoDao {

    /**
     * inserta un nuevo perfil médico en la base de datos local.
     *
     * @param perfil entidad [PerfilMedicoEntity] a registrar.
     */
    @Insert
    suspend fun insertar(perfil: PerfilMedicoEntity)

    /**
     * actualiza la información de un perfil médico existente en la base de datos local.
     *
     * @param perfil entidad [PerfilMedicoEntity] con los datos modificados.
     */
    @Update
    suspend fun actualizar(perfil: PerfilMedicoEntity)

    /**
     * obtiene el listado completo de todos los perfiles médicos almacenados.
     *
     * @return lista de entidades [PerfilMedicoEntity].
     */
    @Query("SELECT * FROM perfil_medico")
    suspend fun obtenerTodos(): List<PerfilMedicoEntity>

    /**
     * obtiene el perfil médico asociado a un usuario específico.
     *
     * @param usuarioId identificador único del usuario consultado.
     * @return la entidad [PerfilMedicoEntity] encontrada o null si no se ha registrado un perfil médico para el usuario.
     */
    @Query("SELECT * FROM perfil_medico WHERE usuarioId = :usuarioId")
    suspend fun obtenerPorUsuario(usuarioId: String): PerfilMedicoEntity?

    /**
     * elimina el perfil médico asociado a un usuario de la base de datos local.
     *
     * @param usuarioId identificador único del usuario cuyo perfil médico será eliminado.
     */
    @Query("DELETE FROM perfil_medico WHERE usuarioId = :usuarioId")
    suspend fun eliminarPorUsuario(usuarioId: String)
}