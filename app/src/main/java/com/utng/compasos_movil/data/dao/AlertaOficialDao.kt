package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AlertaOficialEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los datos de la entidad [AlertaOficialEntity].
 * permite realizar operaciones de inserción y consulta de alertas oficiales gubernamentales o institucionales.
 */
@Dao
interface AlertaOficialDao {

    /**
     * inserta una nueva alerta oficial en la base de datos local.
     *
     * @param alerta entidad [AlertaOficialEntity] a guardar.
     */
    @Insert
    suspend fun insertar(alerta: AlertaOficialEntity)

    /**
     * obtiene el listado completo de alertas oficiales almacenadas.
     *
     * @return lista con todas las entidades [AlertaOficialEntity] registradas.
     */
    @Query("SELECT * FROM alertas_oficiales")
    suspend fun obtenerTodas(): List<AlertaOficialEntity>

    /**
     * obtiene las alertas oficiales filtradas por un municipio específico.
     *
     * @param municipio nombre del municipio por el cual se filtrarán las alertas.
     * @return lista de entidades [AlertaOficialEntity] pertenecientes al municipio especificado.
     */
    @Query("SELECT * FROM alertas_oficiales WHERE municipio = :municipio")
    suspend fun obtenerPorMunicipio(municipio: String): List<AlertaOficialEntity>
}