package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AlertaOficialEntity

@Dao
interface AlertaOficialDao {

    @Insert
    suspend fun insertar(alerta: AlertaOficialEntity)

    @Query("SELECT * FROM alertas_oficiales")
    suspend fun obtenerTodas(): List<AlertaOficialEntity>

    @Query("SELECT * FROM alertas_oficiales WHERE municipio = :municipio")
    suspend fun obtenerPorMunicipio(municipio: String): List<AlertaOficialEntity>
}