package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.NotificacionEntity

@Dao
interface NotificacionDao {

    @Insert
    suspend fun insertar(notificacion: NotificacionEntity)

    @Query("SELECT * FROM notificaciones WHERE alertaId = :alertaId")
    suspend fun obtenerPorAlerta(alertaId: String): List<NotificacionEntity>

    // JOIN con alertas para obtener solo las del usuario logueado
    @Query("""
        SELECT n.* FROM notificaciones n
        INNER JOIN alertas a ON a.id = n.alertaId
        WHERE a.usuarioId = :usuarioId
        ORDER BY n.fecha DESC
    """)
    suspend fun obtenerPorUsuario(usuarioId: String): List<NotificacionEntity>
}