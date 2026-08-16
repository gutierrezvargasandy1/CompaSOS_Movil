package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity

@Dao
interface HistorialUbicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(historial: HistorialUbicacionEntity)

    @Query("SELECT * FROM historial_ubicacion WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<HistorialUbicacionEntity>

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): UsuarioEntity?

    // ── NUEVO: esta tabla es la fuente de "última ubicación por persona" ──────
    // `ubicaciones` no sirve para esto porque cuelga de alertaId: solo existe
    // ubicación cuando hubo alerta. `historial_ubicacion` sí está indexada por
    // usuarioId, que es justo lo que la TV necesita.

    @Query("""
        SELECT * FROM historial_ubicacion
        WHERE usuarioId = :usuarioId
        ORDER BY fecha DESC
        LIMIT 1
    """)
    suspend fun obtenerUltimaDeUsuario(usuarioId: String): HistorialUbicacionEntity?

    /** Evita que la tabla crezca sin límite con el latido del TvSyncService. */
    @Query("DELETE FROM historial_ubicacion WHERE fecha < :antesDe")
    suspend fun limpiarViejas(antesDe: String)
}