package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AlertaEntity

@Dao
interface AlertaDao {

    @Insert
    suspend fun insertar(alerta: AlertaEntity)

    @Query("SELECT * FROM alertas")
    suspend fun obtenerTodas(): List<AlertaEntity>

    @Query("SELECT * FROM alertas WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<AlertaEntity>

    @Query("SELECT * FROM alertas WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: String): List<AlertaEntity>
}