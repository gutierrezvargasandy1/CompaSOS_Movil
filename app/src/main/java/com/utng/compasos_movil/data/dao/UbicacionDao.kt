package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.UbicacionEntity

@Dao
interface UbicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ubicacion: UbicacionEntity)

    @Query("SELECT * FROM ubicaciones WHERE alertaId = :alertaId ORDER BY fecha DESC")
    suspend fun obtenerPorAlerta(alertaId: String): List<UbicacionEntity>

    @Query("""
        SELECT * FROM ubicaciones
        WHERE alertaId = :alertaId
        ORDER BY fecha DESC LIMIT 1
    """)
    suspend fun obtenerUltimaPorAlerta(alertaId: String): UbicacionEntity?
}