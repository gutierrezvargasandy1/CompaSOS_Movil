package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.FamiliaUsuarioEntity

@Dao
interface FamiliaUsuarioDao {

    @Insert
    suspend fun insertar(familiaUsuario: FamiliaUsuarioEntity)

    @Query("SELECT * FROM familia_usuario WHERE familiaId = :familiaId")
    suspend fun obtenerMiembros(familiaId: String): List<FamiliaUsuarioEntity>

    @Query("SELECT * FROM familia_usuario WHERE usuarioId = :usuarioId")
    suspend fun obtenerFamiliasDeUsuario(usuarioId: String): List<FamiliaUsuarioEntity>
}