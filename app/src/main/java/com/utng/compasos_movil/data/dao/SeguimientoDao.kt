package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.SeguimientoEntity

/**
 * interfaz dao (data access object) para el registro y seguimiento de eventos u observaciones
 * asociados a las alertas mediante la entidad [SeguimientoEntity].
 */
@Dao
interface SeguimientoDao {

    /**
     * inserta una nueva entrada de seguimiento para una alerta en la base de datos local.
     *
     * @param seguimiento entidad [SeguimientoEntity] a registrar.
     */
    @Insert
    suspend fun insertar(seguimiento: SeguimientoEntity)

    /**
     * obtiene la lista de eventos de seguimiento vinculados a una alerta específica ordenados cronológicamente por fecha.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return lista de entidades [SeguimientoEntity] pertenecientes a la alerta.
     */
    @Query("SELECT * FROM seguimiento WHERE alertaId = :alertaId ORDER BY fecha")
    suspend fun obtenerPorAlerta(alertaId: String): List<SeguimientoEntity>
}