package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.SensorEntity

/**
 * interfaz dao (data access object) para la gestión y almacenamiento de lecturas registradas
 * por los sensores asociados a las alertas mediante la entidad [SensorEntity].
 */
@Dao
interface SensorDao {

    /**
     * inserta un nuevo registro de lectura de sensor en la base de datos local.
     *
     * @param sensor entidad [SensorEntity] a registrar.
     */
    @Insert
    suspend fun insertar(sensor: SensorEntity)

    /**
     * obtiene el listado de lecturas de sensores vinculadas a una alerta específica ordenadas cronológicamente por fecha.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return lista de entidades [SensorEntity] pertenecientes a la alerta.
     */
    @Query("SELECT * FROM sensores WHERE alertaId = :alertaId ORDER BY fecha")
    suspend fun obtenerPorAlerta(alertaId: String): List<SensorEntity>
}