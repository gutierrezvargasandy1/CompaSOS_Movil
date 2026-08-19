package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AudioEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a las grabaciones de audio
 * asociadas a la entidad [AudioEntity] en la base de datos local.
 */
@Dao
interface AudioDao {

    /**
     * inserta un nuevo registro de audio en la base de datos.
     *
     * @param audio entidad [AudioEntity] que contiene los datos de la grabación.
     */
    @Insert
    suspend fun insertar(audio: AudioEntity)

    /**
     * obtiene el listado de audios vinculados a una alerta específica ordenados por fecha de forma descendente.
     *
     * @param alertaId identificador único de la alerta consultada.
     * @return lista de entidades [AudioEntity] asociadas a la alerta.
     */
    @Query("SELECT * FROM audios WHERE alertaId = :alertaId ORDER BY fecha DESC")
    suspend fun obtenerPorAlerta(alertaId: String): List<AudioEntity>
}