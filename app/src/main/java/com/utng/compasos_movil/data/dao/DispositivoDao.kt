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

    @Query("SELECT * FROM dispositivos WHERE id = :id")
    suspend fun obtenerPorId(id: String): DispositivoEntity?

    // Actualiza batería y estado de conexión cuando llega un mensaje MQTT de estado
    @Query("UPDATE dispositivos SET bateria = :bateria, conectado = :conectado WHERE id = :id")
    suspend fun actualizarEstado(id: String, bateria: Int?, conectado: Boolean)

    @Query("""
    SELECT * FROM dispositivos
    WHERE tipo = 'tv'
      AND usuarioId = :userId
      AND conectado = 1
""")
    suspend fun obtenerTvsVinculados(userId: String): List<DispositivoEntity>
}