package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.utng.compasos_movil.data.entity.PerfilMedicoEntity

@Dao
interface PerfilMedicoDao {

    @Insert
    suspend fun insertar(perfil: PerfilMedicoEntity)

    @Update
    suspend fun actualizar(perfil: PerfilMedicoEntity)

    @Query("SELECT * FROM perfil_medico")
    suspend fun obtenerTodos(): List<PerfilMedicoEntity>

    @Query("SELECT * FROM perfil_medico WHERE usuarioId = :usuarioId")
    suspend fun obtenerPorUsuario(usuarioId: String): PerfilMedicoEntity?

    @Query("DELETE FROM perfil_medico WHERE usuarioId = :usuarioId")
    suspend fun eliminarPorUsuario(usuarioId: String)
}