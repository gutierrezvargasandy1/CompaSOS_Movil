package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.UbicacionEntity

/**
 * interfaz dao (data access object) para la gestión y registro de puntos de ubicación geográfica
 * asociados a las alertas mediante la entidad [UbicacionEntity].
 */
@Dao
interface UbicacionDao {

    /**
     * inserta o reemplaza un punto de ubicación en la base de datos local.
     *
     * @param ubicacion entidad [UbicacionEntity] a registrar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(ubicacion: UbicacionEntity)

    /**
     * obtiene la lista de ubicaciones registradas para una alerta específica ordenadas descendentemente por fecha.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return lista de entidades [UbicacionEntity] pertenecientes a la alerta.
     */
    @Query("SELECT * FROM ubicaciones WHERE alertaId = :alertaId ORDER BY fecha DESC")
    suspend fun obtenerPorAlerta(alertaId: String): List<UbicacionEntity>

    /**
     * consulta y obtiene el punto de ubicación geográfica más reciente registrado para una alerta.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return la entidad [UbicacionEntity] más reciente o null si no existen registros para dicha alerta.
     */
    @Query("""
        SELECT * FROM ubicaciones
        WHERE alertaId = :alertaId
        ORDER BY fecha DESC LIMIT 1
    """)
    suspend fun obtenerUltimaPorAlerta(alertaId: String): UbicacionEntity?
}