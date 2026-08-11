package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity

// Proyección para mostrar el miembro con su nombre (JOIN usuarios + familia_usuario)
data class MiembroConDatos(
    val usuarioId: String,
    val nombre: String,
    val apellidoPaterno: String?,
    val correo: String,
    val rol: String?
)

@Dao
interface FamiliaUsuarioDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(familiaUsuario: FamiliaUsuarioEntity)

    @Query("SELECT * FROM familia_usuario WHERE familiaId = :familiaId")
    suspend fun obtenerMiembros(familiaId: String): List<FamiliaUsuarioEntity>

    @Query("SELECT * FROM familia_usuario WHERE usuarioId = :usuarioId")
    suspend fun obtenerFamiliasDeUsuario(usuarioId: String): List<FamiliaUsuarioEntity>

    @Query("""
        SELECT u.id AS usuarioId,
               u.nombre AS nombre,
               u.apellidoPaterno AS apellidoPaterno,
               u.correo AS correo,
               fu.rol AS rol
        FROM familia_usuario fu
        INNER JOIN usuarios u ON u.id = fu.usuarioId
        WHERE fu.familiaId = :familiaId
    """)
    suspend fun obtenerMiembrosConDatos(familiaId: String): List<MiembroConDatos>

    @Query("SELECT COUNT(*) FROM familia_usuario WHERE familiaId = :familiaId AND usuarioId = :usuarioId")
    suspend fun existeMiembro(familiaId: String, usuarioId: String): Int

    @Query("DELETE FROM familia_usuario WHERE familiaId = :familiaId AND usuarioId = :usuarioId")
    suspend fun eliminarMiembro(familiaId: String, usuarioId: String)
}