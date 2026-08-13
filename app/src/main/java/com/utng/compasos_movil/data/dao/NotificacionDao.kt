package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.NotificacionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificacionDao {

    @Insert
    suspend fun insertar(notificacion: NotificacionEntity)

    @Query("SELECT * FROM notificaciones WHERE alertaId = :alertaId")
    suspend fun obtenerPorAlerta(alertaId: String): List<NotificacionEntity>

    // Alertas que YO envié (notificaciones salientes)
    @Query("""
        SELECT n.* FROM notificaciones n
        INNER JOIN alertas a ON a.id = n.alertaId
        WHERE a.usuarioId = :usuarioId
        ORDER BY n.fecha DESC
    """)
    suspend fun obtenerPorUsuario(usuarioId: String): List<NotificacionEntity>

    // Alertas que YO recibí — suspend (para el ViewModel viejo)
    @Query("""
        SELECT * FROM notificaciones
        WHERE destinatario = :userId
        ORDER BY fecha DESC
    """)
    suspend fun obtenerRecibidasPorUsuario(userId: String): List<NotificacionEntity>

    // ← NUEVO: misma query pero reactiva — Room emite cada vez que hay un INSERT
    @Query("""
        SELECT * FROM notificaciones
        WHERE destinatario = :userId
        ORDER BY fecha DESC
    """)
    fun observarRecibidasPorUsuario(userId: String): Flow<List<NotificacionEntity>>
}