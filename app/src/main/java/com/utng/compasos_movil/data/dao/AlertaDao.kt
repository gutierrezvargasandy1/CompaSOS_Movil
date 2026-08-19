package com.utng.compasos_movil.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.utng.compasos_movil.data.entity.AlertaEntity

/**
 * interfaz dao (data access object) para la gestión de datos y operaciones en la base de datos
 * sobre la entidad [AlertaEntity].
 */
@Dao
interface AlertaDao {

    /**
     * inserta una nueva alerta en la base de datos local.
     *
     * @param alerta objeto [AlertaEntity] a registrar.
     */
    @Insert
    suspend fun insertar(alerta: AlertaEntity)

    /**
     * obtiene el listado completo de alertas registradas en la base de datos.
     *
     * @return lista de objetos [AlertaEntity].
     */
    @Query("SELECT * FROM alertas")
    suspend fun obtenerTodas(): List<AlertaEntity>

    /**
     * consulta y obtiene las alertas filtradas por su estado actual.
     *
     * @param estado estado de la alerta por el cual filtrar.
     * @return lista de objetos [AlertaEntity] que coinciden con el estado especificado.
     */
    @Query("SELECT * FROM alertas WHERE estado = :estado")
    suspend fun obtenerPorEstado(estado: String): List<AlertaEntity>

    /**
     * obtiene una alerta específica buscando por su identificador único.
     *
     * @param id identificador único de la alerta a consultar.
     * @return la entidad [AlertaEntity] encontrada o null si no existe.
     */
    @Query("SELECT * FROM alertas WHERE id = :id")
    suspend fun obtenerPorId(id: String): AlertaEntity?

    /**
     * obtiene todas las alertas asociadas a un usuario específico ordenadas descendentemente por fecha.
     *
     * @param usuarioId identificador del usuario propietario de las alertas.
     * @return lista de objetos [AlertaEntity] ordenados de más reciente a más antiguo.
     */
    @Query("SELECT * FROM alertas WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    suspend fun obtenerPorUsuario(usuarioId: String): List<AlertaEntity>
}