package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.UbicacionEntity

@Dao
interface UbicacionDao {

    @Insert
    suspend fun insertar(ubicacion: UbicacionEntity)

    @Query("SELECT * FROM ubicaciones WHERE alertaId = :alertaId ORDER BY fecha")
    suspend fun obtenerPorAlerta(alertaId: String): List<UbicacionEntity>
}