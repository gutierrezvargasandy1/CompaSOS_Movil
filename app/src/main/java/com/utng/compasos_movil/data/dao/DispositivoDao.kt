package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.DispositivoEntity

@Dao
interface DispositivoDao {

    /**
     * ⚠️ CAMBIO: antes era @Insert a secas (estrategia ABORT). Si volvías a
     * vincular la MISMA TV, el insert chocaba con la PK y tiraba
     * SQLiteConstraintException, así que la re-vinculación crasheaba.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

    @Query("UPDATE dispositivos SET conectado = 0 WHERE id = :id")
    suspend fun desconectar(id: String)
}