package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.SensorEntity

@Dao
interface SensorDao {

    @Insert
    suspend fun insertar(sensor: SensorEntity)

    @Query("SELECT * FROM sensores WHERE alertaId = :alertaId ORDER BY fecha")
    suspend fun obtenerPorAlerta(alertaId: String): List<SensorEntity>
}