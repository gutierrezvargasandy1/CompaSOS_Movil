package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.FamiliaEntity

/**
 * interfaz dao (data access object) para la gestión y acceso a los datos de los grupos familiares
 * representados por la entidad [FamiliaEntity].
 */
@Dao
interface FamiliaDao {

    /**
     * inserta una nueva familia en la base de datos local.
     *
     * @param familia entidad [FamiliaEntity] a registrar.
     */
    @Insert
    suspend fun insertar(familia: FamiliaEntity)

    /**
     * obtiene el listado completo de todas las familias registradas en la base de datos.
     *
     * @return lista de entidades [FamiliaEntity].
     */
    @Query("SELECT * FROM familias")
    suspend fun obtenerTodas(): List<FamiliaEntity>

    /**
     * consulta y obtiene un grupo familiar específico a partir de su identificador único.
     *
     * @param id identificador único de la familia.
     * @return la entidad [FamiliaEntity] encontrada o null si no existe.
     */
    @Query("SELECT * FROM familias WHERE id = :id")
    suspend fun obtenerPorId(id: String): FamiliaEntity?
}