package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.DispositivoEntity

@Dao
interface DispositivoDao {

    @Insert
    suspend fun insertar(dispositivo: DispositivoEntity)

    @Query("SELECT * FROM dispositivos")
    suspend fun obtenerTodos(): List<DispositivoEntity>

    @Query("SELECT * FROM dispositivos WHERE usuarioId = :usuarioId")
    suspend fun obtenerPorUsuario(usuarioId: String): List<DispositivoEntity>
}