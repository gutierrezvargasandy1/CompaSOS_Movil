package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.dao.MiembroConDatos
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity

@Dao
interface FamiliaUsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(familiaUsuario: FamiliaUsuarioEntity)

    // Todas las familias a las que pertenece un usuario
    @Query("SELECT * FROM familia_usuario WHERE usuarioId = :userId")
    suspend fun obtenerFamiliasDeUsuario(userId: String): List<FamiliaUsuarioEntity>

    // Miembros de una familia (solo la relación, sin JOIN)
    @Query("SELECT * FROM familia_usuario WHERE familiaId = :familiaId")
    suspend fun obtenerMiembros(familiaId: String): List<FamiliaUsuarioEntity>

    // Miembros con sus datos de usuario (JOIN con tabla usuarios)
    @Query("""
        SELECT u.*, fu.rol
        FROM usuarios u
        INNER JOIN familia_usuario fu ON fu.usuarioId = u.id
        WHERE fu.familiaId = :familiaId
    """)
    suspend fun obtenerMiembrosConDatos(familiaId: String): List<MiembroConDatos>

    // COUNT para saber si ya es miembro (> 0 = existe)
    @Query("""
        SELECT COUNT(*) FROM familia_usuario
        WHERE familiaId = :familiaId AND usuarioId = :usuarioId
    """)
    suspend fun existeMiembro(familiaId: String, usuarioId: String): Int

    // Para el sistema de alertas: familiares del usuario excluyéndolo a él
    @Query("""
        SELECT DISTINCT fu2.*
        FROM familia_usuario fu2
        WHERE fu2.familiaId IN (
            SELECT familiaId FROM familia_usuario WHERE usuarioId = :usuarioId
        )
        AND fu2.usuarioId != :usuarioId
    """)
    suspend fun obtenerTodosFamiliares(usuarioId: String): List<FamiliaUsuarioEntity>
}