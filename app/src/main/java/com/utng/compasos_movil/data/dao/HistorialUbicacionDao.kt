package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.HistorialUbicacionEntity
import com.utng.compasos_movil.data.entity.UsuarioEntity

/**
 * interfaz dao (data access object) para la gestión y acceso al historial de ubicaciones
 * geográficas de los usuarios representados por la entidad [HistorialUbicacionEntity].
 */
@Dao
interface HistorialUbicacionDao {

    /**
     * inserta o reemplaza un registro del historial de ubicación en la base de datos local.
     *
     * @param historial entidad [HistorialUbicacionEntity] a guardar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(historial: HistorialUbicacionEntity)

    /**
     * obtiene la lista de registros de ubicación pertenecientes a un usuario ordenados descendentemente por fecha.
     *
     * @param usuarioId identificador único del usuario consultado.
     * @return lista de entidades [HistorialUbicacionEntity].
     */
    @Query("SELECT * FROM historial_ubicacion WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<HistorialUbicacionEntity>

    /**
     * consulta y obtiene un usuario por su identificador único.
     *
     * @param id identificador del usuario.
     * @return la entidad [UsuarioEntity] encontrada o null si no existe.
     */
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: String): UsuarioEntity?

    // ── NUEVO: esta tabla es la fuente de "última ubicación por persona" ──────
    // `ubicaciones` no sirve para esto porque cuelga de alertaId: solo existe
    // ubicación cuando hubo alerta. `historial_ubicacion` sí está indexada por
    // usuarioId, que es justo lo que la TV necesita.

    /**
     * obtiene el registro de ubicación geográfica más reciente registrado para un usuario específico.
     *
     * @param usuarioId identificador del usuario consultado.
     * @return la entidad [HistorialUbicacionEntity] más reciente o null si no existen registros.
     */
    @Query("""
        SELECT * FROM historial_ubicacion
        WHERE usuarioId = :usuarioId
        ORDER BY fecha DESC
        LIMIT 1
    """)
    suspend fun obtenerUltimaDeUsuario(usuarioId: String): HistorialUbicacionEntity?

    /**
     * elimina de la base de datos todos los registros de ubicación anteriores a la fecha especificada
     * para evitar un crecimiento desmedido de la tabla por los pings periódicos.
     *
     * @param antesDe fecha límite en formato de texto; los registros previos a este punto serán borrados.
     */
    @Query("DELETE FROM historial_ubicacion WHERE fecha < :antesDe")
    suspend fun limpiarViejas(antesDe: String)
}