package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity

@Dao
interface HistorialUbicacionDao {

    @Insert
    suspend fun insertar(historial: HistorialUbicacionEntity)

    @Query("SELECT * FROM historial_ubicacion WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<HistorialUbicacionEntity>
}