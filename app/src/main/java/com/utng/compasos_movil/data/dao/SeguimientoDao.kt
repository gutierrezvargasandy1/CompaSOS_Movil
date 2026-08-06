package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.SeguimientoEntity

@Dao
interface SeguimientoDao {

    @Insert
    suspend fun insertar(seguimiento: SeguimientoEntity)

    @Query("SELECT * FROM seguimiento WHERE alertaId = :alertaId ORDER BY fecha")
    suspend fun obtenerPorAlerta(alertaId: String): List<SeguimientoEntity>
}